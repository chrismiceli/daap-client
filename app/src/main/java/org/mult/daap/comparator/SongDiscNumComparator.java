package org.mult.daap.comparator;

import org.mult.daap.db.entity.SongEntity;

import java.util.Comparator;

public class SongDiscNumComparator implements Comparator<SongEntity> {
    public int compare(SongEntity s1, SongEntity s2) {
        return (s1.getDiscNum() - s2.getDiscNum());
    }
}