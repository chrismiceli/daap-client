package org.mult.daap;

import org.mult.daap.background.GetSongsForPlaylist;
import org.mult.daap.background.LoginManager;
import org.mult.daap.background.SearchThread;
import org.mult.daap.client.Song;
import org.mult.daap.client.SongNameComparator;
import org.mult.daap.client.StringIgnoreCaseComparator;
import org.mult.daap.client.daap.DaapHost;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;

public class Contents {
    public static final ArrayList<Song> songList = new ArrayList<Song>();
    public static final ArrayList<Song> filteredAlbumSongList = new ArrayList<Song>();
    public static final ArrayList<Song> filteredArtistSongList = new ArrayList<Song>();
    public static final ArrayList<Song> queue = new ArrayList<Song>(10);
    public static ArrayList<Song> activeList = new ArrayList<Song>();
    public static final ArrayList<String> stringElements = new ArrayList<String>();
    public static final ArrayList<String> artistNameList = new ArrayList<String>();
    public static final ArrayList<String> albumNameList = new ArrayList<String>();
    public static final ArrayList<String> artistAlbumNameList = new ArrayList<String>();
    public static final TreeMap<String, ArrayList<Integer>> ArtistElements = new TreeMap<String, ArrayList<Integer>>();
    public static final TreeMap<String, ArrayList<Integer>> AlbumElements = new TreeMap<String, ArrayList<Integer>>();
    public static final TreeMap<String, ArrayList<Integer>> ArtistAlbumElements = new TreeMap<String, ArrayList<Integer>>();
    public static DaapHost daapHost;
    public static GetSongsForPlaylist getSongsForPlaylist = null;
    public static InetAddress address;
    public static LoginManager loginManager;
    public static SearchThread searchResult;
    public static short playlist_position = -1;
    public static boolean shuffle = false;
    public static boolean repeat = false;
    private static int position = 0;

    public static void songListAdd(Song s) {
        Contents.songList.add(s);
        Contents.stringElements.add(s.toString());
    }

    public static void setSongPosition(ArrayList<Song> list, int id) {
        activeList = list;
        Contents.position = id;
    }

    public static Song getSong() throws IndexOutOfBoundsException {
        Song song;
        // Not the queue
        if (activeList.size() > 0 && position < activeList.size()
                && position >= 0) {
            song = activeList.get(position);
            return song;
        } else {
            throw new IndexOutOfBoundsException("End of list");
        }
    }

    public static Song getNextSong() throws IndexOutOfBoundsException {
        position++;
        return getSong();
    }

    public static Song getRandomSong() throws IndexOutOfBoundsException {
        position = new Random(System.currentTimeMillis()).nextInt(activeList
                .size());
        return getSong();
    }

    public static Song getPreviousSong() {
        position--;
        return getSong();
    }

    public static void sortLists() {
        Comparator<Song> snc = new SongNameComparator();
        Comparator<String> snicc = new StringIgnoreCaseComparator();
        Collections.sort(stringElements, snicc); // Must be sorted!
        Collections.sort(songList, snc);
    }

    public static void clearLists() {
        songList.clear();
        stringElements.clear();
        queue.clear();
        ArtistElements.clear();
        AlbumElements.clear();
        artistNameList.clear();
        albumNameList.clear();
    }

    public static void addToQueue(Song s) throws IndexOutOfBoundsException {
        if (queue.size() > 9) {
            throw new IndexOutOfBoundsException("Can't add more than 10");
        } else {
            queue.add(s);
        }
    }
}