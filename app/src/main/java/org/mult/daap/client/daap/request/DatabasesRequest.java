package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.Database;

import java.io.IOException;
import java.util.ArrayList;

public class DatabasesRequest extends Request {
    private Database database;
    private ArrayList<FieldPair> mlclList = new ArrayList<>();
    private ArrayList<FieldPair> mlitList = new ArrayList<>();

    public DatabasesRequest(DaapHost daapHost) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(daapHost);
        query("DabasesRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        return "databases?session-id=" + host.getSessionID() + "&revision-number=" + host.getRevisionNumber();
    }

    private void process() {
        mlclList = new ArrayList<>();
        mlitList = new ArrayList<>();
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 4;
        offset += 4;
        processDatabaseRequest();
        parseMLCL();
    }

    private void processDatabaseRequest() {
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
    private void parseMLCL() {
        for (int i = 0; i < mlclList.size(); i++) {
            processContainerList(mlclList.get(i).position, mlclList.get(i).size);
        }
        parseMLIT();
    }

    private void parseMLIT() {
        processmLitList(mlitList.get(0).position, mlitList.get(0).size);
    }

    /* get all mlit in mlclList */
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

    private void processmLitList(int position, int argSize) {
        String name;
        int size;
        int startPos = position;
        database = new Database();
        boolean bMiid = false;
        boolean bMinm = false;
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            if (name.equals("miid")) {
                bMiid = true;
                database.id = readInt(data, position);
            } else if (name.equals("minm")) {
                bMinm = true;
                database.name = readString(data, position, size);
            }
            if (bMiid && bMinm) {
                break;
            }
            position += size;
        }
    }

    public Database getDatabase() {
        return database;
    }
}
