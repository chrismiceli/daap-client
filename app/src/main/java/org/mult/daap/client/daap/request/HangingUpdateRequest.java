package org.mult.daap.client.daap.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.mult.daap.client.daap.DaapHost;

public class HangingUpdateRequest extends Request {
    /** Constructor for the HangingUpdateRequest object
     * @throws BadResponseCodeException
     * @throws PasswordFailedException
     * @throws IOException */
    public HangingUpdateRequest(DaapHost h) throws BadResponseCodeException,
            PasswordFailedException, IOException {
        super(h);
        query();
        readResponse();
    }

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
}
