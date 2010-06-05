package org.mult.daap.background;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Observable;

import org.mult.daap.Contents;
import org.mult.daap.MediaPlayback;
import org.mult.daap.client.Song;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Downloader extends Observable implements Runnable
{
    private Context context;
    private Song mediaUrl;
    private int mediaLengthInBytes;
    private int totalBytesRead;
    private Integer lastMessage;
    private File downloadedFile;
    private long start;
    private DownloadListener downloadListener;
    public static final Integer MEDIA_PLAYBACK_ERROR = new Integer(-2);
    public static final Integer SDERROR = new Integer(-1);
    public static final Integer INITIALIZED = new Integer(0);
    public static final Integer PROGRESS_UPDATAE = new Integer(1);
    public static final Integer FINISHED_DOWNLOAD = new Integer(2);
    public static final Integer TRANSFERRING_SONG = new Integer(3);
    public static final Integer STARTED_PLAYBACK = new Integer(4);
    public static final Integer STOP_NOTIFICATION = new Integer(5);
    public static final Integer START_NEXT_SONG = new Integer(6);

    private boolean interrupted;
    private boolean finished;

    public Downloader(MediaPlayback mediaPlaybackClass, Song mediaUrl)
    {
        this.mediaUrl = mediaUrl;
        this.mediaLengthInBytes = mediaUrl.size;
        this.totalBytesRead = 0;
        this.interrupted = false;
        this.finished = false;
        this.context = mediaPlaybackClass.getApplicationContext();
        SharedPreferences mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean streaming = mPrefs.getBoolean("streaming_pref", true);
        this.downloadListener = new DownloadListener(streaming);
        this.downloadListener.addObserver(mediaPlaybackClass);
        this.addObserver(this.downloadListener);
        notifyAndSet(INITIALIZED);
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

    public int getMediaLengthInBytes()
    {
        return mediaLengthInBytes;
    }

    public float getDownloadProgress()
    {
        if (mediaLengthInBytes <= 0)
        {
            return 0;
        }
        else
        {
            return (float) ((totalBytesRead / (float) mediaLengthInBytes) * 100.0);
        }
    }

    public File getDownloadedFile()
    {
        return downloadedFile;
    }

    public void run()
    {
        try
        {
            BufferedInputStream stream;
            try
            {
                stream = new BufferedInputStream(Contents.daapHost
                        .getSongStream(mediaUrl), 8192);
            } catch (Exception e)
            {
                e.printStackTrace();
                return;
            }
            mediaLengthInBytes = mediaUrl.size;
            byte buf[] = new byte[16384];
            totalBytesRead = 0;
            SharedPreferences mPrefs;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean sd = mPrefs.getBoolean("sd_as_cache", false);
            if (sd == false)
            {
                downloadedFile = new File(context.getCacheDir(),
                        "downloadingMedia.dat");
            }
            else
            {
                if (isSDPresent() == false)
                {
                    notifyAndSet(SDERROR);
                    return;
                }
                File dir = new File("/sdcard/daap-cache");
                if (dir.exists() == false)
                {
                    new File("/sdcard/daap-cache").mkdir();
                }
                downloadedFile = new File("/sdcard/daap-cache",
                        "downloadingMedia.dat");
                dir.deleteOnExit();
            }
            if (downloadedFile.exists())
            {
                downloadedFile.delete();
            }
            downloadedFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(downloadedFile);
            start = System.currentTimeMillis();
            while (totalBytesRead < mediaLengthInBytes)
            {
                if (isInterrupted())
                {
                    downloadedFile.delete();
                    out.close();
                    stream.close();
                    return; // kills this thread
                }
                int numBytesRead = stream.read(buf);
                out.write(buf, 0, numBytesRead);
                notifyAndSet(PROGRESS_UPDATAE);
                if (numBytesRead <= 0)
                {
                    break;
                }
                totalBytesRead += numBytesRead;
            }
            out.close();
            stream.close();
            finished = true;
            notifyAndSet(FINISHED_DOWNLOAD);
        } catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
    }

    public static boolean isSDPresent()
    {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    private boolean isInterrupted()
    {
        if (interrupted == true)
            return true;
        return false;
    }

    public void interrupt()
    {
        interrupted = true;
    }

    public double estimateMillisToCompletion()
    {
        double rate = totalBytesRead / (System.currentTimeMillis() - start);
        return (mediaLengthInBytes - totalBytesRead) / rate;
    }

    public boolean finished()
    {
        return finished;
    }

    public int getMediaLength()
    {
        return mediaUrl.time;
    }

    public DownloadListener getDownloadListener()
    {
        return downloadListener;
    }
}