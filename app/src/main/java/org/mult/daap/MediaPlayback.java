package org.mult.daap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.client.ISongUrlConsumer;
import org.mult.daap.db.entity.SongEntity;
import org.mult.daap.widget.DAAPClientAppWidgetOneProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MediaPlayback extends Activity implements View.OnTouchListener, View.OnLongClickListener, ISongUrlConsumer {
    private static final int MENU_STOP = 0;
    private static final int MENU_LIBRARY = 1;
    private static final int MENU_DOWNLOAD = 2;
    private static final int REFRESH = 0;
    private static final int COPYING_DIALOG = 1;
    private static final int SUCCESS_COPYING_DIALOG = 2;
    private static final int ERROR_COPYING_DIALOG = 3;
    private static final String logTag = MediaPlayer.class.getName();

    private static MediaPlayer mediaPlayer;
    private MediaPlaybackService mMediaPlaybackService = null;
    private static SongEntity song;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mSongSummary;
    private ImageButton mShuffleButton;
    private ImageButton mRepeatButton;
    private ImageButton mPrevButton;
    private ImageButton mNextButton;
    private ImageButton mPauseButton;
    private SeekBar mProgress;
    private int mTouchSlop;
    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private boolean mDraggingLabel = false;
    private boolean scrobbler_support = false;

    private final DAAPClientAppWidgetOneProvider mAppWidgetProvider = DAAPClientAppWidgetOneProvider.getInstance();

    public MediaPlayback() {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setResult(Activity.RESULT_OK);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.audio_player);
        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        mProgress = findViewById(android.R.id.progress);
        mShuffleButton = findViewById(R.id.shuffleButton);
        mRepeatButton = findViewById(R.id.repeatButton);
        mPauseButton = findViewById(R.id.pause);
        mPrevButton = findViewById(R.id.prev);
        mNextButton = findViewById(R.id.next);
        mCurrentTime = findViewById(R.id.currenttime);
        mTotalTime = findViewById(R.id.totaltime);
        mArtistName = findViewById(R.id.artistname);
        mAlbumName = findViewById(R.id.albumname);
        mTrackName = findViewById(R.id.trackname);
        mSongSummary = findViewById(R.id.song_summary);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        scrobbler_support = mPrefs.getBoolean("scrobbler_pref", false);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = (View) mArtistName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
        v = (View) mAlbumName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
        v = (View) mTrackName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
        if (Contents.shuffle) {
            mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle_on);
        } else {
            mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle);
        }
        if (Contents.repeat) {
            mRepeatButton.setImageResource(R.drawable.ic_menu_repeat_on);
        } else {
            mRepeatButton.setImageResource(R.drawable.ic_menu_repeat);
        }
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton.setOnClickListener(mRepeatListener);
        mPauseButton.setOnClickListener(mPauseListener);
        mPrevButton.setOnClickListener(mPrevListener);
        mNextButton.setOnClickListener(mNextListener);
        mProgress.setMax(100);
        mProgress.setProgress(0);
        mProgress.setSecondaryProgress(0);
        mProgress.setOnSeekBarChangeListener(mSeekListener);
        if (mediaPlayer == null) {
            try {
                startSong(Contents.getSong());
            } catch (IndexOutOfBoundsException e) {
                Log.e(logTag, "Something went wrong with playlist/queue");
                e.printStackTrace();
                finish();
            }
        }
        setUpActivity();
        queueNextRefresh(refreshNow());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == COPYING_DIALOG) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.downloading_file));
            progressDialog.setCancelable(false);
            dialog = progressDialog;
        } else if (id == ERROR_COPYING_DIALOG || id == SUCCESS_COPYING_DIALOG) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (id == ERROR_COPYING_DIALOG) {
                builder.setMessage(R.string.save_error);
            } else {
                builder.setMessage(R.string.save_complete);
            }
            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
        }
        return dialog;
    }

    private void startSong(SongEntity song) {
        clearState();
        mProgress.setEnabled(false);
        mediaPlayer = new MediaPlayer();
        MediaPlayback.song = song;
        Contents.daapHost.getSongURLAsync(song, this);
    }

    public void onSongUrlRetrieved(String songUrl) {
        try {
            mediaPlayer.setDataSource(songUrl);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(normalOnCompletionListener);
            mediaPlayer.setOnErrorListener(mediaPlayerErrorListener);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mProgress.setEnabled(true);
                    stopNotification();
                    startNotification();
                    queueNextRefresh(refreshNow());
                    mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                }
            });
            mediaPlayer.prepareAsync();
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (null != tm) {
                tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
            setUpActivity();
            if (scrobbler_support) {
                scrobble(0); // START
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.media_playback_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setUpActivity() {
        mArtistName.setText(song.artist);
        mAlbumName.setText(song.album);
        mTrackName.setText(song.name);
        mProgress.setProgress(0);
        mProgress.setSecondaryProgress(0);
        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(mMediaPlaybackService, this, MediaPlaybackService.META_CHANGED);
        new LastFMGetSongInfo(this).execute(song);
    }

    public String getTrackName() {
        return song.name;
    }

    public String getArtistName() {
        return song.artist;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();

    }

    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            // intentionally left blank
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            if (mediaPlayer == null) {
                bar.setProgress(0);
            } else {
                double doubleProgress = (double) progress;
                double doubleDuration;
                // get correct length of the song we are going to seek in
                doubleDuration = mediaPlayer.getDuration();
                int desiredSeek = (int) ((doubleProgress / 100.0) * doubleDuration);
                mediaPlayer.seekTo(desiredSeek);
                bar.setProgress(progress);
                handler.removeMessages(REFRESH);
                queueNextRefresh(refreshNow());
            }
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            // intentionally left blank
        }
    };

    private final View.OnClickListener mShuffleListener = new View.OnClickListener() {

        public void onClick(View v) {
            if (Contents.shuffle) {
                Contents.shuffle = false;
                mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle);
            } else {
                Contents.shuffle = true;
                mShuffleButton.setImageResource(R.drawable.ic_menu_shuffle_on);
            }
        }
    };

    private final View.OnClickListener mRepeatListener = new View.OnClickListener() {

        public void onClick(View v) {
            if (Contents.repeat) {
                Contents.repeat = false;
                mRepeatButton.setImageResource(R.drawable.ic_menu_repeat);
            } else {
                Contents.repeat = true;
                mRepeatButton.setImageResource(R.drawable.ic_menu_repeat_on);
            }
        }
    };

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    if (scrobbler_support) {
                        scrobble(2); // PAUSE
                    }
                    mediaPlayer.pause();
                    stopNotification();
                } else {
                    if (scrobbler_support) {
                        scrobble(1); // RESUME
                    }
                    mediaPlayer.start();
                    startNotification();
                }
                setPauseButton();
                queueNextRefresh(refreshNow());
                mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
            }
        }
    };
    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                startSong(Contents.getPreviousSong());
                mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
            } catch (IndexOutOfBoundsException e) {
                handler.removeMessages(REFRESH);
                stopNotification();
                clearState();
                finish();
            }
        }
    };
    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            normalOnCompletionListener.onCompletion(mediaPlayer);
            mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
        }
    };

    @Override
    public void onPause() {
        handler.removeMessages(REFRESH);
        super.onPause();
    }

    @Override
    public void onStop() {
        handler.removeMessages(REFRESH);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PREVIOUS);
        f.addAction(MediaPlaybackService.NEXT);
        f.addAction(MediaPlaybackService.TOGGLEPAUSE);
        f.addAction(MediaPlaybackService.PAUSE);
        f.addAction(MediaPlaybackService.STOP);
        f.addAction(MediaPlaybackService.ADDED);
        f.addAction(MediaPlaybackService.HEADSET_CHANGE);
        registerReceiver(mStatusListener, new IntentFilter(f));

        if (mMediaPlaybackService == null) {
            bindService(new Intent(this, MediaPlaybackService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeMessages(REFRESH);
        if (scrobbler_support) {
            scrobble(3); // COMPLETE
        }
        super.onDestroy();

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            // Share this notification directly with our widgets
            mAppWidgetProvider.notifyChange(mMediaPlaybackService, this, MediaPlaybackService.PLAYER_CLOSED);

            unregisterReceiver(mStatusListener);
            // Detach our existing connection.
            unbindService(connection);
            mMediaPlaybackService = null;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mediaPlayer != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            menu.findItem(MENU_DOWNLOAD).setEnabled(true);
        } else {
            menu.findItem(MENU_DOWNLOAD).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        menu.add(0, MENU_LIBRARY, 0, R.string.menu_library).setIcon(R.drawable.ic_menu_list);
        menu.add(0, MENU_DOWNLOAD, 1, R.string.save).setIcon(android.R.drawable.ic_menu_save);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_STOP:
            clearState();
            stopNotification();
            finish();
            break;
        case MENU_LIBRARY:
            Intent intent = new Intent(MediaPlayback.this, SongsDrawerActivity.class);
            startActivity(intent);
            break;
        case MENU_DOWNLOAD:
            showDialog(COPYING_DIALOG);
            new Thread(new FileCopier()).start();
            break;
        }
        return true;
    }

    private class FileCopier implements Runnable {
        public void run() {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && mediaPlayer != null) {
                boolean wasPlaying = mediaPlayer.isPlaying();
                try {
                    File directory = new File(Environment.getExternalStorageDirectory(), "DAAP");
                    if (directory.mkdirs()) {
                        File destination = new File(directory, "DAAP-" + song.id + "." + song.format);
                        mediaPlayer.pause();
                        InputStream songStream = Contents.daapHost.getSongStream(song);
                        FileOutputStream destinationStream = new FileOutputStream(destination);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = songStream.read(buffer)) > 0) {
                            destinationStream.write(buffer, 0, len);
                        }

                        songStream.close();
                        destinationStream.close();
                        destination.deleteOnExit();
                        handler.sendEmptyMessage(COPYING_DIALOG);

                        if (wasPlaying)
                            mediaPlayer.start();
                        handler.sendEmptyMessage(SUCCESS_COPYING_DIALOG);
                    } else {
                        handler.sendEmptyMessage(ERROR_COPYING_DIALOG);
                    }
                } catch (Exception e) {
                    if (wasPlaying)
                        mediaPlayer.start();
                    handler.sendEmptyMessage(COPYING_DIALOG);
                    handler.sendEmptyMessage(ERROR_COPYING_DIALOG);
                    e.printStackTrace();
                }
            }
        }
    }

    private void setPauseButton() throws IllegalStateException {
        if (mediaPlayer != null) {
            mPauseButton.setEnabled(true);
            if (mediaPlayer.isPlaying()) {
                mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        } else {
            mPauseButton.setEnabled(false);
        }
    }

    private final Handler handler = new CopyHandler(this);

    private void queueNextRefresh(long delay) {
        handler.removeMessages(REFRESH);
        handler.sendEmptyMessageDelayed(REFRESH, delay);
    }

    private long refreshNow() {
        try {
            if (mediaPlayer == null) {
                return 500;
            }
            mTotalTime.setText(makeTimeString(song.time / 1000));
            int mDuration = song.time;
            setPauseButton();
            long pos = mediaPlayer.getCurrentPosition();
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(makeTimeString(pos / 1000));
                if (mediaPlayer.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    remaining = 500;
                }
                mProgress.setProgress((int) (100 * pos / mDuration));
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
        } catch (IllegalStateException e) {
            return 500;
        }
    }

    private static String makeTimeString(long secs) {
        String durationFormat = (secs < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d");
        StringBuilder sFormatBuilder = new StringBuilder();
        Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
        sFormatBuilder.setLength(0);
        final Object[] timeArgs = new Object[5];
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        return sFormatter.format(durationFormat, timeArgs).toString();
    }

    public boolean onLongClick(View arg0) {
        return false;
    }

    private void startNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (null != notificationManager) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext(), "daap_channel_song_playing")
                            .setSmallIcon(R.drawable.stat_notify_musicplayer)
                            .setContentTitle(song.name)
                            .setContentText(song.artist);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, getIntent(), 0);
            Intent resultIntent = new Intent(this, MediaPlayback.class);
            stackBuilder.addParentStack(MediaPlayback.class);
            stackBuilder.addNextIntent(resultIntent);
            mBuilder.setContentIntent(contentIntent);
            Notification notification = new Notification(R.drawable.stat_notify_musicplayer, song.name, System.currentTimeMillis());
            /*notification.setLatestEventInfo(getApplicationContext(), song.name, song.artist, contentIntent);*/
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notificationManager.notify(0, mBuilder.build());
        }
    }

    private void stopNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (null != notificationManager) {
            notificationManager.cancelAll();
        }
    }

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
            // change to idle
            case TelephonyManager.CALL_STATE_IDLE:
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
                break;
            }
        }
    };

    private TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.artistname);
        if (vv != null)
            return (TextView) vv;
        vv = v.findViewById(R.id.albumname);
        if (vv != null)
            return (TextView) vv;
        vv = v.findViewById(R.id.trackname);
        if (vv != null)
            return (TextView) vv;
        return null;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                v.setBackgroundColor(0xff606060);
                mInitialX = mLastX = (int) event.getX();
                mDraggingLabel = false;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.setBackgroundColor(0);
                if (mDraggingLabel) {
                    Message msg = mLabelScroller.obtainMessage(0, tv);
                    mLabelScroller.sendMessageDelayed(msg, 1000);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mDraggingLabel) {
                    int scrollX = tv.getScrollX();
                    int x = (int) event.getX();
                    int delta = mLastX - x;
                    if (delta != 0) {
                        mLastX = x;
                        scrollX += delta;
                        if (scrollX > mTextWidth) {
                            // scrolled the text completely off the view to the left
                            scrollX -= mTextWidth;
                            scrollX -= mViewWidth;
                        }
                        if (scrollX < -mViewWidth) {
                            // scrolled the text completely off the view to the
                            // right
                            scrollX += mViewWidth;
                            scrollX += mTextWidth;
                        }
                        tv.scrollTo(scrollX, 0);
                    }
                    return true;
                }
                int delta = mInitialX - (int) event.getX();
                if (Math.abs(delta) > mTouchSlop) {
                    mLabelScroller.removeMessages(0, tv);
                    if (tv.getEllipsize() != null) {
                        tv.setEllipsize(null);
                    }
                    Layout ll = tv.getLayout();
                    if (ll == null) {
                        return false;
                    }
                    mTextWidth = (int) tv.getLayout().getLineWidth(0);
                    mViewWidth = tv.getWidth();
                    if (mViewWidth > mTextWidth) {
                        tv.setEllipsize(TruncateAt.END);
                        v.cancelLongPress();
                        return false;
                    }
                    mDraggingLabel = true;
                    tv.setHorizontalFadingEdgeEnabled(true);
                    v.cancelLongPress();
                    return true;
                }
                break;
        }

        return false;
    }

    private final Handler mLabelScroller = new ScrollHandler(this);

    public static void clearState() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.i(logTag, "Usually this is not a problem.");
            }
        }
        mediaPlayer = null;
    }

    private final OnErrorListener mediaPlayerErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(logTag, "Error in MediaPlayer: (" + what + ") with extra (" + extra + ")");
            clearState();
            return false;
        }
    };

    private final OnCompletionListener normalOnCompletionListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            try {
                if (scrobbler_support) {
                    scrobble(3); // COMPLETE
                }
                if (Contents.shuffle) {
                    startSong(Contents.getRandomSong());
                } else if (Contents.repeat) {
                    mp.seekTo(0);
                    mp.start();
                    queueNextRefresh(refreshNow());
                } else {
                    startSong(Contents.getNextSong());
                }
            } catch (IndexOutOfBoundsException e) {
                handler.removeMessages(REFRESH);
                stopNotification();
                clearState();
                finish();
            }
        }
    };

    private void scrobble(int code) {
        boolean playing = code == 0 || code == 1;
        @SuppressWarnings("SpellCheckingInspection")
        Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
        bCast.putExtra("state", code);
        bCast.putExtra("app-name", "Daap-client");
        bCast.putExtra("app-package", "org.mult.daap");
        bCast.putExtra("artist", song.artist);
        bCast.putExtra("album", song.album);
        bCast.putExtra("track", song.name);
        bCast.putExtra("duration", song.time / 1000);
        sendBroadcast(bCast);
        Intent i = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
        i.putExtra("playing", playing);
        i.putExtra("artist", song.artist);
        i.putExtra("album", song.album);
        i.putExtra("track", song.name);
        i.putExtra("secs", song.time / 1000);
        sendBroadcast(i);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mMediaPlaybackService = ((MediaPlaybackService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName classname) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mMediaPlaybackService = null;
        }
    };

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (null != action) {
                switch (action) {
                    case MediaPlaybackService.PREVIOUS:
                        startSong(Contents.getPreviousSong());
                        mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                        break;
                    case MediaPlaybackService.NEXT:
                        normalOnCompletionListener.onCompletion(mediaPlayer);
                        mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                        break;
                    case MediaPlaybackService.TOGGLEPAUSE:
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                if (scrobbler_support) {
                                    scrobble(2); // PAUSE
                                }
                                mediaPlayer.pause();
                                stopNotification();
                            } else {
                                if (scrobbler_support) {
                                    scrobble(1); // RESUME
                                }
                                mediaPlayer.start();
                                startNotification();
                            }
                            setPauseButton();
                            queueNextRefresh(refreshNow());
                            mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                        }
                        break;
                    case MediaPlaybackService.PAUSE:
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                if (scrobbler_support) {
                                    scrobble(2); // PAUSE
                                }
                                mediaPlayer.pause();
                                stopNotification();
                            } else {
                                if (scrobbler_support) {
                                    scrobble(1); // RESUME
                                }
                                mediaPlayer.start();
                                startNotification();
                            }
                            setPauseButton();
                            queueNextRefresh(refreshNow());
                            mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                        }
                        break;
                    case MediaPlaybackService.STOP:
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                if (scrobbler_support) {
                                    scrobble(2); // PAUSE
                                }
                                mediaPlayer.pause();
                                mediaPlayer.seekTo(0);
                                stopNotification();
                            }
                            setPauseButton();
                            queueNextRefresh(refreshNow());
                            mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                        }
                        break;
                    case MediaPlaybackService.ADDED:
                        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                        mAppWidgetProvider.performUpdate(mMediaPlaybackService, MediaPlayback.this, appWidgetIds, "");
                        break;
                    case MediaPlaybackService.HEADSET_CHANGE:
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                if (scrobbler_support) {
                                    scrobble(2); // PAUSE
                                }
                                mediaPlayer.pause();
                                stopNotification();
                            }
                            setPauseButton();
                            queueNextRefresh(refreshNow());
                            mAppWidgetProvider.notifyChange(mMediaPlaybackService, MediaPlayback.this, MediaPlaybackService.PLAYSTATE_CHANGED);
                        }
                        break;
                }
            }
        }
    };

    private static class CopyHandler extends Handler {
        private final WeakReference<MediaPlayback> mediaPlaybackWeakReference;

        CopyHandler(MediaPlayback mediaPlayback) {
            this.mediaPlaybackWeakReference = new WeakReference<>(mediaPlayback);
        }

        @Override
        public void handleMessage(Message message) {
            MediaPlayback mediaPlayback = this.mediaPlaybackWeakReference.get();
            if (mediaPlayback != null)
            {
                switch (message.what) {
                    case REFRESH:
                        mediaPlayback.queueNextRefresh(mediaPlayback.refreshNow());
                        break;
                    case COPYING_DIALOG:
                        mediaPlayback.dismissDialog(COPYING_DIALOG);
                        break;
                    case SUCCESS_COPYING_DIALOG:
                        mediaPlayback.showDialog(SUCCESS_COPYING_DIALOG);
                        break;
                    case ERROR_COPYING_DIALOG:
                        mediaPlayback.showDialog(ERROR_COPYING_DIALOG);
                        break;
                }
             }
        }
    }

    private static class LastFMGetSongInfo extends AsyncTask<SongEntity, Void, String> {
        private final WeakReference<MediaPlayback> mediaPlaybackWeakReference;
        LastFMGetSongInfo(MediaPlayback mediaPlayback)
        {
            this.mediaPlaybackWeakReference = new WeakReference<>(mediaPlayback);
        }

        protected String doInBackground(SongEntity... song) {
            String key = "47c0f71763c30293aa52f0ac166e410f";
            String result = "";
            try {
                URL lastFM = new URL("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + URLEncoder.encode(song[0].artist, "UTF-8") + "&api_key=" + key);
                URLConnection lfmConnection = lastFM.openConnection();
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(lfmConnection.getInputStream());
                NodeList nList = doc.getElementsByTagName("summary");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        result = nNode.getFirstChild().getNodeValue();
                        result = result.replace("&quot;", "\"").replace("&apos;", "\'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
                        break;
                    }
                }
            } catch (Exception e) {
                // Do nothing
            }
            return result;
        }

        protected void onPostExecute(String result) {
            MediaPlayback mediaPlayback = this.mediaPlaybackWeakReference.get();
            if (mediaPlayback != null)
            {
                mediaPlayback.mSongSummary.setText(Html.fromHtml(result));
                mediaPlayback.mSongSummary.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private static class ScrollHandler extends Handler
    {
        private final WeakReference<MediaPlayback> mediaPlaybackWeakReference;

        ScrollHandler(MediaPlayback mediaPlayback)
        {
            this.mediaPlaybackWeakReference = new WeakReference<>(mediaPlayback);
        }

        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            } else {
                Message newMessage = obtainMessage(0, tv);
                MediaPlayback mediaPlayback = this.mediaPlaybackWeakReference.get();
                if (mediaPlayback != null)
                {
                    mediaPlayback.mLabelScroller.sendMessageDelayed(newMessage, 15);
                }
            }
        }
    }
}