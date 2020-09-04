package org.mult.daap.client.daap.request;

/**
 * @author wooo
 * @created July 31, 2004
 */
@SuppressWarnings("serial")
public class PasswordFailedException extends Exception {
    /**
     * Constructor for the PasswordFailedException object
     */
    public PasswordFailedException() {
        super("Password Authentication Failed!");
    }

    /**
     * Constructor for the PasswordFailedException object
     *
     * @param serverResponse Description of the Parameter
     */
    public PasswordFailedException(String serverResponse) {
        super(serverResponse);
    }
}
