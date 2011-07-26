/*
    Created on May 6, 2003
    To change the template for this generated file go to
    Window>Preferences>Java>Code Generation>Code and Comments
    Copyright 2003 Joseph Barnett
    This File is part of "one 2 oh my god"
    "one 2 oh my god" is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    Free Software Foundation; either version 2 of the License, or
    your option) any later version.
    "one 2 oh my god" is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with "one 2 oh my god"; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.mult.daap.client.daap.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.mult.daap.client.daap.DaapHost;

/**
 * @author jbarnett To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 * @created July 15, 2004
 */
public class HangingUpdateRequest extends Request {

	/**
	 * Constructor for the HangingUpdateRequest object
	 * 
	 * @throws BadResponseCodeException
	 * @throws PasswordFailedException
	 * @throws IOException
	 */
	public HangingUpdateRequest(DaapHost h) throws BadResponseCodeException,
			PasswordFailedException, IOException {
		super(h);
		query();
		readResponse();
		// process();
	}

	protected void addRequestProperties() {
		// super.addRequestProperties();
		// httpc.addRequestProperty("Connection", "Close");
		httpc.addRequestProperty("Host", host.getAddress());
		httpc.addRequestProperty("Client-DAAP-Version", "3.0");
		httpc.setRequestProperty("User-Agent", "iTunes/4.6 (Windows; N)");
		httpc.addRequestProperty("Client-DAAP-Access-Index", String
				.valueOf(access_index));
		httpc.addRequestProperty("Client-DAAP-Validation", getHashCode(this));
	}

	protected String getRequestString() {
		String ret = "update?";
		ret += "session-id=" + host.getSessionID();
		ret += "&revision-number=" + host.getRevisionNumber();
		ret += "&delta=" + host.getRevisionNumber();
		return ret;
	}

	protected void process() {
	}

	protected void readResponse() {
	}

	protected void query() throws BadResponseCodeException,
			PasswordFailedException {
		URL url = null;
		try {
			url = new URL("http://" + host.getAddress() + ":" + host.getPort()
					+ "/" + getRequestString());
			httpc = (HttpURLConnection) url.openConnection();
			addRequestProperties();
			data = new byte[0];
			httpc.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Description of the Method */
	public void disconnect() {
		httpc.disconnect();
	}

	/**
	 * Gets the revNum attribute of the HangingUpdateRequest object
	 * 
	 * @return The revNum value
	 */
	public int getRevNum() {
		return -1;
	}
}
