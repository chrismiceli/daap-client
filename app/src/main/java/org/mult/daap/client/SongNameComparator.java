package org.mult.daap.client;

import java.util.Comparator;

public class SongNameComparator implements Comparator<Song> {
   public int compare(Song s1, Song s2) {
      return s1.toString().compareToIgnoreCase(s2.toString());
   }
}
