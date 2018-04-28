package org.mult.daap.background;

import android.os.AsyncTask;

import org.mult.daap.AddServerMenu;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.entity.ServerEntity;

import java.lang.ref.WeakReference;

public class SaveServerAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private final ServerEntity server;
    private final WeakReference<AddServerMenu> addServerMenu;

    public SaveServerAsyncTask(AddServerMenu addserverMenu, ServerEntity server) {
        this.addServerMenu = new WeakReference<>(addserverMenu);
        this.server = server;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            ServerDao serverDao = AppDatabase.getInstance(addServerMenu).serverDao();
            serverDao.setDaapServer(server);
            return true;
        }

        return false;
    }
}
