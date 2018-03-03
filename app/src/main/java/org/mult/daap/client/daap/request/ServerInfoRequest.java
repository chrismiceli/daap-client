package org.mult.daap.client.daap.request;

import java.io.IOException;

import org.mult.daap.client.daap.DaapHost;

import android.util.Log;

public class ServerInfoRequest extends Request {
	public ServerInfoRequest(DaapHost host) throws BadResponseCodeException,
			PasswordFailedException, IOException {
		super(host);
		query("ServerInfoRequest");
		readResponse();
	}

	public String getRequestString() {
		return "server-info";
	}

	public String getServerProgram() {
		return httpc.getHeaderField("Daap-Server");
	}
}
