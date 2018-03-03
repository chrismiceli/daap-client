package org.mult.daap.client.daap.request;

import java.io.IOException;

import org.mult.daap.client.daap.DaapHost;

import android.util.Log;

public class UpdateRequest extends Request {
    private int mRevisionNumber = 0;

    public UpdateRequest(DaapHost h) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(h);
        query("UpdateRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        return "update?session-id=" + host.getSessionID() +
                "&revision-number=" + host.getRevisionNumber();
    }

    protected void addRequestProperties() {
        super.addRequestProperties();
        httpc.addRequestProperty("Host", host.getAddress());
    }

    protected void process() {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 8;
        processUpdateRequest();
    }

    public void processUpdateRequest() {
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
            if (name.equals("musr")) {
                mRevisionNumber = readInt(data, offset); // read 4 bytes
                return;
            }
            offset += size;
        }
    }

    public int getRevNum() {
        return mRevisionNumber;
    }
}
