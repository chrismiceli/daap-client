/*
 * Host.java
 *
 * Created on August 9, 2004, 8:35 PM
 */

package org.mult.daap.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Greg
 */
public abstract class Host {
    protected final ArrayList<StatusListener> status_listeners;
    protected boolean auto_connect;
    protected final String name;
    protected int status;
    protected boolean visible;
    @SuppressWarnings("rawtypes")
    protected ArrayList playlists = new ArrayList();

    /**
     * Creates a new instance of Host
     */
    public Host(String name) {
        this.name = name;
        status_listeners = new ArrayList<>();
        visible = false;
    }

    public void connect() throws Exception {
    }

    public void loadPlaylists() throws Exception {
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

    @SuppressWarnings("rawtypes")
    public abstract Collection getPlaylists();

    public abstract InputStream getSongStream(Song s) throws Exception;

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean b) {
        visible = b;
    }

    public boolean equals(Object o) {
        if (o instanceof Host) {
            return name.equals(((Host) o).getName());
        }

        return false;
    }

    public String getToolTipText() {
        return name;
    }

    public String toString() {
        return name;
    }

    public void addStatusListener(StatusListener sl) {
        status_listeners.add(sl);
    }

    public boolean removeStatusListener(StatusListener sl) {
        return status_listeners.remove(sl);
    }

    public boolean isAutoConnect() {
        return auto_connect;
    }

    public void setAutoConnect(boolean aut) {
        auto_connect = aut;
    }
}
