package org.mult.daap.comparator;

import org.mult.daap.db.entity.SongEntity;

import java.util.Comparator;

public class SongTrackComparator implements Comparator<SongEntity> {
    public int compare(SongEntity s1, SongEntity s2) {
        return s1.track - s2.track;
    }
}