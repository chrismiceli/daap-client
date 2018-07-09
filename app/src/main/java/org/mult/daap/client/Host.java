/*
 * Host.java
 *
 * Created on August 9, 2004, 8:35 PM
 */
package org.mult.daap.client;

import android.util.Base64;
import android.util.Log;

import org.mult.daap.background.GetSongURLAsyncTask;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;
import org.mult.daap.client.daap.request.DatabasesRequest;
import org.mult.daap.client.daap.request.LoginRequest;
import org.mult.daap.client.daap.request.PlaylistsRequest;
import org.mult.daap.client.daap.request.ServerInfoRequest;
import org.mult.daap.client.daap.request.SingleDatabaseRequest;
import org.mult.daap.client.daap.request.SongRequest;
import org.mult.daap.client.daap.request.UpdateRequest;
import org.mult.daap.comparator.SongIDComparator;

import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Host {
    private int revisionNum;
    private int databaseId;
    private int sessionId;
    private int requestNum;
    private final String address;
    private final String password;
    private int hostProgram;
    private ArrayList<ISong> songs = new ArrayList<>();
    private ArrayList<Playlist> playlists = new ArrayList<>();
    private ISongFactory songFactory;
    private static final int UNKNOWN_SERVER = 0;
    private static final int ITUNES = 1;
    private static final int GIT_SERVER = 2;
    private static final int MT_DAAPD = 3;

    public Host(String address, String password, ISongFactory songFactory) {
        this.address = address;
        this.password = Base64.encodeToString(("Android_DAAP:" + password).getBytes(), Base64.DEFAULT);
        this.songFactory = songFactory;
    }

    public void connect() throws Exception {
        login();
        grabSongs();
    }

    private void login() throws Exception {
        try {
            revisionNum = 1;
            sessionId = 0;
            ServerInfoRequest s = new ServerInfoRequest(this);
            s.Execute();
            if (s.getServerProgram() != null) {
                hostProgram = parseServerTypeString(s.getServerProgram());
            }
            LoginRequest loginRequest = new LoginRequest(this);
            loginRequest.Execute();
            sessionId = loginRequest.getSessionId();
            UpdateRequest updateRequest = new UpdateRequest(this);
            updateRequest.Execute();
            revisionNum = updateRequest.getRevNum();
        } catch (PasswordFailedException e) {
            e.printStackTrace();
            Log.d("DaapHost", "Password failed");
            throw e;
        } catch (BadResponseCodeException e) {
            if (e.getResponseCode() == 503) {
                Log.d("DaapHost", "tooManyUsers");
                throw e;
            } else {
                e.printStackTrace();
                throw e;
            }
        } catch (ConnectException jce) {
            jce.printStackTrace();
            Log.d("DaapHost", "Net connection exception");
            throw jce;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("DaapHost", "General exception!");
            throw e;
        }
    }

    private void grabSongs() throws Exception {
        try {
            DatabasesRequest databasesRequest = new DatabasesRequest(this);
            databasesRequest.Execute();
            databaseId = databasesRequest.getDatabaseId();
            SingleDatabaseRequest singleDatabaseRequest = new SingleDatabaseRequest(this);
            singleDatabaseRequest.Execute();
            songs = singleDatabaseRequest.getSongs();
            Log.d("DaapHost", "# of songs = " + songs.size());
            Comparator<ISong> sic = new SongIDComparator();
            Collections.sort(songs, sic); // for efficiency in getSongById in
            // Host
            PlaylistsRequest playlistsRequest = new PlaylistsRequest(this);
            playlistsRequest.Execute();
            playlists = playlistsRequest.getPlaylists();
            Log.d("DaapHost", "playlist count = " + playlists.size());
        } catch (BadResponseCodeException e) {
            Log.d("DaapHost", "Bad response code");
        }
    }

    public ISongFactory getSongFactory() {
        return songFactory;
    }

    public boolean isPasswordProtected() {
        return (password != null);
    }

    public int getRevisionNumber() {
        return revisionNum;
    }

    public int getDatabaseID() {
        return databaseId;
    }

    public int getSessionID() {
        return sessionId;
    }

    public void updateNextRequestNumber() {
        requestNum++;
    }

    public int getThisRequestNumber() {
        return requestNum;
    }

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public ArrayList<Playlist> getPlaylists() {
        return playlists;
    }

    public ArrayList<ISong> getSongs() {
        return songs;
    }

    public void getSongURLAsync(ISong song, ISongUrlConsumer songUrlConsumer) {
        GetSongURLAsyncTask songURLAsyncTask = new GetSongURLAsyncTask(this, song, songUrlConsumer);
        songURLAsyncTask.execute();
    }

    public InputStream getSongStream(ISong s) throws Exception {
        try {
            // re-login if we're logged out, or this daap host is a GIT server:
            if (sessionId == 0 || hostProgram == GIT_SERVER) {
                login();
            }

            SongRequest sr = new SongRequest(this, s);
            return sr.getStream();
        } catch (BadResponseCodeException e) {
            Log.d("DaapHost", "Bad response code");
        }
        return null;
    }

    private static int parseServerTypeString(String s) {
        s = s.toLowerCase().trim();
        if (s.startsWith("itunes")) {
            return ITUNES;
        }
        else if (s.startsWith("daapserver")) {
            return GIT_SERVER;
        }
        else if (s.startsWith("mt-daapd")) {
            return MT_DAAPD;
        }
        else {
            return UNKNOWN_SERVER;
        }
    }
}
