package org.mult.daap.background;

import android.os.AsyncTask;
import android.util.Log;

import org.mult.daap.Contents;
import org.mult.daap.SongEntitySongFactory;
import org.mult.daap.client.Host;
import org.mult.daap.client.ILoginConsumer;
import org.mult.daap.client.daap.exception.PasswordFailedException;

import java.lang.ref.WeakReference;

public class LoginManagerAsyncTask extends AsyncTask<Void, Integer, Integer> {
    final public static int CONNECTION_FINISHED = 1;
    final public static int ERROR = 2;
    final public static int PASSWORD_FAILED = 3;
    final public String address;
    final public String password;
    final private WeakReference<ILoginConsumer> loginConsumerWeakReference;

    public LoginManagerAsyncTask(ILoginConsumer loginConsumer, String address, String password) {
        this.loginConsumerWeakReference = new WeakReference<>(loginConsumer);
        this.address = address;
        this.password = password;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        ILoginConsumer loginConsumer = this.loginConsumerWeakReference.get();
        if (loginConsumer != null && !loginConsumer.isFinishing()) {
            loginConsumer.onBeforeLogin();
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // login to the DAAP server, if successful server is stored in database
        try {
            String[] urlAddress = this.address.split(":");
            String hostname = urlAddress[0];

            if (Contents.daapHost != null) {
                Contents.clearLists();
            }

            Contents.daapHost = new Host(this.address, this.password, new SongEntitySongFactory());

            try {
                Contents.daapHost.connect();
            } catch (PasswordFailedException e) {
                return LoginManagerAsyncTask.PASSWORD_FAILED;
            }
        } catch (Exception e) {
            Log.e("DaapHost", e.getMessage());
            return LoginManagerAsyncTask.ERROR;
        }

        return LoginManagerAsyncTask.CONNECTION_FINISHED;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        ILoginConsumer loginConsumer = this.loginConsumerWeakReference.get();
        if (loginConsumer != null && !loginConsumer.isFinishing()) {
            loginConsumer.onAfterLogin(result);
        }
    }
}
