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
    protected ArrayList playlists = new ArrayList();

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
                    + " not found in host");
        } else {
            return songs.get(index);
        }
    }

    @SuppressWarnings("rawtypes")
    public abstract Collection getPlaylists();

    public abstract InputStream getSongStream(Song s) throws Exception;
}
