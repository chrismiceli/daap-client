package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.net.InetAddress;
import java.util.Observable;

public class LoginManager extends Observable implements Runnable {
    public final static int INITIATED = -1;
    public final static int CONNECTION_FINISHED = 1;
    public final static int ERROR = 2;
    public final static int PASSWORD_FAILED = 3;
    public final String address;
    public final String password;
    private final boolean login_required;
    private int lastMessage;
    private boolean interrupted;

    public LoginManager(String add, String password, boolean loginRequired) {
        this.address = add;
        this.password = password;
        this.login_required = loginRequired;
        this.interrupted = false;
        lastMessage = LoginManager.INITIATED;
    }

    public int getLastMessage() {
        return lastMessage;
    }

    public void interrupt() {
        interrupted = true;
    }

    private void notifyAndSet(int value) {
        lastMessage = value;
        setChanged();
        notifyObservers(value);
    }

    public void run() {
        try {
            String[] urlAddress = address.split(":");
            String hostname = urlAddress[0];
            int port = 3689;
            if (urlAddress.length == 2) { // port specified
                port = Integer.valueOf(urlAddress[1]);
            }
            else if (urlAddress.length > 2) { // ipv6
                port = Integer.valueOf(urlAddress[urlAddress.length - 1]);
            }
            Contents.address = InetAddress.getByName(hostname);
            if (Contents.daapHost != null) {
                try {
                    Contents.daapHost.logout();
                    Contents.playlist_position = -1;
                } catch (Exception e) {
                    // Do nothing
                }
                Contents.clearLists();
            }
            if (interrupted) {
                return;
            }
            if (login_required) {
                Contents.daapHost = new DaapHost(password, Contents.address, port);
            }
            else {
                Contents.daapHost = new DaapHost(null, Contents.address, port);
            }
            if (interrupted) {
                return;
            }
            try {
                Contents.daapHost.connect();
            } catch (PasswordFailedException e) {
                notifyAndSet(LoginManager.PASSWORD_FAILED);
                return;
            }
            if (interrupted)
                return;
            notifyAndSet(LoginManager.CONNECTION_FINISHED);
        } catch (Exception e) {
            e.printStackTrace();
            if (interrupted)
                return;
            notifyAndSet(LoginManager.ERROR);
        }
    }
}
