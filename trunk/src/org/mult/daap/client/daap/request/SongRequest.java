/*
 * Created on May 7, 2003
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.mult.daap.client.Song;
import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.Hasher;

/** @author jbarnett To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * @created August 6, 2004 */
public class SongRequest extends Request {
	protected BufferedInputStream b;
	protected long skip_bytes;
	protected Song song;

	public SongRequest(DaapHost h, Song s, long bytes)
			throws PasswordFailedException, BadResponseCodeException,
			IOException {
		super(h);
		host.getNextRequestNumber();
		song = s;
		skip_bytes = bytes;
		query("SongRequest");
		readResponse();
		process();
	}

	protected void addRequestProperties() {
		// httpc.addRequestProperty("Host", "" + host.getAddress() + ":"
		// + host.getPort() + "/");
		httpc.addRequestProperty("Host", "" + host.getAddress());
		httpc.addRequestProperty("Accept", "*/*");
		httpc.addRequestProperty("Cache-Control", "no-cache");
		super.addRequestProperties();
		httpc.addRequestProperty("Client-DAAP-Request-ID",
				"" + host.getThisRequestNumber());
		if (skip_bytes > 0)
			httpc.addRequestProperty("Range", "bytes=" + skip_bytes + "-");
		httpc.addRequestProperty("Connection", "close");
	}

	protected String getRequestString() {
		String ret = "databases/" + host.getDatabaseID();
		ret += "/items/" + song.id + "." + song.format;
		ret += "?session-id=" + host.getSessionID();
		return ret;
	}

	public URL getSongURL() throws MalformedURLException {
		return new URL("http://" + host.getAddress() + ":" + host.getPort()
				+ "/" + getRequestString());
	}

	protected void readResponse() throws IOException {
		b = new BufferedInputStream(httpc.getInputStream(), 8192);
	}

	protected String getHashCode(Request r) {
		return Hasher.GenerateHash("/" + r.getRequestString(), this, true);
	}

	protected void process() {
	}

	public InputStream getStream() {
		return b;
	}
}
