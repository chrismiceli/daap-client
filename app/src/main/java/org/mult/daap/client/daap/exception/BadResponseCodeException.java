/*
 * Created on Aug 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mult.daap.client.daap.exception;

public class BadResponseCodeException extends Exception {
    private int responseCode;

    public BadResponseCodeException(int responseCode) {
        super(Integer.toString(responseCode));
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
