package org.mult.daap.client.daap.request;

import java.io.IOException;

import org.mult.daap.client.daap.DaapHost;

import android.util.Log;

public class LogoutRequest extends Request {
	private int mSessionId;

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
