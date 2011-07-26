/*
 * Host.java
 * 
 * Created on August 9, 2004, 8:35 PM
 */

package org.mult.daap.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * @author Greg
 */
public abstract class Host {
   protected ArrayList<StatusListener> status_listeners;
   protected boolean auto_connect;
   protected String name;
   protected int status;
   protected boolean visible;
   @SuppressWarnings("unchecked")
   protected ArrayList playlists = new ArrayList();
   // public GetItTogether git;

   public static final String[] status_strings = { "Unavailable / Error",
         "Not Connected", "Connecting", "Connected", "Connected" };
   public static final int STATUS_NOT_AVAILABLE = 0;
   public static final int STATUS_NOT_CONNECTED = 1;
   public static final int STATUS_CONNECTING = 2;
   public static final int STATUS_CONNECTED = 3;
   public static final int STATUS_PLAYLISTS_LOADED = 4;

   /** Creates a new instance of Host */
   public Host(String nayme) {
      name = nayme;
      status_listeners = new ArrayList<StatusListener>();
      visible = false;
   }

   /**
    * Causes this Host to connect to the song source and load the songs into
    * memory.
    * 
    * @throws Exception
    */
   public void connect() throws Exception {
   }

   public void loadPlaylists() throws Exception {
   }

   /**
    * Causes this Host to disconnect from the song source and remove the songs
    * from memory.
    */
   public void disconnect() {
   }

   public abstract ArrayList<Song> getSongs();

   public Song getSongById(Integer id) {
      // from 2 minutes to 3 seconds :-D
      ArrayList<Song> s = getSongs();
      int first = 0;
      int upto = s.size();

      while (first < upto) {
         int mid = (first + upto) / 2; // Compute mid point.
         if (s.get(mid).compareTo(id) < 0) {
            first = mid + 1; // Repeat search in top half.
         } else if (s.get(mid).compareTo(id) > 0) {
            upto = mid; // repeat search in bottom half.
         } else {
            return s.get(mid); // Found it. return position
         }
      }
      throw new IllegalStateException("Song ID: " + id + " not found in host:"
            + name);
   }

   @SuppressWarnings("unchecked")
   public abstract Collection getPlaylists();

   public abstract InputStream getSongStream(Song s) throws Exception;

   public String getName() {
      return name;
   }

   public int getStatus() {
      return status;
   }

   public abstract String getTypeString();

   public boolean isVisible() {
      return visible;
   }

   public void setVisible(boolean b) {
      visible = b;
   }

   public boolean equals(Object o) {
      return name.equals(((Host) o).getName());
   }

   public String getToolTipText() {
      return name;
   }

   public String toString() {
      return name;
   }

   public void addStatusListener(StatusListener sl) {
      status_listeners.add(sl);
   }

   public boolean removeStatusListener(StatusListener sl) {
      return status_listeners.remove(sl);
   }

   public boolean isAutoConnect() {
      return auto_connect;
   }

   public void setAutoConnect(boolean aut) {
      auto_connect = aut;
   }

}
