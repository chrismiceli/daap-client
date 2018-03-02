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
import java.util.ArrayList;

import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.Database;

import android.util.Log;

public class DatabasesRequest extends Request {
	private class FieldPair {
		public FieldPair(int s, int p) {
			size = s;
			position = p;
		}

		public int position;
		public int size;
	}

	private Database database;
	private ArrayList<FieldPair> mlclList;
	private ArrayList<FieldPair> mlitList;

	public DatabasesRequest(DaapHost h) throws BadResponseCodeException,
			PasswordFailedException, IOException {
		super(h);
		mlclList = new ArrayList<>();
		mlitList = new ArrayList<>();
		query("DabasesRequest");
		readResponse();
		process();
	}

	protected String getRequestString() {
		String ret = "databases?";
		ret += "session-id=" + host.getSessionID();
		ret += "&revision-number=" + host.getRevisionNumber();
		return ret;
	}

	protected void addRequestProperties() {
		super.addRequestProperties();
	}

	protected void process() {
		mlclList = new ArrayList<FieldPair>();
		mlitList = new ArrayList<FieldPair>();
		if (data.length == 0) {
			Log.d("Request", "Zero Length");
			return;
		}
		offset += 4;
		offset += 4;
		processDatabaseRequest();
		parseMLCL();
	}

	public void processDatabaseRequest() {
		String name;
		int size;
		while (offset < data.length) {
			name = readString(data, offset, 4);
			offset += 4;
			size = readInt(data, offset);
			offset += 4;
			if (size > 10000000)
				Log.d("Request", "This host probably uses gzip encoding");
			if (name.equals("mlcl")) {
				mlclList.add(new FieldPair(size, offset));
			}
			offset += size;
		}
	}

	/* Creates a list of byte arrays for use in mLIT */
	protected void parseMLCL() {
		for (int i = 0; i < mlclList.size(); i++) {
			processContainerList(mlclList.get(i).position, mlclList.get(i).size);
		}
		parseMLIT();
	}

	protected void parseMLIT() {
        processmLitList(mlitList.get(0).position, mlitList.get(0).size);
	}

	/* get all mlit in mlclList */
	public void processContainerList(int position, int argSize) {
		String name;
		int size;
		int startPos = position;
		while (position < argSize + startPos) {
			name = readString(data, position, 4);
			position += 4;
			size = readInt(data, position);
			position += 4;
			if (name.equals("mlit")) {
				mlitList.add(new FieldPair(size, position));
			}
			position += size;
		}
	}

	public void processmLitList(int position, int argSize) {
		String name = "";
		int size;
		int startPos = position;
		database = new Database();
		boolean bMiid = false;
		boolean bMinm = false;
		while (position < argSize + startPos) {
			name = readString(data, position, 4);
			position += 4;
			size = readInt(data, position);
			position += 4;
			if (name.equals("miid")) {
				bMiid = true;
				database.id = readInt(data, position);
			} else if (name.equals("minm")) {
				bMinm = true;
				database.name = readString(data, position, size);
			}
			if (bMiid == true && bMinm == true) {
				bMiid = false;
				bMinm = false;
				break;
			}
			position += size;
		}
	}

	public Database getDatabase() {
		return database;
	}
}
