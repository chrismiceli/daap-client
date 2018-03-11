/*
 * DaapHost.java
 * 
 * Created on August 9, 2004, 8:35 PM
 */
package org.mult.daap.client.daap;

import android.util.Base64;
import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.Song;
import org.mult.daap.client.SongIDComparator;
import org.mult.daap.client.daap.request.BadResponseCodeException;
import org.mult.daap.client.daap.request.DatabasesRequest;
import org.mult.daap.client.daap.request.HangingUpdateRequest;
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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/** @author Greg */
public class DaapHost extends Host {
    private final String address;
    private final int port;
    private int revisionNum;
    private int databaseId;
    private int sessionId;
    private int requestNum;
    private String password;
    private HangingUpdateRequest updateRequest;
    private int hostProgram;
    private ArrayList<Song> songs = new ArrayList<>();
    private static final int UNKNOWN_SERVER = 0;
    private static final int ITUNES = 1;
    private static final int GIT_SERVER = 2;
    private static final int MT_DAAPD = 3;

    public DaapHost(String password, InetAddress inetAddress, int port) {
        super();
        this.password = Base64.encodeToString(("Android_DAAP:" + password).getBytes(), Base64.DEFAULT);
        this.address = inetAddress.getHostAddress();
        this.port = port;
    }

    public void connect() throws Exception {
        login();
        grabSongs();
    }

    public void login() throws Exception {
        try {
            revisionNum = 1;
            sessionId = 0;
            ServerInfoRequest s = new ServerInfoRequest(this);
            Log.d("DAAPHost",
                    "ServerInfo:  " + s.getServerProgram() + " " + s.getHost());
            if (s.getServerProgram() != null) {
                hostProgram = parseServerTypeString(s.getServerProgram());
            }
            LoginRequest loginRequest = new LoginRequest(this);
            sessionId = loginRequest.getSessionId();
            UpdateRequest updateRequest = new UpdateRequest(this);
            revisionNum = updateRequest.getRevNum();
        } catch (PasswordFailedException e) {
            e.printStackTrace();
            Log.d("DaapHost", "Password failed");
            password = null;
            throw e;
        } catch (BadResponseCodeException e) {
            if (e.getResponseCode() == 503) {
                Log.d("DaapHost", "tooManyUsers");
                throw e;
            } else {
                e.printStackTrace();
                throw e;
            }
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
            @SuppressWarnings("unused")
            LogoutRequest logoutRequest = new LogoutRequest(this);
            if (updateRequest != null) {
                updateRequest.disconnect();
            }
        } catch (BadResponseCodeException e) {
            if (e.getResponseCode() == 204) {
                sessionId = 0;
                revisionNum = 1;
            } else {
                throw e;
            }
        }
    }

    private void grabSongs() throws Exception {
        try {
            DatabasesRequest databasesRequest = new DatabasesRequest(this);
            databaseId = databasesRequest.getDatabase().id;
            SingleDatabaseRequest singleDatabaseRequest = new SingleDatabaseRequest(this);
            songs = singleDatabaseRequest.getSongs();
            Log.d("DaapHost", "# of songs = " + songs.size());
            Comparator<Song> sic = new SongIDComparator();
            Collections.sort(songs, sic); // for efficiency in getSongById in
                                            // Host
            PlaylistsRequest playlistsRequest = new PlaylistsRequest(this);
            playlists = playlistsRequest.getPlaylists();
            Log.d("DaapHost", "playlist count = " + playlists.size());
            if (hostProgram == DaapHost.GIT_SERVER) {
                updateRequest = new HangingUpdateRequest(this);
            }
        } catch (BadResponseCodeException e) {
            if (e.getResponseCode() == 500) {
                Log.d("DaapHost", "500 Response code");
                logout();
                login();
                grabSongs(); // try again.
            }
        }
    }

    public void loadPlaylists() throws Exception {
        login();
        for (Object playlist : playlists) {
            ((DaapPlaylist) playlist).initialize();
        }
        // logout();
    }

    private void nullify() {
        songs.clear();
        playlists = null;
        revisionNum = 0;
        databaseId = 0;
        sessionId = 0;
        requestNum = 0;
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
        return revisionNum;
    }

    public int getDatabaseID() {
        return databaseId;
    }

    public int getSessionID() {
        return sessionId;
    }

    public int getNextRequestNumber() {
        requestNum++;
        return requestNum;
    }

    public int getThisRequestNumber() {
        return requestNum;
    }

    public String getPassword() {
        return password;
    }

    @SuppressWarnings("rawtypes")
    public Collection getPlaylists() {
        if (playlists == null)
            playlists = new ArrayList();
        return playlists;
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public String getSongURL(Song s) throws PasswordFailedException,
            BadResponseCodeException, IOException {
        SongRequest sr = new SongRequest(this, s);
        return sr.getSongURL().toString();
    }

    public InputStream getSongStream(Song s) throws Exception {
        try {
            // re-login if we're logged out, or this daaphost is a GIT server:
            if (sessionId == 0 || hostProgram == GIT_SERVER) {
                login();
            }
            SongRequest sr = new SongRequest(this, s);
            return sr.getStream();
        } catch (BadResponseCodeException e) {
            if (e.getResponseCode() == 500) {
                // FIXME: This code here can help with failed song requests, but
                // if the iTunes internal server error
                // really is internal, it causes an infinite loop.
                // System.out.println("Error 500: forbidden.. attempting to re-login ONCE");
                // if (login(false))
                // return getSongStream(s, bytes);
            }
        }
        return null;
    }

    private static int parseServerTypeString(String s) {
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
}