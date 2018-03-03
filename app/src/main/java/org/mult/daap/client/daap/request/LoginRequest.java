package org.mult.daap.client.daap.request;

import java.io.IOException;

import org.mult.daap.client.daap.DaapHost;

import android.util.Log;

public class LoginRequest extends Request {
    private int mSessionId;

    public LoginRequest(DaapHost daapHost) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(daapHost);
        query("LoginRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        return "login";
    }

    protected void addRequestProperties() {
        super.addRequestProperties();
    }

    protected void process() {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 8;
        processLoginRequest();
    }

    public void processLoginRequest() {
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
            if (name.equals("mlid")) {
                mSessionId = readInt(data, offset); // read 4 bytes
                return;
            }
            offset += size;
        }
    }

    public int getSessionId() {
        return mSessionId;
    }
}
