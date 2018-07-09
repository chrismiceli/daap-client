package org.mult.daap.comparator;


import org.mult.daap.client.ISong;

import java.util.Comparator;

public class SongNameComparator implements Comparator<ISong> {
   public int compare(ISong s1, ISong s2) {
      return s1.toString().compareToIgnoreCase(s2.toString());
   }
}
