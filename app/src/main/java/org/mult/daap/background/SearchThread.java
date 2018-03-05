package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.client.Song;

import java.util.ArrayList;
import java.util.Observable;

public class SearchThread extends Observable implements Runnable {
    private final String searchQuery;
    private ArrayList<Song> lastMessage;

    public SearchThread(String searchQuery) {
       this.searchQuery = searchQuery;
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
        ArrayList<Song> srList = new ArrayList<>();
        String upperSearchQuery = searchQuery.toUpperCase();
        for (Song s : Contents.songList) {
            if (s.name.toUpperCase().contains(upperSearchQuery) ||
                s.artist.toUpperCase().contains(upperSearchQuery) ||
                s.album.toUpperCase().contains(upperSearchQuery)) {
                srList.add(s);
            }
        }
        notifyAndSet(srList);
    }
}
