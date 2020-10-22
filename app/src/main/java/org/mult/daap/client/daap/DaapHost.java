/*
 * DaapHost.java
 *
 * Created on August 9, 2004, 8:35 PM
 */
package org.mult.daap.client.daap;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.Song;
import org.mult.daap.client.SongIDComparator;
import org.mult.daap.client.daap.request.BadResponseCodeException;
import org.mult.daap.client.daap.request.DatabasesRequest;
import org.mult.daap.client.daap.request.LoginRequest;
import org.mult.daap.client.daap.request.LogoutRequest;
import org.mult.daap.client.daap.request.PasswordFailedException;
import org.mult.daap.client.daap.request.PlaylistsRequest;
import org.mult.daap.client.daap.request.ServerInfoRequest;
import org.mult.daap.client.daap.request.SingleDatabaseRequest;
import org.mult.daap.client.daap.request.SongRequest;
import org.mult.daap.client.daap.request.UpdateRequest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

// import ca.odell.glazedlists.BasicEventList;
// import ca.odell.glazedlists.EventList;

/**
 * @author Greg
 */
public class DaapHost extends Host {
    public final String address;
    protected final int port;
    protected int revision_num;
    protected int database_id;
    protected int session_id;
    protected int request_num;
    protected String password;
    protected int host_prog;
    // a program-specific integer, so we can hack together compatibility.
    protected ArrayList<Song> songs = new ArrayList<>();
    public static final int UNKNOWN_SERVER = 0;
    public static final int ITUNES = 1;
    public static final int GIT_SERVER = 2;
    public static final int MT_DAAPD = 3;

    public DaapHost(String name, String pwd, InetAddress addy, int porty) {
        super(name);
        password = Base64.encodeToString(("Android_DAAP:" + pwd).getBytes(), Base64.DEFAULT);
        address = addy.getHostAddress();
        port = porty;
    }

    public void connect() throws Exception {
        login();
        grabSongs();
    }

    public void login() throws Exception {
        try {
            revision_num = 1;
            session_id = 0;
            ServerInfoRequest s = new ServerInfoRequest(this);
            Log.d("DAAPHost",
                    "ServerInfo:  " + s.getServerVersion() + " "
                            + s.getServerProgram() + " " + s.getHost());
            if (s.getServerProgram() != null)
                host_prog = parseServerTypeString(s.getServerProgram());
            LoginRequest l = new LoginRequest(this);
            session_id = l.getSessionId();
            UpdateRequest u = new UpdateRequest(this);
            revision_num = u.getRevNum();
        } catch (PasswordFailedException e) {
            e.printStackTrace();
            Log.d("DaapHost", "Password failed");
            password = null;
            throw e;
        } catch (BadResponseCodeException e) {
            if (e.response_code == 503) {
                Log.d("DaapHost", "tooManyUsers");
            } else {
                e.printStackTrace();
            }
            throw e;
        } catch (java.net.ConnectException jce) {
            jce.printStackTrace();
            Log.d("DaapHost", "Net connection exception");
            nullify();
            throw jce;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("DaapHost", "General exception!");
            throw e;
        }
    }

    public void logout() throws PasswordFailedException, IOException,
            BadResponseCodeException {
        // don't logout when connected to a de.kapsi server:
        try {
            new LogoutRequest(this);
        } catch (BadResponseCodeException e) {
            if (e.response_code == 204) {
                session_id = 0;
                revision_num = 1;
            } else {
                throw e;
            }
        }
    }

    public void grabSongs() throws Exception {
        try {
            DatabasesRequest d = new DatabasesRequest(this);
            database_id = d.getDbs().get(0).id;
            SingleDatabaseRequest g = new SingleDatabaseRequest(this);
            songs = g.getSongs();
            Log.d("DaapHost", "# of songs = " + songs.size());
            Comparator<Song> sic = new SongIDComparator();
            Collections.sort(songs, sic); // for efficiency in getSongById in
            // Host
            PlaylistsRequest p = new PlaylistsRequest(this);
            playlists = p.getPlaylists();
            Log.d("DaapHost", "playlist count = " + playlists.size());
        } catch (BadResponseCodeException e) {
            if (e.response_code == 500) {
                Log.d("DaapHost", "500 Response code");
                logout();
                login();
                grabSongs(); // try again.
            }
        }
    }

    private void nullify() {
        songs.clear();
        playlists = null;
        revision_num = 0;
        database_id = 0;
        session_id = 0;
        request_num = 0;
    }

    public boolean isPasswordProtected() {
        return (password != null);
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getRevisionNumber() {
        return revision_num;
    }

    public int getDatabaseID() {
        return database_id;
    }

    public int getSessionID() {
        return session_id;
    }

    public void getNextRequestNumber() {
        request_num++;
    }

    public int getThisRequestNumber() {
        return request_num;
    }

    public String getPassword() {
        return password;
    }

    public Collection<DaapPlaylist> getPlaylists() {
        if (playlists == null)
            playlists = new ArrayList<>();
        return playlists;
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public InputStream getSongStream(Song s) throws Exception {
        return getSongStream(s, 0);
    }

    public void getSongURLAsync(Song song, ISongUrlConsumer songUrlConsumer) {
        GetSongURLAsyncTask songURLAsyncTask = new GetSongURLAsyncTask(this, song, songUrlConsumer);
        songURLAsyncTask.execute();
    }

    public InputStream getSongStream(Song s, long bytes) throws Exception {
        try {
            // re-login if we're logged out, or this daaphost is a GIT server:
            if (session_id == 0 || host_prog == GIT_SERVER) {
                login();
            }
            SongRequest sr = new SongRequest(this, s, bytes);
            return sr.getStream();
        } catch (BadResponseCodeException ignored) {
        }
        return null;
    }

    public static int parseServerTypeString(String s) {
        s = s.toLowerCase().trim();
        if (s.startsWith("itunes"))
            return ITUNES;
        else if (s.startsWith("daapserver"))
            return GIT_SERVER;
        else if (s.startsWith("mt-daapd"))
            return MT_DAAPD;
        else
            return UNKNOWN_SERVER;
    }

    static class GetSongURLAsyncTask extends AsyncTask<Void, Void, String> {
        final DaapHost host;
        final Song song;
        final WeakReference<ISongUrlConsumer> songUrlConsumerWeakReference;

        GetSongURLAsyncTask(DaapHost host, Song song, ISongUrlConsumer songUrlConsumer) {
            this.host = host;
            this.song = song;
            this.songUrlConsumerWeakReference = new WeakReference<>(songUrlConsumer);
        }

        @Override
        protected String doInBackground(Void... voids) {
            SongRequest sr = null;
            try {
                sr = new SongRequest(this.host, this.song, 0);
            } catch (PasswordFailedException | BadResponseCodeException | IOException ignored) {
            }
            String result = null;

            if (sr != null) {
                try {
                    sr.getSongURL();
                } catch (IOException ignored) {
                }

                try {
                    result = sr.getSongURL().toString();
                } catch (MalformedURLException ignored) {
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String songUrl) {
            ISongUrlConsumer songUrlConsumer = this.songUrlConsumerWeakReference.get();
            if (null != songUrlConsumer) {
                songUrlConsumer.onSongUrlRetrieved(songUrl);
            }
        }
    }
}