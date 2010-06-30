package org.mult.daap.background;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.mult.daap.Contents;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Log;

public class DownloadListener extends Observable implements Observer
{
    private boolean startedPlayback;
    private boolean streaming;
    private Integer lastMessage;

    public DownloadListener(boolean streaming)
    {
        this.startedPlayback = false;
        this.streaming = streaming;
        notifyAndSet(Downloader.INITIALIZED);
    }

    public void update(Observable observable, Object data)
    {
        try
        {
            Downloader downloader = (Downloader) observable;
            int messageValue = ((Integer) data).intValue();
            if (messageValue == Downloader.SDERROR.floatValue())
            {
                notifyAndSet(Downloader.SDERROR);
                return;
            }
            if (messageValue == Downloader.PROGRESS_UPDATAE.intValue())
            {
                if (streaming && startedPlayback == false)
                {
                    // can we stream?
                    if ((int) downloader.getDownloadProgress() >= 10
                            && downloader.estimateMillisToCompletion() < downloader
                                    .getMediaLength())
                    {
                        startPlayback(downloader, createPlayFile(downloader
                                .getDownloadedFile()), 0, true);
                    }
                }
            }
            else if (messageValue == Downloader.FINISHED_DOWNLOAD.intValue())
            {
                Log.v("DownloadListener", "Finished was sent");
                notifyAndSet(Downloader.FINISHED_DOWNLOAD);
                downloader.deleteObservers();
                downloader.interrupt();
                if (startedPlayback)
                {
                    MediaPlayer mediaPlayer = Contents.mediaPlayer;
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    boolean wasPlaying = mediaPlayer.isPlaying();
                    startPlayback(downloader, downloader.getDownloadedFile()
                            .getAbsolutePath(), currentPosition, wasPlaying);
                }
                else
                {
                    startPlayback(downloader, downloader.getDownloadedFile()
                            .getAbsolutePath(), 0, true);
                }
                // No longer need this
                new File(downloader.getDownloadedFile().getAbsolutePath()
                        + "-play").delete();
                Contents.mediaPlayer
                        .setOnCompletionListener(normalOnCompletionListener);
                Contents.downloadThread = null;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            notifyAndSet(Downloader.MEDIA_PLAYBACK_ERROR);
        }
    }

    private void startPlayback(Downloader downloader, String filename,
            int position, boolean start)
    {
        MediaPlayer oldMediaPlayer = Contents.mediaPlayer;
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(mediaPlayerErrorListener);
        mediaPlayer.setOnCompletionListener(streamingCompletionListener);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try
        {
            FileInputStream fis = new FileInputStream(filename);
            mediaPlayer.setDataSource(fis.getFD(), 0, downloader
                    .getMediaLengthInBytes());
            mediaPlayer.prepare();
            mediaPlayer.seekTo(position);
            if (oldMediaPlayer != null && start)
            {
                oldMediaPlayer.pause();
                mediaPlayer.seekTo(oldMediaPlayer.getCurrentPosition());
                mediaPlayer.start();
            }
            else if (oldMediaPlayer != null && !start)
            {
                oldMediaPlayer.pause();
                mediaPlayer.seekTo(oldMediaPlayer.getCurrentPosition());
            }
            else
            {
                // no old media player
                mediaPlayer.seekTo(position);
                if (start)
                    mediaPlayer.start();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        Contents.mediaPlayer = mediaPlayer;
        Contents.currentlyPlayingFile = new File(filename);
        if (oldMediaPlayer != null)
        {
            oldMediaPlayer.release();
        }
        notifyAndSet(Downloader.STARTED_PLAYBACK);
        startedPlayback = true;
    }

    public void triggerChange(MediaPlayer mediaPlayer)
    {
        Downloader downloader = Contents.downloadThread;
        if (downloader != null)
            downloader.deleteObservers();
        else
            return;
        if (mediaPlayer != null)
        {
            int currentPosition = mediaPlayer.getCurrentPosition();
            try
            {
                startPlayback(downloader, createPlayFile(downloader
                        .getDownloadedFile()), currentPosition, true);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        downloader.addObserver(this);
        update(downloader, downloader.getLastMessage());
    }

    private OnErrorListener mediaPlayerErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra)
        {
            Log.e(getClass().getName(), "Error in MediaPlayer: (" + what
                    + ") with extra (" + extra + ")");
            return false;
        }
    };

    private String createPlayFile(File downloadFile) throws IOException
    {
        String playFile = downloadFile.getAbsolutePath() + "-play";
        copyFile(downloadFile, playFile);
        return playFile;
    }

    public static void copyFile(File fromFile, String toString)
            throws IOException, FileNotFoundException
    {
        FileInputStream from = new FileInputStream(fromFile);
        FileOutputStream to = new FileOutputStream(toString);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = from.read(buffer)) != -1)
            to.write(buffer, 0, bytesRead); // write
        if (from != null)
            from.close();
        if (to != null)
            to.close();
    }

    public Integer getLastMessage()
    {
        return lastMessage;
    }

    private void notifyAndSet(Integer value)
    {
        lastMessage = value;
        setChanged();
        notifyObservers(value);
    }

    private OnCompletionListener streamingCompletionListener = new OnCompletionListener() {

        public void onCompletion(MediaPlayer mediaPlayer)
        {
            // We only got here if we were playing, so pass true
            notifyAndSet(Downloader.TRANSFERRING_SONG);
            triggerChange(mediaPlayer);
            notifyAndSet(Downloader.STARTED_PLAYBACK);
        }
    };

    private OnCompletionListener normalOnCompletionListener = new OnCompletionListener() {

        public void onCompletion(MediaPlayer mp)
        {
            if (mp != null)
            {
                try
                {
                    if (Contents.repeat)
                    {
                        notifyAndSet(Downloader.REPEAT_SONG);
                        return;
                    }
                    else if (Contents.shuffle)
                    {
                        Contents.setRandomSong();
                        notifyAndSet(Downloader.START_NEXT_SONG);
                        return;
                    }
                    else
                    {
                        Contents.setNextSong();
                        notifyAndSet(Downloader.START_NEXT_SONG);
                        return;
                    }
                } catch (IndexOutOfBoundsException e)
                {
                    notifyAndSet(Downloader.STOP_NOTIFICATION);
                    Contents.clearState();
                    return;
                }
            }
        }
    };
}