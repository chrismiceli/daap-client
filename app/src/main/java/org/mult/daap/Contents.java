package org.mult.daap;

import org.mult.daap.background.SearchThread;
import org.mult.daap.client.Host;
import org.mult.daap.db.entity.SongEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class Contents {
    public static final ArrayList<SongEntity> songList = new ArrayList<>();
    public static ArrayList<SongEntity> filteredAlbumSongList = new ArrayList<>();
    public static ArrayList<SongEntity> filteredArtistSongList = new ArrayList<>();
    public static final ArrayList<SongEntity> queue = new ArrayList<>(10);
    private static ArrayList<SongEntity> activeList = new ArrayList<>();
    public static final ArrayList<String> artistNameList = new ArrayList<>();
    public static final ArrayList<String> albumNameList = new ArrayList<>();
    public static ArrayList<String> artistAlbumNameList = new ArrayList<>();
    public static TreeMap<String, ArrayList<Integer>> ArtistAlbumElements = new TreeMap<>();
    public static Host daapHost;
    public static SearchThread searchResult;
    public static boolean shuffle = false;
    public static boolean repeat = false;
    private static int position = 0;

    public static void setSongPosition(ArrayList<SongEntity> list, int id) {
        activeList = list;
        Contents.position = id;
    }

    public static SongEntity getSong() throws IndexOutOfBoundsException {
        SongEntity song;
        // Not the queue
        if (activeList.size() > 0 && position < activeList.size()
                && position >= 0) {
            song = activeList.get(position);
            return song;
        } else {
            throw new IndexOutOfBoundsException("End of list");
        }
    }

    public static SongEntity getNextSong() throws IndexOutOfBoundsException {
        position++;
        return getSong();
    }

    public static SongEntity getRandomSong() throws IndexOutOfBoundsException {
        position = new Random(System.currentTimeMillis()).nextInt(activeList
                .size());
        return getSong();
    }

    public static SongEntity getPreviousSong() {
        position--;
        return getSong();
    }

    public static void clearLists() {
        songList.clear();
        queue.clear();
        artistNameList.clear();
        albumNameList.clear();
    }

    public static void addToQueue(SongEntity s) throws IndexOutOfBoundsException {
        // TODO: need to invalidate the context menu for the songs fragment
        if (queue.size() > 9) {
            throw new IndexOutOfBoundsException("Can't add more than 10");
        } else {
            queue.add(s);
        }
    }
}