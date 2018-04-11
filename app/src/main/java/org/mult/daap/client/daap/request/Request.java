package org.mult.daap.client.daap.request;

import android.util.Log;
import android.util.Pair;

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
import java.util.ArrayList;

abstract class Request {
    final Host host;
    int offset = 0;
    HttpURLConnection httpc;
    final static String access_index = "2";

    Request(Host daapHost) {
        host = daapHost;
    }

    abstract protected String getRequestString();

    abstract public void Execute() throws BadResponseCodeException, PasswordFailedException, IOException;

    void query() throws BadResponseCodeException,
            PasswordFailedException, IOException {
        query(false);
    }

    // Retry if we don't recognize the response code and turn of the Accept-Encoding.
    // This resolves issue 58.
    private void query(boolean retry)
            throws BadResponseCodeException, PasswordFailedException,
            IOException {
        // needed for bug in android:
        // http://code.google.com/p/android/issues/detail?id=7786
        System.setProperty("http.keepAlive", "false");
        URL url = new URL("http://" + host.getAddress() + ":" + host.getPort()
                + "/" + getRequestString());
        Log.d("Request", url.toString());
        httpc = (HttpURLConnection) url.openConnection();
        httpc.setConnectTimeout(45000);
        if (!retry) {
            httpc.setRequestProperty("Accept-Encoding", "identity");
        }
        for(Pair<String, String> requestProperty : getRequestProperties()) {
            httpc.addRequestProperty(requestProperty.first, requestProperty.second);
        }

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
                query(true);
            }
        }
    }

    byte[] readResponse() throws IOException {
        DataInputStream in = new DataInputStream(httpc.getInputStream());
        int len = httpc.getContentLength();
        if (httpc.getContentLength() == -1) {
            return null;
        }
        byte[] data = new byte[len];
        in.readFully(data);
        return data;
    }

    ArrayList<Pair<String, String>> getRequestProperties() {
        ArrayList<Pair<String, String>> requestProperties = new ArrayList<>();
        requestProperties.add(new Pair<>("User-Agent", "iTunes/4.6 (Windows; N)"));
        requestProperties.add(new Pair<>("Accept-Language", "en-us, en;q=5.0"));
        requestProperties.add(new Pair<>("Client-DAAP-Access-Index", Request.access_index));
        requestProperties.add(new Pair<>("Client-DAAP-Version", "3.0"));
        requestProperties.add(new Pair<>("Client-DAAP-Validation", getHashCode(this)));
        if (host.isPasswordProtected()) {
            requestProperties.add(new Pair<>("Authorization", "Basic " + host.getPassword()));
        }

        return requestProperties;
    }

    String getHashCode(Request r) {
        return Hasher.GenerateHash("/" + r.getRequestString());
    }

    static String readString(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    static int readInt(byte[] data, int offset) {
        return (((data[offset] & 0xff) << 24)
                | ((data[1 + offset] & 0xff) << 16)
                | ((data[2 + offset] & 0xff) << 8)
                | (data[3 + offset] & 0xff));
    }

    /* convert from hex in binary to decimal */
    static int readInt(byte[] data, int offset, int size) {
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