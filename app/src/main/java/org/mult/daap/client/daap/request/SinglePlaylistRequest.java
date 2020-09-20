package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapPlaylist;

import java.io.IOException;
import java.util.ArrayList;

/*
 * @author Greg
 */
public class SinglePlaylistRequest extends Request {
    private static class FieldPair {
        public FieldPair(int s, int p) {
            size = s;
            position = p;
        }

        public final int position;
        public final int size;
    }

    private final ArrayList<Song> mSongList;
    protected final DaapPlaylist playlist;
    private ArrayList<FieldPair> mlclList;
    private ArrayList<FieldPair> mlitList;

    public SinglePlaylistRequest(DaapPlaylist p)
            throws BadResponseCodeException, PasswordFailedException,
            IOException {
        super(p.getHost());
        mlclList = new ArrayList<>();
        mlitList = new ArrayList<>();
        mSongList = new ArrayList<>();
        playlist = p;
        query("SinglePlaylistRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        String ret = "databases/" + host.getDatabaseID() + "/";
        ret += "containers/" + playlist.getId() + "/";
        ret += "items?type=music&";
        ret += "meta=dmap.itemid";
        //			ret += "meta=dmap.itemid,dmap.containeritemid";
        ret += "&session-id=" + host.getSessionID();
        ret += "&revision-number=" + host.getRevisionNumber();
        return ret;
    }

    protected void addRequestProperties() {
        super.addRequestProperties();
    }

    protected void process() {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 4;
        offset += 4;
        processSinglePlaylistRequest();
        parseMLCL();
    }

    public void processSinglePlaylistRequest() {
        String name;
        int size;
        while (offset < data.length) {
            name = readString(data, offset, 4);
            offset += 4;
            size = readInt(data, offset);
            offset += 4;
            if (size > 10000000)
                Log.d("Request", "This host probably uses gzip encoding");
            if (name.equals("mlcl")) {
                mlclList.add(new FieldPair(size, offset));
            }
            offset += size;
        }
    }

    /* Creates a list of byte arrays for use in mLIT */
    protected void parseMLCL() {
        for (int i = 0; i < mlclList.size(); i++) {
            processContainerList(mlclList.get(i).position, mlclList.get(i).size);
        }
        parseMLIT();
    }

    protected void parseMLIT() {
        for (int i = 0; i < mlitList.size(); i++) {
            processmlitItem(mlitList.get(i).position, mlitList.get(i).size);
        }
        mlitList = null;
        mlclList = null;
    }

    public void processmlitItem(int position, int argSize) {
        String name;
        int size;
        int startPos = position;
        int song_id = 0;
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            if (name.equals("miid")) {
                song_id = Request.readInt(data, position);
            }
            position += size;
        }
        mSongList.add(host.getSongById(song_id));
    }

    /* get all mlit in mlclList */
    public void processContainerList(int position, int argSize) {
        String name;
        int size;
        int startPos = position;
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            if (name.equals("mlit")) {
                mlitList.add(new FieldPair(size, position));
            }
            position += size;
        }
    }

    public ArrayList<Song> getSongs() {
        return mSongList;
    }
}