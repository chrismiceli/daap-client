package org.mult.daap;

import org.mult.daap.background.SearchThread;
import org.mult.daap.client.Host;
import org.mult.daap.client.ISong;
import org.mult.daap.comparator.SongNameComparator;
import org.mult.daap.comparator.StringIgnoreCaseComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;

public class Contents {
    public static ArrayList<ISong> songList = new ArrayList<>();
    public static ArrayList<ISong> filteredAlbumSongList = new ArrayList<>();
    public static ArrayList<ISong> filteredArtistSongList = new ArrayList<>();
    public static ArrayList<ISong> queue = new ArrayList<>(10);
    private static ArrayList<ISong> activeList = new ArrayList<>();
    public static ArrayList<String> stringElements = new ArrayList<>();
    public static ArrayList<String> artistNameList = new ArrayList<>();
    public static ArrayList<String> albumNameList = new ArrayList<>();
    public static ArrayList<String> artistAlbumNameList = new ArrayList<>();
    public static TreeMap<String, ArrayList<Integer>> ArtistElements = new TreeMap<>();
    public static TreeMap<String, ArrayList<Integer>> AlbumElements = new TreeMap<>();
    public static TreeMap<String, ArrayList<Integer>> ArtistAlbumElements = new TreeMap<>();
    public static Host daapHost;
    public static SearchThread searchResult;
    public static boolean shuffle = false;
    public static boolean repeat = false;
    private static int position = 0;

    public static void songListAdd(ISong s) {
        Contents.songList.add(s);
        Contents.stringElements.add(s.toString());
    }

    public static void setSongPosition(ArrayList<ISong> list, int id) {
        activeList = list;
        Contents.position = id;
    }

    public static ISong getSong() throws IndexOutOfBoundsException {
        ISong song;
        // Not the queue
        if (activeList.size() > 0 && position < activeList.size()
                && position >= 0) {
            song = activeList.get(position);
            return song;
        } else {
            throw new IndexOutOfBoundsException("End of list");
        }
    }

    public static ISong getNextSong() throws IndexOutOfBoundsException {
        position++;
        return getSong();
    }

    public static ISong getRandomSong() throws IndexOutOfBoundsException {
        position = new Random(System.currentTimeMillis()).nextInt(activeList
                .size());
        return getSong();
    }

    public static ISong getPreviousSong() {
        position--;
        return getSong();
    }

    public static void sortLists() {
        Comparator<ISong> snc = new SongNameComparator();
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

    public static void addToQueue(ISong s) throws IndexOutOfBoundsException {
        if (queue.size() > 9) {
            throw new IndexOutOfBoundsException("Can't add more than 10");
        } else {
            queue.add(s);
        }
    }
}