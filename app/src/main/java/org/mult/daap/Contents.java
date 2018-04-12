package org.mult.daap;

import org.mult.daap.background.GetSongsForPlaylist;
import org.mult.daap.background.SearchThread;
import org.mult.daap.client.Host;
import org.mult.daap.client.Song;
import org.mult.daap.comparator.SongNameComparator;
import org.mult.daap.comparator.StringIgnoreCaseComparator;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;

public class Contents {
    public static ArrayList<Song> songList = new ArrayList<>();
    public static ArrayList<Song> filteredAlbumSongList = new ArrayList<>();
    public static ArrayList<Song> filteredArtistSongList = new ArrayList<>();
    public static ArrayList<Song> queue = new ArrayList<>(10);
    public static ArrayList<Song> activeList = new ArrayList<>();
    public static ArrayList<String> stringElements = new ArrayList<>();
    public static ArrayList<String> artistNameList = new ArrayList<>();
    public static ArrayList<String> albumNameList = new ArrayList<>();
    public static ArrayList<String> artistAlbumNameList = new ArrayList<>();
    public static TreeMap<String, ArrayList<Integer>> ArtistElements = new TreeMap<>();
    public static TreeMap<String, ArrayList<Integer>> AlbumElements = new TreeMap<>();
    public static TreeMap<String, ArrayList<Integer>> ArtistAlbumElements = new TreeMap<>();
    public static Host daapHost;
    public static GetSongsForPlaylist getSongsForPlaylist = null;
    public static InetAddress address;
    public static SearchThread searchResult;
    public static int playlist_id = -1;
    public static boolean shuffle = false;
    public static boolean repeat = false;
    public static boolean lastUsedAlbumActivity = false;
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