package org.mult.daap.client.daap.exception;

public class PasswordFailedException extends Exception {

    /** Constructor for the PasswordFailedException object
     * @param serverResponse
     * Description of the Parameter */
    public PasswordFailedException(String serverResponse) {
        super(serverResponse);
    }
}
