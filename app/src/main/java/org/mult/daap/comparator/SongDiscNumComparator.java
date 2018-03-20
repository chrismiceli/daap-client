package org.mult.daap.comparator;

import org.mult.daap.client.Song;

import java.util.Comparator;

public class SongDiscNumComparator implements Comparator<Song> {
    public int compare(Song s1, Song s2) {
        return (s1.disc_num - s2.disc_num);
    }
}