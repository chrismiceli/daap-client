package org.mult.daap.client.daap.request;

import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;

public class LogoutRequest extends Request {

    public LogoutRequest(DaapHost daapHost) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(daapHost);
        query("LogoutRequest");
        readResponse();
    }

    protected String getRequestString() {
        return "logout?session-id=" + host.getSessionID();
    }
}
