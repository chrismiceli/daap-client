/*
 * Created on Aug 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client;

import org.mult.daap.client.daap.DaapHost;

import java.util.ArrayList;
import java.util.Collection;

public class Playlist {
    private int id;
    private final ArrayList<Song> songs = new ArrayList<>();
    private final DaapHost host;
    private String name;

    public Playlist(DaapHost daapHost) {
        host = daapHost;
    }

    public Playlist(DaapHost daapHost, String name) {
        this.host = daapHost;
        this.name = name;
    }

    public DaapHost getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Collection<Song> getSongs() {
        return songs;
    }
}
