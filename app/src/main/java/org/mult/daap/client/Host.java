/*
 * Host.java
 * 
 * Created on August 9, 2004, 8:35 PM
 */

package org.mult.daap.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * 
 * @author Greg
 */
public abstract class Host {
	private final String name;
	protected ArrayList playlists = new ArrayList();

	/** Creates a new instance of Host */
	public Host(String name) {
		this.name = name;
	}

	/**
	 * Causes this Host to connect to the song source and load the songs into
	 * memory.
	 *
	 * @throws Exception
	 */
	public abstract void connect() throws Exception;

	public abstract void loadPlaylists() throws Exception;

	public abstract ArrayList<Song> getSongs();

	public Song getSongById(Integer id) {
		// from 2 minutes to 3 seconds :-D
		ArrayList<Song> songs = getSongs();
        int index = Collections.binarySearch(songs, id);
        if (index < 0) {
            throw new IllegalStateException("Song ID: " + id
                    + " not found in host:" + name);
        } else {
            return songs.get(index);
        }
	}

	@SuppressWarnings("rawtypes")
	public abstract Collection getPlaylists();

	public abstract InputStream getSongStream(Song s) throws Exception;

	public String getName() {
		return name;
	}

	public boolean equals(Object o) {
		return o.getClass() == Host.class && name.equals(((Host) o).getName());

	}

	public String toString() {
		return name;
	}

}
