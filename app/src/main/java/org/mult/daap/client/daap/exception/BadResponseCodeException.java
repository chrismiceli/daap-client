/*
 * Created on Aug 10, 2004
 *
 */
package org.mult.daap.client.daap.exception;

public class BadResponseCodeException extends Exception {
    private final int responseCode;

    public BadResponseCodeException(int responseCode) {
        super(Integer.toString(responseCode));
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
