package org.mult.daap.client.daap.request;

import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.IOException;

public class ServerInfoRequest extends Request {
    public ServerInfoRequest(Host host) {
        super(host);
    }

    public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException {
        query();
        readResponse();
    }

    public String getRequestString() {
        return "server-info";
    }

    public String getServerProgram() {
        return httpc.getHeaderField("Daap-Server");
    }
}
