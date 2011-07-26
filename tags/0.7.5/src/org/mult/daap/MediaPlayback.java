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
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import org.mult.daap.background.DownloadListener;
import org.mult.daap.background.Downloader;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MediaPlayback extends Activity implements View.OnTouchListener,
        View.OnLongClickListener, Observer
{
    public static final int REFRESH = 1;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private ImageButton mPrevButton;
    private ImageButton mNextButton;
    private ImageButton mPauseButton;
    private SeekBar mProgress;

    private static final int MENU_STOP = 0;
    private static final int MENU_DOWNLOAD = 1;
    private static final int MENU_REPEAT = 2;
    private static final int MENU_SHUFFLE = 3;
    private int mTouchSlop;
    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private boolean mDraggingLabel = false;

    public MediaPlayback()
    {}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        setResult(Activity.RESULT_OK);
        if (Contents.address == null)
        {
            // We got kicked out of memory probably
            SharedPreferences mPrefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
            if (mPrefs.getBoolean("sd_as_cache", false) == true)
            {
                deleteDirectory(new File("/sdcard/daap-cache/"));
            }
            else
            {
                File dm = new File(getCacheDir(), "downloadingMedia.dat");
                if (dm.exists())
                {
                    dm.delete();
                }
            }
            Contents.clearState();
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
    public void onResume()
    {
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
        mProgress.setOnSeekBarChangeListener(mSeekListener);
        setUpActivity();

        Downloader downloadThread = Contents.downloadThread;
        MediaPlayer mediaPlayer = Contents.mediaPlayer;
        if (downloadThread == null && mediaPlayer == null)
        {
            // We're not getting nor playing anything
            Log.v("MediaPlayback", "Got handle downloading");
            handleDownloading();
        }
        else if (downloadThread == null && mediaPlayer != null)
        {
            // We're done getting the file but we do have a media player
            // do nothing as queueNextRefresh is called below
        }
        else if (downloadThread != null && mediaPlayer == null)
        {
            // we are getting the file
            DownloadListener downloadListener = downloadThread
                    .getDownloadListener();
            downloadListener.addObserver(this);
            this.update(downloadListener, downloadListener.getLastMessage());
        }
        else
        // if (downloadThread != null && mediaPlayer != null)
        {
            // we're streaming
            DownloadListener downloadListener = downloadThread
                    .getDownloadListener();
            downloadListener.addObserver(this);
            this.update(downloadListener, downloadListener.getLastMessage());
        }
        queueNextRefresh(refreshNow());
    }

    protected void handleDownloading()
    {
        // Fire off a thread away from the UI thread
        Downloader downloadThread = Contents.downloadThread;
        if (downloadThread != null)
        {
            downloadThread.interrupt();
            downloadThread.deleteObservers();
        }
        Contents.clearState();
        try
        {
            downloadThread = new Downloader(this, Contents.getSong());
        } catch (IndexOutOfBoundsException e)
        {
            handler.removeMessages(REFRESH);
            stopNotification();
            finish();
            return;
        }
        Contents.downloadThread = downloadThread;
        mCurrentTime.setText("");
        mTotalTime.setText("");
        mProgress.setSecondaryProgress(0);
        mProgress.setProgress(0);
        mPauseButton.setEnabled(false);
        startNotification();
        queueNextRefresh(refreshNow());
        new Thread(downloadThread).start();
    }

    private void setUpActivity()
    {
        try
        {
            mArtistName.setText(Contents.getSong().artist);
            mAlbumName.setText(Contents.getSong().album);
            mTrackName.setText(Contents.getSong().name);
            mProgress.setProgress(0);
            mProgress.setSecondaryProgress(0);
        } catch (IndexOutOfBoundsException e)
        {
            handler.removeMessages(REFRESH);
            stopNotification();
            Contents.clearState();
            finish();
            return;
        }
    }

    public void update(Observable observable, Object data)
    {
        int message = ((Integer) data).intValue();
        if (message == Downloader.FINISHED_DOWNLOAD.intValue())
        {
            // prevent mediaplayback issues
            handler.removeMessages(REFRESH);
        }
        else if (message == Downloader.STARTED_PLAYBACK.intValue())
        {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        else if (message == Downloader.TRANSFERRING_SONG.intValue())
        {
            // mPauseButton.setEnabled(false);
            queueNextRefresh(refreshNow());
        }
        else if (message == Downloader.START_NEXT_SONG.intValue())
        {
            stopNotification();
            handleDownloading();
        }
        else if (message == Downloader.REPEAT_SONG.intValue())
        {
            Contents.mediaPlayer.seekTo(0);
            Contents.mediaPlayer.start();
        }
        else if (message == Downloader.SDERROR.intValue())
        {
            Toast tst1 = Toast.makeText(MediaPlayback.this,
                    getString(R.string.no_sdcard_error_message_playback),
                    Toast.LENGTH_LONG);
            tst1.setGravity(Gravity.CENTER, tst1.getXOffset() / 2, tst1
                    .getYOffset() / 2);
            tst1.show();
            stopNotification();
            Contents.clearState();
            observable.deleteObservers();
            finish();
            return;
        }
        else if (message == Downloader.MEDIA_PLAYBACK_ERROR.intValue())
        {
            Toast tst = Toast
                    .makeText(MediaPlayback.this,
                            getString(R.string.media_playback_error),
                            Toast.LENGTH_LONG);
            tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                    .getYOffset() / 2);
            tst.show();
            stopNotification();
            Contents.clearState();
            observable.deleteObservers();
            finish();
            return;
        }
        else if (message == Downloader.STOP_NOTIFICATION.intValue())
        {
            stopNotification();
        }
        handler.sendEmptyMessage(REFRESH);
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case REFRESH:
                    queueNextRefresh(refreshNow());
                    break;
            }
        }
    };

    public void startNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(
                R.drawable.stat_notify_musicplayer, Contents.getSong().name,
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, getIntent(), 0);
        notification.setLatestEventInfo(getApplicationContext(), Contents
                .getSong().name, Contents.getSong().artist, contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManager.notify(0, notification);
    }

    public void stopNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar)
        {
        // intentionally left blank
        }

        public void onProgressChanged(SeekBar bar, int progress,
                boolean fromuser)
        {
            MediaPlayer mediaPlayer = Contents.mediaPlayer;
            if (!fromuser)
            {
                return;
            }
            if (mediaPlayer == null)
            {
                bar.setProgress(0);
                return;
            }
            else
            {
                double doubleProgress = (double) progress;
                double doubleDuration;
                // get correct length of the song we are going to seek in
                if (Contents.downloadThread != null)
                    doubleDuration = Contents.downloadThread.getMediaLength();
                else
                    doubleDuration = mediaPlayer.getDuration();
                int desiredSeek = (int) ((doubleProgress / 100.0) * doubleDuration);
                if (Contents.downloadThread == null)
                {
                    mediaPlayer.seekTo(desiredSeek);
                }
                handler.removeMessages(REFRESH);
                queueNextRefresh(refreshNow());
            }
        }

        public void onStopTrackingTouch(SeekBar seekBar)
        {
        // intentionally left blank
        }
    };

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v)
        {
            if (Contents.mediaPlayer != null)
            {
                if (Contents.mediaPlayer.isPlaying())
                {
                    Contents.mediaPlayer.pause();
                    stopNotification();
                }
                else
                {
                    Contents.mediaPlayer.start();
                    startNotification();
                }
                setPauseButton();
                queueNextRefresh(refreshNow());
            }
        }
    };

    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v)
        {
            try
            {
                Contents.setPreviousSong();
            } catch (IndexOutOfBoundsException e)
            {
                handler.removeMessages(REFRESH);
                stopNotification();
                Contents.clearState();
                finish();
                return;
            }
            stopNotification();
            setUpActivity();
            handleDownloading();
        }
    };

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v)
        {
            try
            {
                if (Contents.shuffle)
                {
                    Contents.setRandomSong();
                }
                else
                {
                    Contents.setNextSong();
                }
            } catch (IndexOutOfBoundsException e)
            {
                handler.removeMessages(REFRESH);
                stopNotification();
                Contents.clearState();
                finish();
                return;
            }
            stopNotification();
            setUpActivity();
            handleDownloading();
        }
    };

    @Override
    public void onPause()
    {
        handler.removeMessages(REFRESH);
        super.onPause();
    }

    @Override
    public void onStop()
    {
        handler.removeMessages(REFRESH);
        super.onStop();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        queueNextRefresh(refreshNow());
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        setIntent(intent);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        handler.removeMessages(REFRESH);
        if (Contents.downloadThread != null)
        {
            Contents.downloadThread.getDownloadListener().deleteObservers();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        if (Downloader.isSDPresent() && Contents.downloadThread == null
                && Contents.mediaPlayer != null)
        {
            menu.findItem(MENU_DOWNLOAD).setEnabled(true);
        }
        else
        {
            menu.findItem(MENU_DOWNLOAD).setEnabled(false);
        }
        if (Contents.shuffle)
        {
            menu.findItem(MENU_SHUFFLE).setIcon(R.drawable.ic_menu_shuffle_on);
        }
        else
        {
            menu.findItem(MENU_SHUFFLE).setIcon(R.drawable.ic_menu_shuffle);
        }
        if (Contents.repeat)
        {
            menu.findItem(MENU_REPEAT).setIcon(R.drawable.ic_menu_repeat_on);
        }
        else
        {
            menu.findItem(MENU_REPEAT).setIcon(R.drawable.ic_menu_repeat);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_SHUFFLE:
                Contents.shuffle = !Contents.shuffle;
                break;
            case MENU_REPEAT:
                Contents.repeat = !Contents.repeat;
                break;
            case MENU_STOP:
                Downloader downloadThread = Contents.downloadThread;
                if (downloadThread != null)
                {
                    downloadThread.interrupt();
                    downloadThread.deleteObservers();
                }
                Contents.clearState();
                stopNotification();
                Contents.downloadThread = null;
                finish();
                break;
            case MENU_DOWNLOAD:
                if (Downloader.isSDPresent() && Contents.mediaPlayer != null
                        && Contents.downloadThread == null)
                {
                    boolean isPlaying = Contents.mediaPlayer.isPlaying();
                    try
                    {
                        String directory = "/sdcard/media/audio/DAAP/";
                        String destination = directory + Contents.getSong().id
                                + "." + Contents.getSong().format;
                        File directoryFile = new File(directory);
                        if (directoryFile.exists() == false)
                        {
                            if (directoryFile.mkdirs() == false)
                            {
                                throw new IOException(
                                        "Could not create directories");
                            }
                        }
                        handler.removeMessages(REFRESH);
                        Contents.mediaPlayer.pause();
                        DownloadListener.copyFile(
                                Contents.currentlyPlayingFile, destination);
                        if (isPlaying)
                            Contents.mediaPlayer.start();
                        queueNextRefresh(refreshNow());
                        Toast tst = Toast
                                .makeText(MediaPlayback.this,
                                        getString(R.string.save_complete)
                                                + destination,
                                        Toast.LENGTH_LONG);
                        tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                                tst.getYOffset() / 2);
                        tst.show();
                    } catch (Exception e)
                    {
                        if (isPlaying)
                            Contents.mediaPlayer.start();
                        queueNextRefresh(refreshNow());
                        Toast tst = Toast.makeText(MediaPlayback.this,
                                getString(R.string.error_title),
                                Toast.LENGTH_LONG);
                        tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                                tst.getYOffset() / 2);
                        tst.show();
                        e.printStackTrace();
                    }
                }
        }
        return true;
    }

    private void setPauseButton() throws IllegalStateException
    {
        try
        {
            if (Contents.mediaPlayer != null)
            {
                mPauseButton.setEnabled(true);
                if (Contents.mediaPlayer.isPlaying())
                {
                    mPauseButton
                            .setImageResource(android.R.drawable.ic_media_pause);
                }
                else
                {
                    mPauseButton
                            .setImageResource(android.R.drawable.ic_media_play);
                }
            }
            else
            {
                mPauseButton.setEnabled(false);
            }
        } catch (IllegalStateException e)
        {
            throw e;
        }
    }

    private void queueNextRefresh(long delay)
    {
        handler.removeMessages(REFRESH);
        handler.sendEmptyMessageDelayed(REFRESH, delay);
    }

    private long refreshNow()
    {
        try
        {
            if (Contents.downloadThread != null)
            {
                mProgress.setSecondaryProgress((int) Contents.downloadThread
                        .getDownloadProgress());
            }
            else
            {
                mProgress.setSecondaryProgress(100);
            }
            if (Contents.mediaPlayer == null)
            {
                return 500;
            }
            mTotalTime.setText(makeTimeString(Contents.getSong().time / 1000));
            int mDuration = Contents.getSong().time;
            setPauseButton();
            long pos = Contents.mediaPlayer.getCurrentPosition();
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0))
            {
                mCurrentTime.setText(makeTimeString(pos / 1000));
                if (Contents.mediaPlayer.isPlaying())
                {
                    mCurrentTime.setVisibility(View.VISIBLE);
                }
                else
                {
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
        } catch (IllegalStateException e)
        {
            return 500;
        }
    }

    private static String makeTimeString(long secs)
    {
        String durationformat = (secs < 3600 ? "%2$d:%5$02d"
                : "%1$d:%3$02d:%5$02d");
        StringBuilder sFormatBuilder = new StringBuilder();
        Formatter sFormatter = new Formatter(sFormatBuilder, Locale
                .getDefault());

        sFormatBuilder.setLength(0);

        final Object[] timeArgs = new Object[5];
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }

    public boolean onLongClick(View arg0)
    {
        return false;
    }

    static public boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    deleteDirectory(files[i]);
                }
                else
                {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    TextView textViewForContainer(View v)
    {
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

    public boolean onTouch(View v, MotionEvent event)
    {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null)
        {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN)
        {
            v.setBackgroundColor(0xff606060);
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
        }
        else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL)
        {
            v.setBackgroundColor(0);
            if (mDraggingLabel)
            {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        }
        else if (action == MotionEvent.ACTION_MOVE)
        {
            if (mDraggingLabel)
            {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0)
                {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth)
                    {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth)
                    {
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
            if (Math.abs(delta) > mTouchSlop)
            {
                mLabelScroller.removeMessages(0, tv);
                if (tv.getEllipsize() != null)
                {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                if (ll == null)
                {
                    return false;
                }
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth)
                {
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
        public void handleMessage(Message msg)
        {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0)
            {
                tv.setEllipsize(TruncateAt.END);
            }
            else
            {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumsber)
        {
            switch (state)
            {
                // change to idle
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (Contents.mediaPlayer != null)
                    {
                        Contents.mediaPlayer.pause();
                    }
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    if (Contents.mediaPlayer != null)
                    {
                        Contents.mediaPlayer.pause();
                    }
                    break;
            }
        }
    };
};