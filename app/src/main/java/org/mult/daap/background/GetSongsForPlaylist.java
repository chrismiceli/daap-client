package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.MediaPlayback;
import org.mult.daap.PlaylistBrowser;
import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapPlaylist;

import java.util.ArrayList;
import java.util.Observable;

public class GetSongsForPlaylist extends Observable implements Runnable {
    private DaapPlaylist playList;
    private int lastMessage;

    public GetSongsForPlaylist(DaapPlaylist playList) {
        this.playList = playList;
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
            if (playList.all_songs == true) {
                if (Contents.daapHost.getSongs().size() == 0) {
                    notifyAndSet(PlaylistBrowser.EMPTY);
                    return;
                }
                for (Song song : Contents.daapHost.getSongs()) {
                    if (Contents.ArtistElements.containsKey(song.artist)) {
                        Contents.ArtistElements.get(song.artist).add(song.id);
                    } else {
                        ArrayList<Integer> t = new ArrayList<Integer>();
                        t.add(song.id);
                        Contents.ArtistElements.put(song.artist, t);
                    }
                    if (Contents.AlbumElements.containsKey(song.album)) {
                        Contents.AlbumElements.get(song.album).add(song.id);
                    } else {
                        ArrayList<Integer> t = new ArrayList<Integer>();
                        t.add(song.id);
                        Contents.AlbumElements.put(song.album, t);
                    }
                    Contents.songListAdd(song);
                }
                Contents.sortLists();
                notifyAndSet(PlaylistBrowser.FINISHED);
            } else {
                playList.initialize();
                ArrayList<Song> el = (ArrayList<Song>) playList.getSongs();
                if (el.size() == 0) {
                    notifyAndSet(PlaylistBrowser.EMPTY);
                    return;
                }
                for (Song song : el) {
                    if (Contents.ArtistElements.containsKey(song.artist)) {
                        Contents.ArtistElements.get(song.artist).add(song.id);
                    } else {
                        ArrayList<Integer> t = new ArrayList<Integer>();
                        t.add(song.id);
                        Contents.ArtistElements.put(song.artist, t);
                    }
                    if (Contents.AlbumElements.containsKey(song.album)) {
                        Contents.AlbumElements.get(song.album).add(song.id);
                    } else {
                        ArrayList<Integer> t = new ArrayList<Integer>();
                        t.add(song.id);
                        Contents.AlbumElements.put(song.album, t);
                    }
                    Contents.songListAdd(song);
                }
                Contents.sortLists();
                notifyAndSet(PlaylistBrowser.FINISHED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
