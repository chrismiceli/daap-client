package org.mult.daap.client.daap.request;

import android.util.Log;

import org.mult.daap.client.Host;
import org.mult.daap.client.daap.Hasher;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Request {
    protected final Host host;
    protected int offset = 0;
    protected HttpURLConnection httpc;
    protected final int access_index = 2;

    public Request(Host daapHost) {
        // needed for bug in android:
        // http://code.google.com/p/android/issues/detail?id=7786
        System.setProperty("http.keepAlive", "false");
        host = daapHost;
    }

    abstract protected String getRequestString();

    protected void query(String caller) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        query(caller, false);
    }

    // Retry if we don't recognize the response code and turn of the Accept-Encoding.
    // This resolves issue 58.
    private void query(String caller, boolean retry)
            throws BadResponseCodeException, PasswordFailedException,
            IOException {
        URL url = new URL("http://" + host.getAddress() + ":" + host.getPort()
                + "/" + getRequestString());
        Log.d("Request", url.toString());
        httpc = (HttpURLConnection) url.openConnection();
        httpc.setConnectTimeout(45000);
        if (!retry) {
            httpc.setRequestProperty("Accept-Encoding", "identity");
        }
        addRequestProperties();
        httpc.connect();
        int responseCode = httpc.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK &&
            responseCode != HttpURLConnection.HTTP_PARTIAL) {
            String response_message = httpc.getResponseMessage();
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new PasswordFailedException("" + responseCode + ": "
                        + response_message);
            }
            if (retry) {
                throw new BadResponseCodeException(responseCode);
            }
            else {
                query(caller, true);
            }
        }
    }

    protected byte[] readResponse() throws IOException {
        DataInputStream in = new DataInputStream(httpc.getInputStream());
        int len = httpc.getContentLength();
        if (httpc.getContentLength() == -1) {
            return null;
        }
        byte[] data = new byte[len];
        in.readFully(data);
        return data;
    }

    protected void addRequestProperties() {
        httpc.setRequestProperty("User-Agent", "iTunes/4.6 (Windows; N)");
        httpc.addRequestProperty("Accept-Language", "en-us, en;q=5.0");
        httpc.addRequestProperty("Client-DAAP-Access-Index",
                String.valueOf(access_index));
        httpc.addRequestProperty("Client-DAAP-Version", "3.0");
        httpc.addRequestProperty("Client-DAAP-Validation", getHashCode(this));
        if (host.isPasswordProtected()) {
            httpc.addRequestProperty("Authorization",
                    "Basic " + host.getPassword());
        }
    }

    protected String getHashCode(Request r) {
        return Hasher.GenerateHash("/" + r.getRequestString());
    }

    protected static String readString(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected static int readInt(byte[] data, int offset) {
        return (((data[offset] & 0xff) << 24)
                | ((data[1 + offset] & 0xff) << 16)
                | ((data[2 + offset] & 0xff) << 8)
                | (data[3 + offset] & 0xff));
    }

    /* convert from hex in binary to decimal */
    protected static int readInt(byte[] data, int offset, int size) {
        int i = 0;
        try {
            ByteArrayInputStream b = new ByteArrayInputStream(data, offset, size);
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