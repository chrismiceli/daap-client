package org.mult.daap.client;

import android.support.annotation.NonNull;

public class Song implements Comparable<Object> {
	public String name;
	public int id;
	public int time;
	public String album;
	public String artist;
	public short track;
	public short disc_num;
	public String format;
	public int size;
	public Host host;

	public String toString() {
		return artist + (artist.length() > 0 ? " - " : "") + name;
	}

	public int compareTo(@NonNull Object another) {
		if (another instanceof Song) {
			return (this.id - ((Song) another).id);
		} else if (another instanceof Integer) {
			return (this.id - (Integer) another);
		}

		return 0; // all the same if can't compare
	}
}