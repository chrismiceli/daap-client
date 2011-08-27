/*
 * Created on May 6, 2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * Copyright 2003 Joseph Barnett
 * 
 * This File is part of "one 2 oh my god"
 * 
 * "one 2 oh my god" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * Free Software Foundation; either version 2 of the License, or
 * your option) any later version.
 * 
 * "one 2 oh my god" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with "one 2 oh my god"; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * 
 * update: Greg Jordan, 2004
 */
package org.mult.daap.client.daap.request;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.Hasher;

import android.util.Log;

/**
 * @author jbarnett To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class Request {
    protected DaapHost host;
    protected int response_code;
    protected String response_message;
    public byte[] data;
    protected int offset;
    protected HttpURLConnection httpc;
    protected int access_index;

    // start of song request.
    public Request(DaapHost h) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        host = h;
        response_code = -1;
        offset = 0;
        access_index = 2;
    }

    protected String getRequestString() {
        return "";
    }

    protected void query(String caller) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        query(caller, false);
    }

    // Retry if we don't recognize the response code and turn of the Accept-Encoding.
    // This resolves issue 58.
    protected void query(String caller, boolean retry)
            throws BadResponseCodeException, PasswordFailedException,
            IOException {
        URL url = new URL("http://" + host.getAddress() + ":" + host.getPort()
                + "/" + getRequestString());
        Log.v("Request", url.toString());
        httpc = (HttpURLConnection) url.openConnection();
        httpc.setConnectTimeout(45000);
        if (!retry) {
            httpc.setRequestProperty("Accept-Encoding", "identity");
        }
        addRequestProperties();
        httpc.connect();
        response_code = httpc.getResponseCode();
        if (response_code != HttpURLConnection.HTTP_OK
                && response_code != HttpURLConnection.HTTP_PARTIAL) {
            response_message = httpc.getResponseMessage();
            if (response_code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new PasswordFailedException("" + response_code + ": "
                        + response_message);
            }
            if (retry) {
                throw new BadResponseCodeException(response_code,
                        response_message + " by " + host.getName());
            }
            else {
                query(caller, true);
            }
        }
    }

    protected void readResponse() throws IOException {
        DataInputStream in = new DataInputStream(httpc.getInputStream());
        int len = httpc.getContentLength();
        if (httpc.getContentLength() == -1) {
            return;
        }
        data = new byte[len];
        in.readFully(data);
    }

    protected void addRequestProperties() {
        httpc.setRequestProperty("User-Agent", "iTunes/4.6 (Windows; N)");
        httpc.addRequestProperty("Accept-Language", "en-us, en;q=5.0");
        httpc.addRequestProperty("Client-DAAP-Access-Index",
                String.valueOf(access_index));
        httpc.addRequestProperty("Client-DAAP-Version", "3.0");
        httpc.addRequestProperty("Client-DAAP-Validation", getHashCode(this));
        // httpc.addRequestProperty("Accept-Encoding", "");
        if (host.isPasswordProtected()) {
            httpc.addRequestProperty("Authorization",
                    "Basic " + host.getPassword());
        }
        // httpc.addRequestProperty("Connection", "Close");
    }

    public int getResponseCode() {
        return response_code;
    }

    protected String getHashCode(Request r) {
        return Hasher.GenerateHash("/" + r.getRequestString(), this, false);
    }

    public static String readString(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
            // data,start,length, encoding
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected int dataInt() {
        return readInt(data, offset, 4);
    }

    protected static int readInt(byte[] data, int offset) {
        int i = 0;
        i = (((data[0 + offset] & 0xff) << 24)
                | ((data[1 + offset] & 0xff) << 16)
                | ((data[2 + offset] & 0xff) << 8) | (data[3 + offset] & 0xff));
        return i;
    }

    public DaapHost getHost() {
        return host;
    }

    public int getAccessIndex() {
        return access_index;
    }

    /* convert from hex in binary to decimal */
    public static int readInt(byte[] data, int offset, int size) {
        int i = 0;
        try {
            ByteArrayInputStream b = new ByteArrayInputStream(data, offset,
                    size);
            DataInputStream d = new DataInputStream(b);
            int pow = size * 2 - 1;
            for (int j = 0; j < size; j++) {
                int num = (0xFF & d.readByte());
                int up = ((int) Math.pow(16, pow)) * (num / 16);
                pow--;
                int down = ((int) Math.pow(16, pow)) * (num % 16);
                i += up + down;
                pow--;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return i;
    }
}