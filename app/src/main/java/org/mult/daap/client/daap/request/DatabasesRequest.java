package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;
import java.util.ArrayList;

public class DatabasesRequest extends Request {
    private int databaseId;


    public DatabasesRequest(Host daapHost) {
        super(daapHost);
    }

    @Override
    public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException {
        query();
        byte[] data = readResponse();
        process(data);
    }

    public int getDatabaseId() {
        return databaseId;
    }

    @Override
    protected String getRequestString() {
        return "databases?session-id=" + host.getSessionID() + "&revision-number=" + host.getRevisionNumber();
    }

    private void process(byte[] data) {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 4;
        offset += 4;
        ArrayList<FieldPair> mlclList = processDatabaseRequest(data);
        parseMLCL(data, mlclList);
    }

    private ArrayList<FieldPair> processDatabaseRequest(byte[] data) {
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
        processMlitList(data, mlitList.get(0).position, mlitList.get(0).size);
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

    private void processMlitList(byte[] data, int position, int argSize) {
        String name;
        int size;
        int startPos = position;
        while (position < argSize + startPos) {
            name = readString(data, position, 4);
            position += 4;
            size = readInt(data, position);
            position += 4;
            if (name.equals("miid")) {
                databaseId = readInt(data, position);
                break;
            }
            position += size;
        }
    }
}
