package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.MediaPlayback;
import org.mult.daap.PlaylistBrowser;
import org.mult.daap.client.ISong;
import org.mult.daap.client.Playlist;

import java.util.ArrayList;
import java.util.Observable;

public class GetSongsForPlaylist extends Observable implements Runnable {
    private int lastMessage;

    public GetSongsForPlaylist(Playlist playList) {
        this.lastMessage = PlaylistBrowser.INITIALIZED;
    }

    public int getLastMessage() {
        return lastMessage;
    }

    private void notifyAndSet(int message) {
        lastMessage = message;
        setChanged();
        notifyObservers(message);
    }

    public void run() {
        MediaPlayback.clearState();
        Contents.clearLists();
        try {
            if (Contents.daapHost.getSongs().size() == 0) {
                notifyAndSet(PlaylistBrowser.EMPTY);
                return;
            }
            for (ISong song : Contents.daapHost.getSongs()) {
                if (Contents.ArtistElements.containsKey(song.getArtist())) {
                    Contents.ArtistElements.get(song.getArtist()).add(song.getId());
                }
                else {
                    ArrayList<Integer> t = new ArrayList<>();
                    t.add(song.getId());
                    Contents.ArtistElements.put(song.getArtist(), t);
                }
                if (Contents.AlbumElements.containsKey(song.getAlbum())) {
                    Contents.AlbumElements.get(song.getAlbum()).add(song.getId());
                }
                else {
                    ArrayList<Integer> t = new ArrayList<>();
                    t.add(song.getId());
                    Contents.AlbumElements.put(song.getAlbum(), t);
                }
                Contents.songListAdd(song);
            }
            Contents.sortLists();
            notifyAndSet(PlaylistBrowser.FINISHED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
