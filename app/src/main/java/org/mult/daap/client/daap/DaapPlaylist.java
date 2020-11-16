/*
 * Created on Aug 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client.daap;

import android.util.Log;

import org.mult.daap.client.Playlist;
import org.mult.daap.client.Song;
import org.mult.daap.client.daap.request.BadResponseCodeException;
import org.mult.daap.client.daap.request.SinglePlaylistRequest;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Greg
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DaapPlaylist extends Playlist {
    public int id;
    protected ArrayList<Song> songs;
    protected final DaapHost host;

    public DaapPlaylist(DaapHost h) {
        host = h;
        this.all_songs = false;
    }

    public DaapPlaylist(DaapHost h, String n, boolean as) {
        host = h;
        name = n;
        this.all_songs = as;
    }

    public void initialize() throws Exception {
        try {
            SinglePlaylistRequest p = new SinglePlaylistRequest(this);
            // should be like singledatabaserequest
            songs = p.getSongs();
        } catch (BadResponseCodeException e) {
            Log.d("DaapPlaylist", "BadResponse " + e.getMessage());
            host.login();
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("DaapPlaylist", "Error code " + e
                    + " on playlist");
        }
    }

    public DaapHost getHost() {
        return host;
    }

    public int getId() {
        return id;
    }

    public Collection<Song> getSongs() {
        if (songs == null)
            return new ArrayList<>();
        return songs;
    }
}
