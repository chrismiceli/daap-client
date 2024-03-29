package org.mult.daap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
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

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.ISongUrlConsumer;
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
    private static final String CHANNEL_ID = "daap_channel_song_playing";
    private static final int MENU_STOP = 0;
    private static final int MENU_LIBRARY = 1;
    private static final int MENU_DOWNLOAD = 2;
    static final int REFRESH = 0;
    static final int COPYING_DIALOG = 1;
    static final int SUCCESS_COPYING_DIALOG = 2;
    static final int ERROR_COPYING_DIALOG = 3;
    private static final String logTag = MediaPlayer.class.getName();

    private static MediaPlayer mediaPlayer;
    private static Song song;
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

    public MediaPlayback() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        createNotificationChannel();
        setResult(Activity.RESULT_OK);
        if (Contents.address == null) {
            // We got kicked out of memory probably
            clearState();
            Contents.clearLists();
            stopNotification();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
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
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.media_playback_notification_channel_name);
            String description = getString(R.string.media_playback_notification_channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setSound(null, null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
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
            builder.setPositiveButton(android.R.string.ok, (dialog1, which) -> dialog1.dismiss());
            dialog = builder.create();
        }
        return dialog;
    }

    private void startSong(Song song) {
        clearState();
        mProgress.setEnabled(false);
        mediaPlayer = new MediaPlayer();
        MediaPlayback.song = song;
        setUpActivity();
        Contents.daapHost.getSongURLAsync(song, this);
    }

    private void setUpActivity() {
        mArtistName.setText(song.artist);
        mAlbumName.setText(song.album);
        mTrackName.setText(song.name);
        mProgress.setProgress(0);
        mProgress.setSecondaryProgress(0);
        new LastFMGetSongInfo(this).execute(song);
    }

    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            // intentionally left blank
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            if (mediaPlayer == null) {
                bar.setProgress(0);
            } else {
                // get correct length of the song we are going to seek in
                double doubleDuration = mediaPlayer.getDuration();
                int desiredSeek = (int) (((double) progress / 100.0) * doubleDuration);
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
                    mediaPlayer.pause();
                    stopNotification();
                } else {
                    mediaPlayer.start();
                    startNotification();
                }
                setPauseButton();
                queueNextRefresh(refreshNow());
            }
        }
    };
    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                startSong(Contents.getPreviousSong());
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
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeMessages(REFRESH);
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(MENU_DOWNLOAD).setEnabled(mediaPlayer != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
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
                Intent intent = new Intent(MediaPlayback.this, PlaylistBrowser.class);
                startActivity(intent);
                break;
            case MENU_DOWNLOAD:
                showDialog(COPYING_DIALOG);
                new Thread(new FileCopier(this.getExternalFilesDir(Environment.DIRECTORY_MUSIC))).start();
                break;
        }
        return true;
    }

    @Override
    public void onSongUrlRetrieved(String songUrl) {
        try {
            mediaPlayer.setDataSource(songUrl);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(normalOnCompletionListener);
            mediaPlayer.setOnErrorListener(mediaPlayerErrorListener);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                mProgress.setEnabled(true);
                stopNotification();
                startNotification();
                queueNextRefresh(refreshNow());
            });
            mediaPlayer.prepareAsync();
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    tm.registerTelephonyCallback(getMainExecutor(), new PhoneTelephonyCallback());
                }
            } else {
                tm.listen(new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumsber) {
                        handleCallStateChanged(state);
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE);
            }
            setUpActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.media_playback_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private class FileCopier implements Runnable {
        private final File directory;

        FileCopier(File directory) {
            this.directory = directory;
        }

        public void run() {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && mediaPlayer != null) {
                boolean wasPlaying = mediaPlayer.isPlaying();
                try {
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
                    handler.sendEmptyMessage(COPYING_DIALOG);

                    if (wasPlaying) {
                        mediaPlayer.start();
                    }
                    handler.sendEmptyMessage(SUCCESS_COPYING_DIALOG);
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

    void queueNextRefresh(long delay) {
        handler.removeMessages(REFRESH);
        handler.sendEmptyMessageDelayed(REFRESH, delay);
    }

    long refreshNow() {
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
        String durationformat = (secs < 3600 ? "%2$d:%5$02d" : "%1$d:%3$02d:%5$02d");
        StringBuilder sFormatBuilder = new StringBuilder();
        Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
        sFormatBuilder.setLength(0);
        final Object[] timeArgs = new Object[5];
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        return sFormatter.format(durationformat, timeArgs).toString();
    }

    @Override
    public boolean onLongClick(View arg0) {
        return false;
    }

    public void startNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.stat_notify_musicplayer)
                        .setContentTitle(song.name)
                        .setContentText(song.artist)
                        .setSmallIcon(R.drawable.stat_notify_musicplayer)
                        .setSound(null);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, getIntent(), PendingIntent.FLAG_IMMUTABLE);
        Intent resultIntent = new Intent(this, MediaPlayback.class);
        stackBuilder.addParentStack(MediaPlayback.class);
        stackBuilder.addNextIntent(resultIntent);
        mBuilder.setContentIntent(contentIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, mBuilder.build());
    }

    public void stopNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class PhoneTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            handleCallStateChanged(state);
        }
    }

    private void handleCallStateChanged(int state) {
        switch (state) {
            // change to idle
            case TelephonyManager.CALL_STATE_IDLE:
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
            case TelephonyManager.CALL_STATE_RINGING:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
                break;
        }
    }

    TextView textViewForContainer(View v) {
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            v.setBackgroundColor(0xff606060);
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(0);
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the
                        // right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
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
        }
        return false;
    }

    final Handler mLabelScroller = new ScrollHandler(this);

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

    private final OnErrorListener mediaPlayerErrorListener = (mp, what, extra) -> {
        Log.e(logTag, "Error in MediaPlayer: (" + what + ") with extra (" + extra + ")");
        clearState();
        return false;
    };

    private final OnCompletionListener normalOnCompletionListener = mp -> {
        try {
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
    };

    static private class LastFMGetSongInfo extends AsyncTask<Song, Void, String> {
        private final WeakReference<MediaPlayback> mediaPlaybackWeakReference;

        LastFMGetSongInfo(MediaPlayback mediaPlayback) {
            this.mediaPlaybackWeakReference = new WeakReference<>(mediaPlayback);
        }

        protected String doInBackground(Song... song) {
            String key = "47c0f71763c30293aa52f0ac166e410f";
            String retval = "";
            try {
                URL lastFM = new URL("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + URLEncoder.encode(song[0].artist, "UTF-8") + "&api_key=" + key);
                Log.d("MediaPlayback", "Songurl = " + lastFM);
                URLConnection lfmConnection = lastFM.openConnection();
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(lfmConnection.getInputStream());
                NodeList nList = doc.getElementsByTagName("summary");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        retval = nNode.getFirstChild().getNodeValue();
                        retval = retval.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
                        break;
                    }
                }
            } catch (Exception e) {
                // Do nothing
            }
            return retval;
        }

        protected void onPostExecute(String result) {
            MediaPlayback mediaPlayback = mediaPlaybackWeakReference.get();
            if (mediaPlayback != null) {
                mediaPlayback.mSongSummary.setText(Html.fromHtml(result));
                mediaPlayback.mSongSummary.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private static class ScrollHandler extends Handler {
        private final WeakReference<MediaPlayback> mediaPlaybackWeakReference;

        ScrollHandler(MediaPlayback mediaPlayback) {
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
                if (mediaPlayback != null) {
                    mediaPlayback.mLabelScroller.sendMessageDelayed(newMessage, 15);
                }
            }
        }
    }
}