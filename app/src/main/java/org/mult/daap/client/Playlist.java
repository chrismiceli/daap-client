/*
 * Created on Aug 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client;

/**
 * @author Greg, for GIT
 */
public abstract class Playlist {
    public String name;
    public boolean all_songs;

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
