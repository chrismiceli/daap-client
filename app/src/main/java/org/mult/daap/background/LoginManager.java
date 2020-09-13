package org.mult.daap.background;

import org.mult.daap.Contents;
import org.mult.daap.client.daap.DaapHost;
import org.mult.daap.client.daap.request.PasswordFailedException;

import java.net.InetAddress;
import java.util.Observable;

public class LoginManager extends Observable implements Runnable {
    public final static Integer INITIATED = Integer.valueOf(-1);
    // public final static Integer FETCHED_MUSIC = new Integer(0);
    public final static Integer CONNECTION_FINISHED = Integer.valueOf(1);
    public final static Integer ERROR = Integer.valueOf(2);
    public final static Integer PASSWORD_FAILED = Integer.valueOf(3);
    public final String name;
    public final String address;
    public final String password;
    private final boolean login_required;
    private Integer lastMessage;
    private boolean interrupted;

    public LoginManager(String serverName, String add, String password,
                        boolean loginRequired) {
        this.name = serverName;
        this.address = add;
        this.password = password;
        this.login_required = loginRequired;
        this.interrupted = false;
        lastMessage = LoginManager.INITIATED;
    }

    public Integer getLastMessage() {
        return lastMessage;
    }

    public void interrupt() {
        interrupted = true;
    }

    private void notifyAndSet(Integer value) {
        lastMessage = value;
        setChanged();
        notifyObservers(value);
    }

    public void run() {
        try {
            String[] urlAddress = address.split(":");
            String hostname = urlAddress[0];
            int port = 3689;
            if (urlAddress.length == 1) { // No port specified use default
            } else if (urlAddress.length == 2) { // port specified
                port = Integer.valueOf(urlAddress[1]);
            } else if (urlAddress.length > 2) { // ipv6
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
            if (interrupted)
                return;
            if (login_required) {
                Contents.daapHost = new DaapHost(name, password,
                        Contents.address, port);
            } else {
                Contents.daapHost = new DaapHost(name, null, Contents.address,
                        port);
            }
            if (interrupted)
                return;
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
