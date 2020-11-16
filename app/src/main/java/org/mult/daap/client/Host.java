/*
 * Host.java
 *
 * Created on August 9, 2004, 8:35 PM
 */

package org.mult.daap.client;

import org.mult.daap.client.daap.DaapPlaylist;

import java.util.ArrayList;

/**
 * @author Greg
 */
public abstract class Host {
    protected final String name;
    protected ArrayList<DaapPlaylist> playlists = new ArrayList<>();

    /**
     * Creates a new instance of Host
     */
    public Host(String name) {
        this.name = name;
    }

    public abstract ArrayList<Song> getSongs();

    public Song getSongById(Integer id) {
        // from 2 minutes to 3 seconds :-D
        ArrayList<Song> s = getSongs();
        int first = 0;
        int upto = s.size();

        while (first < upto) {
            int mid = (first + upto) / 2; // Compute mid point.
            if (s.get(mid).compareTo(id) < 0) {
                first = mid + 1; // Repeat search in top half.
            } else if (s.get(mid).compareTo(id) > 0) {
                upto = mid; // repeat search in bottom half.
            } else {
                return s.get(mid); // Found it. return position
            }
        }
        throw new IllegalStateException("Song ID: " + id
                + " not found in host:" + name);
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (o instanceof Host) {
            return name.equals(((Host) o).getName());
        }

        return false;
    }

    public String toString() {
        return name;
    }
}
