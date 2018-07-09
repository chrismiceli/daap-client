package org.mult.daap.background;

import android.os.AsyncTask;

import org.mult.daap.AddServerMenu;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.entity.ServerEntity;

import java.lang.ref.WeakReference;

public class GetServerAsyncTask extends AsyncTask<Void,Void, ServerEntity> {
    private final WeakReference<AddServerMenu> addServerMenu;

    public GetServerAsyncTask(AddServerMenu addServerMenu) {
        this.addServerMenu = new WeakReference<>(addServerMenu);
    }

    @Override
    protected ServerEntity doInBackground(Void...voids){
        ServerEntity result = null;
        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            ServerDao serverDao = AppDatabase.getInstance(addServerMenu).serverDao();
            ServerEntity server = serverDao.loadServer();
            if (server != null) {
                result = server;
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(ServerEntity serverEntity) {
        super.onPostExecute(serverEntity);

        AddServerMenu addServerMenu = this.addServerMenu.get();
        if (addServerMenu != null && !addServerMenu.isFinishing()) {
            addServerMenu.OnServerRetrieved(serverEntity);
        }
    }
}
