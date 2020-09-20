package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.client.Song;

import java.util.ArrayList;
import java.util.Observable;

public class SearchThread extends Observable implements Runnable {
    public final static Integer INITIATED = -1;
    public final static Integer CONNECTION_FINISHED = 1;
    public final static Integer ERROR = 2;
    private final String searchQuery;
    private ArrayList<Song> lastMessage;
    public ArrayList<Song> srList = null;

    public SearchThread(String sQ) {
        searchQuery = sQ;
        lastMessage = null;
    }

    public ArrayList<Song> getLastMessage() {
        return lastMessage;
    }

    private void notifyAndSet(ArrayList<Song> value) {
        lastMessage = value;
        setChanged();
        notifyObservers(value);
    }

    public void run() {
        srList = new ArrayList<>();
        for (Song s : Contents.songList) {
            if (s.name.toUpperCase().contains(searchQuery.toUpperCase())
                    || s.artist.toUpperCase().contains(searchQuery.toUpperCase())
                    || s.album.toUpperCase().contains(searchQuery.toUpperCase())) {
                srList.add(s);
            }
        }
        notifyAndSet(srList);
    }
}
