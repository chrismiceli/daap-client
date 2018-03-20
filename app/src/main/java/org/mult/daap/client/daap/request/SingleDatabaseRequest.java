package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;
import java.util.ArrayList;

public class SingleDatabaseRequest extends Request {
    private final ArrayList<Song> mSongList = new ArrayList<>();
    private final ArrayList<FieldPair> mlclList = new ArrayList<>();
    private final ArrayList<FieldPair> mlitList = new ArrayList<>();

    public SingleDatabaseRequest(DaapHost daapHost) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(daapHost);
        query("SingleDatabaseRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        return "databases/" + host.getDatabaseID() + "/items?type=" +
                "music&meta=dmap.itemid,dmap.itemname,daap.songalbum,daap.songartist,daap.songtime,daap.songsize,daap.songtracknumber,daap.songdiscnumber,daap.songformat" +
                "&session-id=" + host.getSessionID() + "&revision-number=" + host.getRevisionNumber();
    }

    private void process() {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 8;
        processSingleDatabaseRequest();
        parseMLCL();
    }

    private void processSingleDatabaseRequest() {
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
        Song song = new Song();
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            song.host = host;
            switch (name) {
                case "miid":
                    song.id = Request.readInt(data, position, 4);
                    break;
                case "minm":
                    song.name = readString(data, position, size).trim();
                    break;
                case "asal":
                    song.album = readString(data, position, size).trim();
                    break;
                case "asar":
                    song.artist = readString(data, position, size).trim();
                    break;
                case "astn":
                    song.track = (short) readInt(data, position, 2);
                    break;
                case "asfm":
                    song.format = readString(data, position, size);
                    break;
                case "astm":
                    song.time = readInt(data, position, 4);
                    break;
                case "assz":
                    song.size = readInt(data, position, 4);
                    break;
                case "asdn":
                    song.disc_num = (short) readInt(data, position, 2);
                    break;
            }

            position += size;
        }
        mSongList.add(song);
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