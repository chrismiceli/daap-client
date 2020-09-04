/*
 * Created on May 6, 2003
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * Copyright 2003 Joseph Barnett
 * This File is part of "one 2 oh my god"
 * "one 2 oh my god" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * Free Software Foundation; either version 2 of the License, or
 * your option) any later version.
 * "one 2 oh my god" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with "one 2 oh my god"; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapHost;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author jbarnett To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * @created
 */
public class SingleDatabaseRequest extends Request {
    private class FieldPair {
        public FieldPair(int s, int p) {
            size = s;
            position = p;
        }

        public int position;
        public int size;
    }

    private ArrayList<Song> mSongList;
    private ArrayList<FieldPair> mlclList;
    private ArrayList<FieldPair> mlitList;

    public SingleDatabaseRequest(DaapHost h) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(h);
        mSongList = new ArrayList<Song>();
        mlclList = new ArrayList<FieldPair>();
        mlitList = new ArrayList<FieldPair>();
        query("SingleDatabaseRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        String ret = "databases/";
        ret += host.getDatabaseID() + "/";
        ret += "items?";
        ret += "type=music&";
        // ret +=
        // "meta=dmap.itemid,dmap.itemname,daap.songalbum,daap.songartist,daap.songtracknumber,daap.songgenre,daap.songformat,daap.songtime,daap.songsize,daap.songbitrate,daap.songcompilation";
        ret += "meta=dmap.itemid,dmap.itemname,daap.songalbum,daap.songartist,daap.songtime,daap.songsize,daap.songtracknumber,daap.songdiscnumber,daap.songformat";
        ret += "&session-id=" + host.getSessionID();
        ret += "&revision-number=" + host.getRevisionNumber();
        return ret;
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
        Song s = new Song();
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            s.host = host;
            if (name.equals("miid")) {
                s.id = Request.readInt(data, position, 4);
            } else if (name.equals("minm")) {
                s.name = readString(data, position, size).trim();
                // } else if (name.equals("mper")) {
                // s.persistent_id = readString(data, position, size).trim();
            } else if (name.equals("asal")) {
                s.album = readString(data, position, size).trim();
            } else if (name.equals("asar")) {
                s.artist = readString(data, position, size).trim();
            } else if (name.equals("astn")) {
                s.track = (short) readInt(data, position, 2);
                // } else if (name.equals("asgn")) {
                // s.genre = readString(data, position, size);
            } else if (name.equals("asfm")) {
                s.format = readString(data, position, size);
            } else if (name.equals("astm")) {
                s.time = readInt(data, position, 4);
            } else if (name.equals("assz")) {
                s.size = readInt(data, position, 4);
                // } else if (name.equals("asco")) {
                // s.compilation = (readInt(data, position, 1) == 1);
            } else if (name.equals("asdn")) {
                s.disc_num = (short) readInt(data, position, 2);
                // } else if (name.equals("asbr")) {
                // s.bitrate = readInt(data, position, 2);
            }
            position += size;
        }
        mSongList.add(s);
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