package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.db.entity.SongEntity;

import java.util.ArrayList;
import java.util.Observable;

public class SearchThread extends Observable implements Runnable {
    private final String searchQuery;
    private ArrayList<SongEntity> lastMessage;

    public SearchThread(String searchQuery) {
       this.searchQuery = searchQuery;
    }

    public ArrayList<SongEntity> getLastMessage() {
      return lastMessage;
   }

    private void notifyAndSet(ArrayList<SongEntity> value) {
       lastMessage = value;
       setChanged();
       notifyObservers(value);
    }

    public void run() {
        ArrayList<SongEntity> srList = new ArrayList<>();
        String upperSearchQuery = searchQuery.toUpperCase();
        for (SongEntity s : Contents.songList) {
            if (s.getName().toUpperCase().contains(upperSearchQuery) ||
                s.getArtist().toUpperCase().contains(upperSearchQuery) ||
                s.getAlbum().toUpperCase().contains(upperSearchQuery)) {
                srList.add(s);
            }
        }
        notifyAndSet(srList);
    }
}
