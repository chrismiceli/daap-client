package org.mult.daap.client;

import java.util.Comparator;

public class SongIDComparator implements Comparator<Song> {
   public int compare(Song s1, Song s2) {
      return (s1.id - s2.id);
   }
}
