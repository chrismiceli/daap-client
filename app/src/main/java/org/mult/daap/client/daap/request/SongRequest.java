package org.mult.daap.client.daap.request;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.Hasher;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class SongRequest extends Request {
    private BufferedInputStream b;
    private final Song song;

    public SongRequest(DaapHost daapHost, Song song)
            throws PasswordFailedException, BadResponseCodeException,
            IOException {
        super(daapHost);
        host.getNextRequestNumber();
        this.song = song;
        query("SongRequest");
        readResponse();
    }

    protected void addRequestProperties() {
        httpc.addRequestProperty("Host", "" + host.getAddress());
        httpc.addRequestProperty("Accept", "*/*");
        httpc.addRequestProperty("Cache-Control", "no-cache");
        super.addRequestProperties();
        httpc.addRequestProperty("Client-DAAP-Request-ID",
                "" + host.getThisRequestNumber());
        httpc.addRequestProperty("Connection", "close");
    }

    protected String getRequestString() {
        return "databases/" + host.getDatabaseID() +
                "/items/" + song.id + "." + song.format +
                "?session-id=" + host.getSessionID();
    }

    public URL getSongURL() throws MalformedURLException {
        return new URL("http://" + host.getAddress() + ":" + host.getPort()
                + "/" + getRequestString());
    }

    protected void readResponse() throws IOException {
        b = new BufferedInputStream(httpc.getInputStream(), 8192);
    }

    protected String getHashCode(Request r) {
        return Hasher.GenerateHash("/" + r.getRequestString());
    }

    public InputStream getStream() {
        return b;
    }
}