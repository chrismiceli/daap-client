package org.mult.daap.background;

import android.os.AsyncTask;

import org.mult.daap.AddServerMenu;
import org.mult.daap.Contents;
import org.mult.daap.client.Host;
import org.mult.daap.client.daap.exception.PasswordFailedException;
import org.mult.daap.db.entity.ServerEntity;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class LoginManager extends AsyncTask<Void, Integer, Integer> {
    final public static int INITIATED = 1;
    final public static int CONNECTION_FINISHED = 2;
    final private static int ERROR = 3;
    final public static int PASSWORD_FAILED = 4;
    final public ServerEntity server;
    final private WeakReference<AddServerMenu> addServerMenu;

    public LoginManager(AddServerMenu addServerMenu, ServerEntity server) {
        this.addServerMenu = new WeakReference<>(addServerMenu);
        this.server = server;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            addServerMenu.onBeforeLogin();
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // login to the DAAP server, if successful server is stored in database
        try {
            String[] urlAddress = this.server.getAddress().split(":");
            String hostname = urlAddress[0];

            Contents.address = InetAddress.getByName(hostname);

            if (Contents.daapHost != null) {
                try {
                    Contents.daapHost.logout();
                } catch (Exception e) {
                    // do nothing
                }

                Contents.clearLists();
            }

            Contents.daapHost = new Host(this.server.getPassword(), Contents.address, this.server.getPort());

            try {
                Contents.daapHost.connect();
            } catch (PasswordFailedException e) {
                publishProgress(LoginManager.PASSWORD_FAILED);
                return LoginManager.PASSWORD_FAILED;
            }
        } catch (Exception e) {
            publishProgress(LoginManager.ERROR);
            return LoginManager.ERROR;
        }

        publishProgress(LoginManager.CONNECTION_FINISHED);
        return LoginManager.CONNECTION_FINISHED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int state = values[0];
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            addServerMenu.onLoginUpdate(state, this.server);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
                addServerMenu.onAfterLogin(result);
            }
    }
}
