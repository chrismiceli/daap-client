package org.mult.daap.client.daap.request;

import java.io.IOException;
import java.util.ArrayList;

import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.DaapPlaylist;

import android.util.Log;

public class PlaylistsRequest extends Request {
    private ArrayList<DaapPlaylist> mPlaylist;
    private ArrayList<FieldPair> mlclList;
    private ArrayList<FieldPair> mlitList;

    public PlaylistsRequest(DaapHost h) throws BadResponseCodeException, PasswordFailedException, IOException {
        super(h);
        mlclList = new ArrayList<FieldPair>();
        mlitList = new ArrayList<FieldPair>();
        mPlaylist = new ArrayList<DaapPlaylist>();
        query("PlaylistRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        String ret = "databases/";
        ret += host.getDatabaseID() + "/";
        ret += "containers?";
        ret += "session-id=" + host.getSessionID();
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
        processSingleDatabaseRequest();
        parseMLCL();
    }

    public void processSingleDatabaseRequest() {
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
        String name = "";
        int size;
        int startPos = position;
        DaapPlaylist p = new DaapPlaylist(host);
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            if (name.equals("minm")) {
                p.setName(readString(data, position, size));
            }

            position += size;
        }
        if (!p.getName().equals(host.getName())) {
            mPlaylist.add(p);
        }
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

    public ArrayList<DaapPlaylist> getPlaylists() {
        return mPlaylist;
    }
}