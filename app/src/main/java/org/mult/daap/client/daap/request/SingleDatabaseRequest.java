package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.ISong;
import org.mult.daap.client.ISongFactory;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;
import java.util.ArrayList;

public class SingleDatabaseRequest extends Request {
    private final ArrayList<ISong> mSongList = new ArrayList<>();
    private final ISongFactory songFactory;

    public SingleDatabaseRequest(Host daapHost) {
        super(daapHost);
        this.songFactory = this.host.getSongFactory();
    }

    public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException {
        query();
        byte[] data = readResponse();
        process(data);
    }

    @Override
    protected String getRequestString() {
        return "databases/" + host.getDatabaseID() + "/items?type=" +
                "music&meta=dmap.itemid,dmap.itemname,daap.songalbum,daap.songartist,daap.songtime,daap.songsize,daap.songtracknumber,daap.songdiscnumber,daap.songformat" +
                "&session-id=" + host.getSessionID() + "&revision-number=" + host.getRevisionNumber();
    }

    private void process(byte[] data) {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 8;
        ArrayList<FieldPair> mlclList = processSingleDatabaseRequest(data);
        parseMLCL(data, mlclList);
    }

    private ArrayList<FieldPair> processSingleDatabaseRequest(byte[] data) {
        ArrayList<FieldPair> mlclList = new ArrayList<>();
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

        return mlclList;
    }

    private void parseMLCL(byte[] data, ArrayList<FieldPair> mlclList) {
        ArrayList<FieldPair> mlitList = new ArrayList<>();
        for (FieldPair mlcl : mlclList) {
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

        int id = 0;
        String songName = null;
        int time = 0;
        String album = null;
        String artist = null;
        short track = 0;
        short discNum = 0;
        String format = null;
        int songSize = 0;

        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            switch (name) {
                case "miid":
                    id = Request.readInt(data, position, 4);
                    break;
                case "minm":
                    songName = readString(data, position, size).trim();
                    break;
                case "asal":
                    album = readString(data, position, size).trim();
                    break;
                case "asar":
                    artist = readString(data, position, size).trim();
                    break;
                case "astn":
                    track = (short) readInt(data, position, 2);
                    break;
                case "asfm":
                    format = readString(data, position, size);
                    break;
                case "astm":
                    time = readInt(data, position, 4);
                    break;
                case "assz":
                    songSize = readInt(data, position, 4);
                    break;
                case "asdn":
                    discNum = (short) readInt(data, position, 2);
                    break;
            }

            position += size;
        }
        mSongList.add(this.songFactory.createSong(id, songName, time, album, artist, track, discNum, format, songSize));
    }

    private ArrayList<FieldPair> processContainerList(byte[] data, int position, int argSize) {
        String name;
        ArrayList<FieldPair> mlitList = new ArrayList<>();
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

        return mlitList;
    }

    public ArrayList<ISong> getSongs() {
        return mSongList;
    }
}