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

import android.util.Log;

import org.mult.daap.client.daap.DaapHost;

import java.io.IOException;

/**
 * @author jbarnett
 * @created July 15, 2004
 */
public class UpdateRequest extends Request {
    private int mRevisionNumber = 0;

    public UpdateRequest(DaapHost h) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(h);
        query("UpdateRequest");
        readResponse();
        process();
    }

    protected String getRequestString() {
        String ret = "update?";
        ret += "session-id=" + host.getSessionID();
        ret += "&revision-number=" + host.getRevisionNumber();
        return ret;
    }

    protected void addRequestProperties() {
        super.addRequestProperties();
        httpc.addRequestProperty("Host", host.getAddress());
    }

    protected void process() {
        if (data.length == 0) {
            Log.d("Request", "Zero Length");
            return;
        }
        offset += 4;
        offset += 4;
        processUpdateRequest();
    }

    public void processUpdateRequest() {
        String name;
        int size;
        while (offset < data.length) {
            name = readString(data, offset, 4);
            offset += 4;
            size = readInt(data, offset);
            offset += 4;
            if (size > 10000000)
                Log.d("Request", "This host probably uses gzip encoding");
            if (name.equals("musr")) {
                mRevisionNumber = readInt(data, offset); // read 4 bytes
                break;
            }
            offset += size;
        }
    }

    public int getRevNum() {
        return mRevisionNumber;
    }
}
