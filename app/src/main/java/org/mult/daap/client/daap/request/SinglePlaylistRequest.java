package org.mult.daap.client.daap.request;

import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

public class SinglePlaylistRequest extends Request {
    private final ArrayList<Integer> songIds = new ArrayList<>();
    private final int playlistId;

    public SinglePlaylistRequest(Host host, int playlistId) {
        super(host);
        this.playlistId = playlistId;

    }

    public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException {
        query();
        byte[] data = readResponse();
        process(data);
    }

    protected String getRequestString() {
        return  "databases/" +
        host.getDatabaseID() +
        "/containers/" + this.playlistId +
        "/items?type=music&meta=dmap.itemid&session-id=" +
        host.getSessionID() +
        "&revision-number=" +
        host.getRevisionNumber();
    }

    private void process(byte[] data) {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 4;
        offset += 4;
        ArrayList<FieldPair> mlclList = processSinglePlaylistRequest(data);
        parseMLCL(data, mlclList);
    }

    private ArrayList<FieldPair> processSinglePlaylistRequest(byte[] data) {
        String name;
        int size;
        ArrayList<FieldPair> mlclList = new ArrayList<>();
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

        return mlclList;
    }

    /* Creates a list of byte arrays for use in mLIT */
    private void parseMLCL(byte[] data, ArrayList<FieldPair> mlclList) {
        ArrayList<FieldPair> mlitList = new ArrayList<>();
        for(FieldPair mlcl : mlclList) {
            mlitList.addAll(processContainerList(data, mlcl.position, mlcl.size));
        }
        parseMLIT(data, mlitList);
    }

    private void parseMLIT(byte[] data, ArrayList<FieldPair> mlitList) {
        for (FieldPair mlit : mlitList) {
            processmlitItem(data, mlit.position, mlit.size);
        }
    }

    private void processmlitItem(byte[] data, int position, int argSize) {
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

        this.songIds.add(song_id);
    }

    /* get all mlit in mlclList */
    private ArrayList<FieldPair> processContainerList(byte[] data, int position, int argSize) {
        String name;
        int size;
        int startPos = position;
        ArrayList<FieldPair> mlitList = new ArrayList<>();
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

        return mlitList;
    }

    public ArrayList<Integer> getSongIds() {
        return this.songIds;
    }
}