package org.mult.daap.client.daap.request;

import org.mult.daap.client.Host;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HangingUpdateRequest extends Request {
    /** Constructor for the HangingUpdateRequest object
     * @throws IOException */
    public HangingUpdateRequest(Host h) throws IOException {
        super(h);
        query();
        readResponse();
    }

    @Override
    protected void addRequestProperties() {
        // super.addRequestProperties();
        // httpc.addRequestProperty("Connection", "Close");
        httpc.addRequestProperty("Host", host.getAddress());
        httpc.addRequestProperty("Client-DAAP-Version", "3.0");
        httpc.setRequestProperty("User-Agent", "iTunes/4.6 (Windows; N)");
        httpc.addRequestProperty("Client-DAAP-Access-Index",
                String.valueOf(access_index));
        httpc.addRequestProperty("Client-DAAP-Validation", getHashCode(this));
    }

    @Override
    protected String getRequestString() {
        String ret = "update?";
        ret += "session-id=" + host.getSessionID();
        ret += "&revision-number=" + host.getRevisionNumber();
        ret += "&delta=" + host.getRevisionNumber();
        return ret;
    }

    private void query() {
        try {
            URL url = new URL("http://" + host.getAddress() + ":" + host.getPort()
                    + "/" + getRequestString());
            httpc = (HttpURLConnection) url.openConnection();
            addRequestProperties();
            httpc.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Description of the Method */
    public void disconnect() {
        httpc.disconnect();
    }
}
