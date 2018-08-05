package org.mult.daap.background;

import android.os.AsyncTask;

import org.mult.daap.AddServerMenu;
import org.mult.daap.client.Host;
import org.mult.daap.client.ISong;
import org.mult.daap.client.Playlist;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.db.entity.SongEntity;
import org.mult.daap.model.Server;

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
            ServerDao serverDao = AppDatabase.getInstance(addServerMenu).serverDao();
            SongDao songDao = AppDatabase.getInstance(addServerMenu).songDao();
            PlaylistDao playlistDao = AppDatabase.getInstance(addServerMenu).playlistDao();
            List<SongEntity> songs = new ArrayList<>();
            List<PlaylistEntity> playlists = new ArrayList<>();
            try {
                this.host.grabSongs();
                for (ISong song : this.host.getSongs()) {
                    songs.add(new SongEntity(song));
                }

                for (Playlist playlist : this.host.getPlaylists()) {
                    playlists.add(new PlaylistEntity(playlist.getId(), playlist.getName()));
                }

                serverDao.setDaapServer(new ServerEntity(this.host.getAddress(), this.host.getPassword()));
                songDao.setSongs(songs);
                playlistDao.setPlaylists(playlists);
                return true;
            } catch (Exception e) {
                return false;
            }
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
