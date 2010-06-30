/*
 * Created on Jan 15, 2005
 */
package org.mult.daap.client;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Greg
 *         A playlist where all of the contained songs are just used as
 *         "pointers", and the correct Song is searched for using a certain
 *         precedence of factors.
 */
public class MixedPlaylist extends Playlist {

   public MixedPlaylist() {
      super();
   }

   @SuppressWarnings("unchecked")
   protected ArrayList songs;

   public void initialize() {

   }

   @SuppressWarnings("unchecked")
   public void addSongs(Collection s) {
      songs = (ArrayList<Song>) s;
      // songs.addAll(s);
   }

   @SuppressWarnings("unchecked")
   public Collection getSongs() {
      // TODO Auto-generated method stub
      return null;
   }

}
