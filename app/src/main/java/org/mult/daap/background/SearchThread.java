package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.client.ISong;

import java.util.ArrayList;
import java.util.Observable;

public class SearchThread extends Observable implements Runnable {
    private final String searchQuery;
    private ArrayList<ISong> lastMessage;

    public SearchThread(String searchQuery) {
       this.searchQuery = searchQuery;
    }

    public ArrayList<ISong> getLastMessage() {
      return lastMessage;
   }

    private void notifyAndSet(ArrayList<ISong> value) {
       lastMessage = value;
       setChanged();
       notifyObservers(value);
    }

    public void run() {
        ArrayList<ISong> srList = new ArrayList<>();
        String upperSearchQuery = searchQuery.toUpperCase();
        for (ISong s : Contents.songList) {
            if (s.getName().toUpperCase().contains(upperSearchQuery) ||
                s.getArtist().toUpperCase().contains(upperSearchQuery) ||
                s.getAlbum().toUpperCase().contains(upperSearchQuery)) {
                srList.add(s);
            }
        }
        notifyAndSet(srList);
    }
}
