package org.mult.daap.client.daap.request;

import android.util.Pair;

import org.mult.daap.client.Host;
import org.mult.daap.client.Song;
import org.mult.daap.client.daap.Hasher;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class SongRequest extends Request {
    private BufferedInputStream bufferedInputStream;
    private final Song song;

    public SongRequest(Host daapHost, Song song) {
        super(daapHost);
        this.song = song;
    }

    public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException {
        host.updateNextRequestNumber();
        query();
    }

    @Override
    protected ArrayList<Pair<String, String>> getRequestProperties() {
        ArrayList<Pair<String, String>> requestProperties = new ArrayList<>();
        requestProperties.add(new Pair<>("Host", "" + host.getAddress()));
        requestProperties.add(new Pair<>("Accept", "*/*"));
        requestProperties.add(new Pair<>("Cache-Control", "no-cache"));
        requestProperties.addAll(super.getRequestProperties());
        requestProperties.add(new Pair<>("Client-DAAP-Request-ID", Integer.toString(host.getThisRequestNumber())));
        requestProperties.add(new Pair<>("Connection", "close"));
        return requestProperties;
    }

    @Override
    protected String getRequestString() {
        return "databases/" + host.getDatabaseID() +
                "/items/" + song.id + "." + song.format +
                "?session-id=" + host.getSessionID();
    }

    public URL getSongURL() throws MalformedURLException {
        return new URL("http://" + host.getAddress() + ":" + host.getPort()
                + "/" + getRequestString());
    }

    @Override
    protected byte[] readResponse() throws IOException {
        bufferedInputStream = new BufferedInputStream(httpc.getInputStream(), 8192);
        return null;
    }

    protected String getHashCode(Request r) {
        return Hasher.GenerateHash("/" + r.getRequestString());
    }

    public InputStream getStream() {
        return bufferedInputStream;
    }
}