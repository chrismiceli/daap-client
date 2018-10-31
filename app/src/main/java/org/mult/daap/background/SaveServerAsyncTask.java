package org.mult.daap.background;

import android.os.AsyncTask;

import org.mult.daap.AddServerMenu;
import org.mult.daap.client.DatabaseHost;
import org.mult.daap.client.Host;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SaveServerAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private final Host host;
    private final WeakReference<AddServerMenu> addServerMenu;

    public SaveServerAsyncTask(AddServerMenu addserverMenu, Host host) {
        this.addServerMenu = new WeakReference<>(addserverMenu);
        this.host = host;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            DatabaseHost databaseHost = new DatabaseHost(addServerMenu.getApplicationContext());
            databaseHost.setServer(this.host, addServerMenu);
            return true;
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            addServerMenu.onAfterSave();
        }
    }
}
