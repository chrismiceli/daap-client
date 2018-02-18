/*
 * Created on May 7, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
Copyright 2003 Joseph Barnett

This File is part of "one 2 oh my god"

"one 2 oh my god" is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
Free Software Foundation; either version 2 of the License, or
your option) any later version.

"one 2 oh my god" is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with "one 2 oh my god"; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */
package org.mult.daap.client.daap;

import org.mult.daap.client.Song;

/** @author jbarnett
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments */
public class DaapSong extends Song {
	public String persistent_id;

	public DaapSong() {
		super();
	}

	public DaapSong(DaapHost h) {
		super();
		host = h;
	}

	/*
	 * public boolean equals(Object o) { if (((DaapSong)o).id == id) return
	 * true; return false; }
	 */
	public void setHost(DaapHost h) {
		host = h;
	}

	public String getPersistentId() {
		return persistent_id;
	}
}
