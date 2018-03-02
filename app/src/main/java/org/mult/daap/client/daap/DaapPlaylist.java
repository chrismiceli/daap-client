/*
 * Created on Aug 18, 2004
 * 
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client.daap;

import java.util.ArrayList;
import java.util.Collection;

import org.mult.daap.client.Playlist;
import org.mult.daap.client.Song;
import org.mult.daap.client.daap.request.BadResponseCodeException;
import org.mult.daap.client.daap.request.SinglePlaylistRequest;

import android.util.Log;

/** @author Greg
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates */
public class DaapPlaylist extends Playlist {
	private int id;
	private ArrayList<Song> songs;
	protected final DaapHost host;

	public DaapPlaylist(DaapHost daapHost) {
		host = daapHost;
	}

	public DaapPlaylist(DaapHost daapHost, String name, boolean allSongs) {
		host = daapHost;
		this.name = name;
		setAllSongs(allSongs);
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
			Log.d("DaapPlaylist", "Error on playlist");
			e.printStackTrace();
		}
	}

	public DaapHost getHost() {
		return host;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
	    this.id = id;
    }

	public Collection<Song> getSongs() {
		if (songs == null) {
            return new ArrayList<>();
        }

		return songs;
	}
}
