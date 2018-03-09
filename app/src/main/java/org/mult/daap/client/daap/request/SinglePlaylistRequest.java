package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapPlaylist;

import java.io.IOException;
import java.util.ArrayList;

public class SinglePlaylistRequest extends Request {
    private final ArrayList<Song> mSongList = new ArrayList<>();
    private final DaapPlaylist playlist;
    private ArrayList<FieldPair> mlclList = new ArrayList<>();
    private ArrayList<FieldPair> mlitList = new ArrayList<>();

    public SinglePlaylistRequest(DaapPlaylist daapPlaylist)
            throws BadResponseCodeException, PasswordFailedException,
            IOException {
        super(daapPlaylist.getHost());
        playlist = daapPlaylist;
        query("SinglePlaylistRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        return "databases/" + host.getDatabaseID() +
                "/containers/" + playlist.getId() +
                "/items?type=music&meta=dmap.itemid&session-id=" +
                host.getSessionID() +
                "&revision-number=" +
                host.getRevisionNumber();
    }

    private void process() {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 8;
        processSinglePlaylistRequest();
        parseMLCL();
    }

    private void processSinglePlaylistRequest() {
        String name;
        int size;
        while (offset < data.length) {
            name = readString(data, offset, 4);
            offset += 4;
            size = readInt(data, offset);
            offset += 4;
            if (size > 10000000) {
                Log.d("Request", "This host probably uses gzip encoding");
            }
            if (name.equals("mlcl")) {
                mlclList.add(new FieldPair(size, offset));
            }
            offset += size;
        }
    }

    /* Creates a list of byte arrays for use in mLIT */
    private void parseMLCL() {
        for (FieldPair mlcl : mlclList) {
            processContainerList(mlcl.position, mlcl.size);
        }
        parseMLIT();
    }

    private void parseMLIT() {
        for (FieldPair mlit : mlitList) {
            processmlitItem(mlit.position, mlit.size);
        }
    }

    private void processmlitItem(int position, int argSize) {
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

    private void processContainerList(int position, int argSize) {
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