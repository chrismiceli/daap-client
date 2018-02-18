/*
 * Created on May 7, 2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * Copyright 2003 Joseph Barnett
 * 
 * This File is part of "one 2 oh my god"
 * 
 * "one 2 oh my god" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * Free Software Foundation; either version 2 of the License, or
 * your option) any later version.
 * 
 * "one 2 oh my god" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with "one 2 oh my god"; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.mult.daap.client;

/** @author jbarnett
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments */
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

	// public int status;
	// public static int STATUS_OK = 0;
	// public static int STATUS_NOT_FOUND = 2;
	// public static int STATUS_ERROR = 3;
	// public boolean compilation;
	// public int bitrate;
	// public String persistent_id;
	// public String genre;
	// public boolean is_available;
	public Song() {
		name = "";
		id = 0;
		album = "";
		artist = "";
		track = -1;
		// genre = "";
		format = "";
		// compilation = false;
		host = null;
		// status = Song.STATUS_OK;
	}

	public String toString() {
		String ret = artist + (artist.length() > 0 ? " - " : "") + name;
		return ret;
	}

	public int compareTo(Object another) {
		if (another instanceof Song) {
			return (this.id - ((Song) another).id);
		} else if (another instanceof Integer) {
			return (this.id - (Integer) another);
		}
		return 0; // all the same if can't compare
	}
}