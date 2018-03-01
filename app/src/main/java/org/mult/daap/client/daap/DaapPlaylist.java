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
	public int id;
	public String persistent_id;
	public boolean smart_playlist;
	public int song_count = 0;
	protected ArrayList<Song> songs;
	protected DaapHost host;

	public DaapPlaylist(DaapHost h) {
		host = h;
		setStatus(Playlist.STATUS_NOT_INITIALIZED);
		this.all_songs = false;
	}

	public DaapPlaylist(DaapHost h, String n, boolean as) {
		host = h;
		name = n;
		this.all_songs = as;
		setStatus(Playlist.STATUS_NOT_INITIALIZED);
	}

	public void initialize() throws Exception {
		setStatus(Playlist.STATUS_INITIALIZING);
		try {
			SinglePlaylistRequest p = new SinglePlaylistRequest(this);
			// should be like singledatabaserequest
			songs = p.getSongs();
			p = null;
			setStatus(Playlist.STATUS_INITIALIZED);
		} catch (BadResponseCodeException e) {
			Log.d("DaapPlaylist", "BadResponse " + e.getMessage());
			host.login();
			initialize();
		} catch (Exception e) {
			setStatus(Playlist.STATUS_NOT_INITIALIZED);
			e.printStackTrace();
			Log.d("DaapPlaylist", "Error on playlist");
			e.printStackTrace();
		}
	}

	public DaapHost getHost() {
		return host;
	}

	public String getPersistentId() {
		return persistent_id;
	}

	public int getId() {
		return id;
	}

	public Collection<Song> getSongs() {
		if (songs == null)
			return new ArrayList<Song>();
		return songs;
	}
}
