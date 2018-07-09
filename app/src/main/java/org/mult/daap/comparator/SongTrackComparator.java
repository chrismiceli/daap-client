package org.mult.daap.comparator;

import org.mult.daap.client.ISong;

import java.util.Comparator;

public class SongTrackComparator implements Comparator<ISong> {
    public int compare(ISong s1, ISong s2) {
        return s1.getTrack() - s2.getTrack();
    }
}