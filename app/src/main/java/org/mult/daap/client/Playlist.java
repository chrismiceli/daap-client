/*
 * Created on Aug 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client;

import java.util.Collection;

public abstract class Playlist {
    protected String name;
    private boolean allSongs;

    public abstract void initialize() throws Exception;

    public abstract Collection getSongs();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getAllSongs() {
        return allSongs;
    }
    
    public void setAllSongs(boolean allSongs) {
        this.allSongs = allSongs;
    }

    public String toString() {
        return name;
    }
}
