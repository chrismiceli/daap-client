package org.mult.daap.client.daap.request;

import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;

public class LogoutRequest extends Request {

    public LogoutRequest(Host daapHost) {
        super(daapHost);
    }

    public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException {
        query();
        readResponse();
    }

    protected String getRequestString() {
        return "logout?session-id=" + host.getSessionID();
    }
}
