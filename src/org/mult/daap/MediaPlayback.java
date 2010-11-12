/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mult.daap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Formatter;
import java.util.Locale;

import org.mult.daap.client.Song;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
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

public class MediaPlayback extends Activity implements View.OnTouchListener,
        View.OnLongClickListener {
    private static final int MENU_STOP = 0;
    private static final int MENU_DOWNLOAD = 1;
    private static final int MENU_REPEAT = 2;
    private static final int MENU_SHUFFLE = 3;
    private static final int REFRESH = 0;
    private static final int COPYING_DIALOG = 1;
    private static final int SUCCESS_COPYING_DIALOG = 2;
    private static final int ERROR_COPYING_DIALOG = 3;
    private static final String logTag = MediaPlayer.class.getName();

    private static MediaPlayer mediaPlayer;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private ImageButton mPrevButton;
    private ImageButton mNextButton;
    private ImageButton mPauseButton;
    private SeekBar mProgress;
    private Song song;
    private int mTouchSlop;
    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private boolean mDraggingLabel = false;

    public MediaPlayback() {}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
        mProgress = (SeekBar) findViewById(android.R.id.progress);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mNextButton = (ImageButton) findViewById(R.id.next);
        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mAlbumName = (TextView) findViewById(R.id.albumname);
        mTrackName = (TextView) findViewById(R.id.trackname);
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
        }
        else if (id == ERROR_COPYING_DIALOG || id == SUCCESS_COPYING_DIALOG) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (id == ERROR_COPYING_DIALOG) {
                builder.setMessage(R.string.save_error);
            }
            else {
                builder.setMessage(R.string.save_complete);
            }
            builder.setPositiveButton(android.R.string.ok,
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            dialog = builder.create();
        }
        return dialog;
    }

    private void startSong(Song song) {
        clearState();
        mProgress.setEnabled(false);
        mediaPlayer = new MediaPlayer();
        this.song = song;
        try {
            mediaPlayer.setDataSource(Contents.daapHost.getSongURL(song));
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(normalOnCompletionListener);
            mediaPlayer.setOnErrorListener(mediaPlayerErrorListener);
            mediaPlayer
                    .setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                            mProgress.setEnabled(true);
                            stopNotification();
                            startNotification();
                            queueNextRefresh(refreshNow());
                        }
                    });
            mediaPlayer.prepareAsync();
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            setUpActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.media_playback_error,
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setUpActivity() {
        mArtistName.setText(song.artist);
        mAlbumName.setText(song.album);
        mTrackName.setText(song.name);
        mProgress.setProgress(0);
        mProgress.setSecondaryProgress(0);
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            // intentionally left blank
        }

        public void onProgressChanged(SeekBar bar, int progress,
                boolean fromuser) {
            if (!fromuser) {
                return;
            }
            if (mediaPlayer == null) {
                bar.setProgress(0);
                return;
            }
            else {
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
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    stopNotification();
                }
                else {
                    mediaPlayer.start();
                    startNotification();
                }
                setPauseButton();
                queueNextRefresh(refreshNow());
            }
        }
    };
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                startSong(Contents.getPreviousSong());
            } catch (IndexOutOfBoundsException e) {
                handler.removeMessages(REFRESH);
                stopNotification();
                clearState();
                finish();
                return;
            }
        }
    };
    private View.OnClickListener mNextListener = new View.OnClickListener() {
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
    public void onStart() {
        super.onStart();
        queueNextRefresh(refreshNow());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeMessages(REFRESH);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Log.v("MediaPlayback",
        // "External Storage:" + Environment.getExternalStorageDirectory());
        // Log.v("MediaPlayback",
        // "External Storage State = "
        // + Environment.getExternalStorageState());
        // if (mediaPlayer == null) {
        // Log.v("MediaPlayback", "mediaplayer == null!");
        // }
        // else {
        // Log.v("MediaPlayback", "mediaPlayer != null");
        // }
        if (mediaPlayer != null
                && Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED)) {
            menu.findItem(MENU_DOWNLOAD).setEnabled(true);
        }
        else {
            menu.findItem(MENU_DOWNLOAD).setEnabled(false);
        }
        if (Contents.shuffle) {
            menu.findItem(MENU_SHUFFLE).setIcon(R.drawable.ic_menu_shuffle_on);
        }
        else {
            menu.findItem(MENU_SHUFFLE).setIcon(R.drawable.ic_menu_shuffle);
        }
        if (Contents.repeat) {
            menu.findItem(MENU_REPEAT).setIcon(R.drawable.ic_menu_repeat_on);
        }
        else {
            menu.findItem(MENU_REPEAT).setIcon(R.drawable.ic_menu_repeat);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        menu.add(0, MENU_DOWNLOAD, 1, R.string.save).setIcon(
                android.R.drawable.ic_menu_save);
        menu.add(0, MENU_REPEAT, 2, R.string.repeat);
        menu.add(0, MENU_SHUFFLE, 3, R.string.shuffle);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHUFFLE:
                Contents.shuffle = !Contents.shuffle;
                break;
            case MENU_REPEAT:
                Contents.repeat = !Contents.repeat;
                break;
            case MENU_STOP:
                clearState();
                stopNotification();
                finish();
                break;
            case MENU_DOWNLOAD:
                showDialog(COPYING_DIALOG);
                new Thread(new FileCopier()).start();
                break;
        }
        return true;
    }

    private class FileCopier implements Runnable {
        @Override
        public void run() {
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)
                    && mediaPlayer != null) {
                boolean wasPlaying = mediaPlayer.isPlaying();
                try {
                    File directory = new File(
                            Environment.getExternalStorageDirectory(), "DAAP");
                    directory.mkdirs();
                    File destination = new File(directory, "DAAP-" + song.id
                            + "." + song.format);
                    mediaPlayer.pause();
                    InputStream songStream = Contents.daapHost
                            .getSongStream(song);
                    FileOutputStream destinationStream = new FileOutputStream(
                            destination);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = songStream.read(buffer)) > 0) {
                        destinationStream.write(buffer, 0, len);
                    }
                    if (songStream != null)
                        songStream.close();
                    if (destinationStream != null)
                        destinationStream.close();
                    destination.deleteOnExit();
                    handler.sendEmptyMessage(COPYING_DIALOG);

                    if (wasPlaying)
                        mediaPlayer.start();
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
        try {
            if (mediaPlayer != null) {
                mPauseButton.setEnabled(true);
                if (mediaPlayer.isPlaying()) {
                    mPauseButton
                            .setImageResource(android.R.drawable.ic_media_pause);
                }
                else {
                    mPauseButton
                            .setImageResource(android.R.drawable.ic_media_play);
                }
            }
            else {
                mPauseButton.setEnabled(false);
            }
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case REFRESH:
                    queueNextRefresh(refreshNow());
                    break;
                case COPYING_DIALOG:
                    dismissDialog(COPYING_DIALOG);
                    break;
                case SUCCESS_COPYING_DIALOG:
                    showDialog(SUCCESS_COPYING_DIALOG);
                    break;
                case ERROR_COPYING_DIALOG:
                    showDialog(ERROR_COPYING_DIALOG);
                    break;
            }
        }
    };

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
                }
                else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime
                            .setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                                    : View.INVISIBLE);
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
        String durationformat = (secs < 3600 ? "%2$d:%5$02d"
                : "%1$d:%3$02d:%5$02d");
        StringBuilder sFormatBuilder = new StringBuilder();
        Formatter sFormatter = new Formatter(sFormatBuilder,
                Locale.getDefault());
        sFormatBuilder.setLength(0);
        final Object[] timeArgs = new Object[5];
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        return sFormatter.format(durationformat, timeArgs).toString();
    }

    public boolean onLongClick(View arg0) {
        return false;
    }

    public void startNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(
                R.drawable.stat_notify_musicplayer, song.name,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, getIntent(), 0);
        notification.setLatestEventInfo(getApplicationContext(), song.name,
                song.artist, contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManager.notify(0, notification);
    }

    public void stopNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumsber) {
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
        }
        else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(0);
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        }
        else if (action == MotionEvent.ACTION_MOVE) {
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

    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            }
            else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };

    public static void clearState() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.v(logTag, "Usually this is not a problem.");
            }
        }
        mediaPlayer = null;
    }

    private OnErrorListener mediaPlayerErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(logTag, "Error in MediaPlayer: (" + what + ") with extra ("
                    + extra + ")");
            clearState();
            return false;
        }
    };

    private OnCompletionListener normalOnCompletionListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            try {
                if (Contents.shuffle == true) {
                    startSong(Contents.getRandomSong());
                }
                else if (Contents.repeat == true) {
                    mp.seekTo(0);
                    mp.start();
                    queueNextRefresh(refreshNow());
                }
                else {
                    startSong(Contents.getNextSong());
                }
            } catch (IndexOutOfBoundsException e) {
                handler.removeMessages(REFRESH);
                stopNotification();
                clearState();
                finish();
                return;
            }
        }
    };
};