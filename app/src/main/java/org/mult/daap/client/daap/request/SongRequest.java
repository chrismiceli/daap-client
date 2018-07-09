package org.mult.daap.client.daap.request;

import android.util.Pair;

import org.mult.daap.client.Host;
import org.mult.daap.client.ISong;
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
    private final ISong song;

    public SongRequest(Host daapHost, ISong song) {
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
                "/items/" + song.getId() + "." + song.getFormat() +
                "?session-id=" + host.getSessionID();
    }

    public URL getSongURL() throws MalformedURLException {
        return new URL("http://" + host.getAddress() + "/" + getRequestString());
    }

    @Override
    protected byte[] readResponse() throws IOException {
        bufferedInputStream = new BufferedInputStream(httpc.getInputStream(), 8192);
        return null;
    }

    public InputStream getStream() {
        return bufferedInputStream;
    }
}