package org.mult.daap;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;

import org.mult.daap.background.Downloader;
import org.mult.daap.background.GetSongsForPlaylist;
import org.mult.daap.background.LoginManager;
import org.mult.daap.background.SearchThread;
import org.mult.daap.client.Song;
import org.mult.daap.client.SongNameComparator;
import org.mult.daap.client.StringIgnoreCaseComparator;
import org.mult.daap.client.daap.DaapHost;

import android.media.MediaPlayer;
import android.util.Log;

public class Contents
{
    public static ArrayList<Song> songList = new ArrayList<Song>();
    public static ArrayList<Song> filteredAlbumSongList = new ArrayList<Song>();
    public static ArrayList<Song> filteredArtistSongList = new ArrayList<Song>();
    public static ArrayList<Song> queue = new ArrayList<Song>(10);
    public static ArrayList<Song> activeList = new ArrayList<Song>();
    public static ArrayList<String> stringElements = new ArrayList<String>();
    public static ArrayList<String> artistNameList = new ArrayList<String>();
    public static ArrayList<String> albumNameList = new ArrayList<String>();
    public static TreeMap<String, ArrayList<Integer>> ArtistElements = new TreeMap<String, ArrayList<Integer>>();
    public static TreeMap<String, ArrayList<Integer>> AlbumElements = new TreeMap<String, ArrayList<Integer>>();
    public static DaapHost daapHost;
    public static Downloader downloadThread = null;
    public static GetSongsForPlaylist getSongsForPlaylist = null;
    public static InetAddress address;
    public static LoginManager loginManager;
    public static SearchThread searchResult;
    public static MediaPlayer mediaPlayer;
    public static short playlist_position = -1;
    public static File currentlyPlayingFile;
    public static boolean shuffle = false;
    public static boolean repeat = false;

    private static int position = 0;

    public static void songListAdd(Song s)
    {
        Contents.songList.add(s);
        Contents.stringElements.add(s.toString());
    }

    public static void setSongPosition(ArrayList<Song> list, int id)
    {
        activeList = list;
        Contents.position = id;
    }

    public static Song getSong()
    {
        return activeList.get(position);
    }

    public static void setNextSong()
    {
        if (activeList == queue)
        {
            queue.remove(0);
            position--; // queue is different
        }
        if (position + 1 >= activeList.size())
        {
            throw new IndexOutOfBoundsException("End of list");
        }
        else
        {
            position++;
        }
    }

    public static void setRandomSong()
    {
        Random random = new Random(System.currentTimeMillis());
        if (activeList == queue)
        {
            queue.remove(position);
            if (activeList.size() == 0)
                throw new IndexOutOfBoundsException("End of list");
        }
        position = random.nextInt(activeList.size());
        Log.v("Contents", "position = " + position);
    }

    public static void setPreviousSong()
    {
        if (position - 1 < 0)
        {
            throw new IndexOutOfBoundsException("Beginning of list");
        }
        else
        {
            position--;
        }
    }

    public static void sortLists()
    {
        Comparator<Song> snc = new SongNameComparator();
        Comparator<String> snicc = new StringIgnoreCaseComparator();
        Collections.sort(stringElements, snicc); // Must be sorted!
        Collections.sort(songList, snc);

    }

    public static void clearState()
    {
        if (downloadThread != null)
        {
            downloadThread.interrupt();
            downloadThread.deleteObservers();
        }
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = null;
        downloadThread = null;
    }

    public static void clearLists()
    {
        songList.clear();
        stringElements.clear();
        queue.clear();
        ArtistElements.clear();
        AlbumElements.clear();
        artistNameList.clear();
        albumNameList.clear();
    }

    public static void addToQueue(Song s) throws IndexOutOfBoundsException
    {
        if (queue.size() > 9)
        {
            throw new IndexOutOfBoundsException("Can't add more than 10");
        }
        else
        {
            queue.add(s);
        }
    }
}