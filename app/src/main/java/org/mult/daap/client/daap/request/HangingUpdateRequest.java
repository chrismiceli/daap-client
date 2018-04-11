package org.mult.daap.client.daap.request;

import android.util.Pair;

import org.mult.daap.client.Host;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class HangingUpdateRequest extends Request {
    public HangingUpdateRequest(Host h) {
        super(h);
    }

    @Override
    public void Execute() throws IOException {
        query();
        readResponse();
    }

    @Override
    ArrayList<Pair<String, String>> getRequestProperties() {
        ArrayList<Pair<String, String>> requestProperties = new ArrayList<>();

        requestProperties.add(new Pair<>("Host", host.getAddress()));
        requestProperties.add(new Pair<>("Client-DAAP-Version", "3.0"));
        requestProperties.add(new Pair<>("User-Agent", "iTunes/4.6 (Windows; N)"));
        requestProperties.add(new Pair<>("Client-DAAP-Access-Index", Request.access_index));
        requestProperties.add(new Pair<>("Client-DAAP-Validation", getHashCode(this)));
        return requestProperties;
    }

    @Override
    protected String getRequestString() {
        String ret = "update?";
        ret += "session-id=" + host.getSessionID();
        ret += "&revision-number=" + host.getRevisionNumber();
        ret += "&delta=" + host.getRevisionNumber();
        return ret;
    }

    @Override
    void query() {
        try {
            URL url = new URL("http://" + host.getAddress() + ":" + host.getPort()
                    + "/" + getRequestString());
            httpc = (HttpURLConnection) url.openConnection();
            for(Pair<String, String> requestProperty : getRequestProperties()) {
                httpc.addRequestProperty(requestProperty.first, requestProperty.second);
            }

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
