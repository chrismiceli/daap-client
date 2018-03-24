package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;

public class LoginRequest extends Request {
    private int mSessionId;

    public LoginRequest(Host daapHost) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(daapHost);
        query("LoginRequest");
        byte[] data = readResponse();
        process(data);
    }

    @Override
    protected String getRequestString() {
        return "login";
    }

    private void process(byte[] data) {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 8;
        processLoginRequest(data);
    }

    private void processLoginRequest(byte[] data) {
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
