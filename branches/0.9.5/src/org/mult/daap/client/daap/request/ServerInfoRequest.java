/*
 * Created on May 6, 2003
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * Copyright 2003 Joseph Barnett
 * This File is part of "one 2 oh my god"
 * "one 2 oh my god" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * Free Software Foundation; either version 2 of the License, or
 * your option) any later version.
 * "one 2 oh my god" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with "one 2 oh my god"; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.mult.daap.client.daap.request;

import java.io.IOException;

import org.mult.daap.client.daap.DaapHost;

import android.util.Log;

/** @author jbarnett To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * @created August 6, 2004 */
public class ServerInfoRequest extends Request {
	private double mServerVersion = 0.0;

	public ServerInfoRequest(DaapHost host) throws BadResponseCodeException,
			PasswordFailedException, IOException {
		super(host);
		query("ServerInfoRequest");
		readResponse();
		process();
	}

	public String getRequestString() {
		return "server-info";
	}

	public String getServerProgram() {
		return httpc.getHeaderField("Daap-Server");
	}

	public void process() {
		if (data.length == 0) {
			Log.d("Request", "Zero Length");
			return;
		}
		offset += 8;
		processServerInfoRequest();
	}

	public void processServerInfoRequest() {
		String name;
		int size;
		while (offset < data.length) {
			name = readString(data, offset, 4);
			offset += 4;
			size = readInt(data, offset);
			offset += 4;
			if (size > 10000000)
				Log.d("Request", "This host probably uses gzip encoding");
			if (name.equals("apro")) {
				// readInt(fp.value, 0, 2) + (0.01 * readInt(fp.value, 2, 2)
				mServerVersion = readInt(data, offset, 2)
						+ (0.01 * readInt(data, offset + 2, 2));
				break;
			}
			offset += size;
		}
	}

	public double getServerVersion() {
		return mServerVersion;
	}
}
