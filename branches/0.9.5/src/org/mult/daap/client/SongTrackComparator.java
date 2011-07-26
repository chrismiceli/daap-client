package org.mult.daap.client;

import java.util.Comparator;

public class SongTrackComparator implements Comparator<Song> {
	public int compare(Song s1, Song s2) {
		return (s1.track - s2.track);
	}
}