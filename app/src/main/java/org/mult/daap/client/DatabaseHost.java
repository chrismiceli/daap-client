package org.mult.daap.client;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import org.mult.daap.R;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.QueueDao;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.PlaylistSongEntity;
import org.mult.daap.db.entity.QueueEntity;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHost {
    private final Context applicationContext;

    public DatabaseHost(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ServerEntity getServer() {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContext).serverDao();
        return serverDao.loadServer();
    }

    public List<PlaylistEntity> getPlaylists() {
        PlaylistDao playlistDao = AppDatabase.getInstance(this.applicationContext).playlistDao();
        return playlistDao.loadPlaylists();
    }

    public void setServer(Host host, AppCompatActivity activity) {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContext).serverDao();
        SongDao songDao = AppDatabase.getInstance(this.applicationContext).songDao();
        PlaylistDao playlistDao = AppDatabase.getInstance(this.applicationContext).playlistDao();
        serverDao.setDaapServer(new ServerEntity(host.getAddress(), host.getPassword()));
        songDao.setSongs(host.fetchSongs());
        List<PlaylistEntity> playlists = host.fetchPlaylists();

        PlaylistEntity allSongsPlaylist = new PlaylistEntity();
        allSongsPlaylist.setId(-1);
        allSongsPlaylist.setIsSongsRetrieved(false);
        allSongsPlaylist.setName(activity.getString(R.string.all_songs));
        playlists.add(0, allSongsPlaylist);
        playlistDao.setPlaylists(playlists);
    }

    public PlaylistEntity fetchSinglePlaylist(Host host, int playlistId) {
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
        PlaylistEntity playlistEntity = playlistDao.loadPlaylist(playlistId);
        if (playlistEntity.getIsSongsRetrieved()) {
            return playlistEntity;
        } else {
            List<Integer> songIds = host.fetchSongIdsForPlaylist(host, playlistId);
            List<PlaylistSongEntity> playlistSongEntities = new ArrayList<>();
            for (Integer songId : songIds) {
                playlistSongEntities.add(new PlaylistSongEntity(playlistId, songId));
            }
            playlistEntity.setIsSongsRetrieved(true);
            playlistDao.setPlaylist(playlistEntity);
            playlistDao.setSongsForPlaylist(playlistSongEntities);
            return playlistEntity;
        }
    }

    /**
     * Retrieves the list of artists, optionally filtered to a playlist
     *
     * @param playlistId The playlist identifier to filter the artists to, -1 to retrieve all artists
     * @return The list of artists
     */
    public List<ArtistEntity> getArtists(int playlistId) {
        if (playlistId == -1) {
            SongDao songDao = AppDatabase.getInstance(applicationContext).songDao();
            return songDao.loadArtists();
        } else {
            PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
            return playlistDao.loadArtistsForPlaylist(playlistId);
        }
    }

    /**
     * Retrieves the list of albums, optionally filtered to a playlist
     *
     * @param playlistId The playlist identifier to filter the albums to, -1 to retrieve all albums
     * @return The list of albums
     */
    public List<AlbumEntity> getAlbums(int playlistId) {
        if (playlistId == -1) {
            SongDao songDao = AppDatabase.getInstance(applicationContext).songDao();
            return songDao.loadAlbums();
        } else {
            PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
            return playlistDao.loadAlbumsForPlaylist(playlistId);
        }
    }

    public List<SongEntity> getSongsForPlaylist(int playlistId, String artistFilter, String albumFilter) {
        if (playlistId == -1) {
            SongDao songDao = AppDatabase.getInstance(applicationContext).songDao();
            if (artistFilter != null) {
                return songDao.loadArtistSongs(artistFilter);
            } else if (albumFilter != null) {
                return songDao.loadAlbumSongs(albumFilter);
            } else {
                return songDao.loadSongs();
            }
        } else {
            PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
            if (artistFilter != null) {
                return playlistDao.loadArtistSongsForPlaylist(playlistId, artistFilter);
            } else if (albumFilter != null) {
                return playlistDao.loadAlbumSongsForPlaylist(playlistId, albumFilter);
            } else {
                return playlistDao.loadSongsForPlaylist(playlistId);
            }
        }
    }

    public void addSongToTopOfQueueAsync(SongEntity songEntity, IQueueWorker queueWorker) {
        new SongQueueAdder(this.applicationContext, queueWorker, songEntity).execute();
    }

    private static class SongQueueAdder extends AsyncTask<Void, Void, SongEntity> {
        private final WeakReference<Context> contextWeakReference;
        private final WeakReference<IQueueWorker> queueWorkerWeakReference;
        private final SongEntity songEntity;

        SongQueueAdder(Context applicationContext, IQueueWorker queueWorker, SongEntity songEntity) {
            this.contextWeakReference = new WeakReference<>(applicationContext);
            this.queueWorkerWeakReference = new WeakReference<>(queueWorker);
            this.songEntity = songEntity;
        }

        @Override
        protected SongEntity doInBackground(Void... voids) {
            Context applicationContext = this.contextWeakReference.get();
            if (null != applicationContext) {
                QueueDao queueDao = AppDatabase.getInstance(applicationContext).queueDao();
                queueDao.add(new QueueEntity(this.songEntity.id));
                return this.songEntity;
            }

            return null;
        }

        @Override
        protected void onPostExecute(SongEntity songEntity) {
            IQueueWorker queueWorker = this.queueWorkerWeakReference.get();
            if (null != queueWorker && null != songEntity) {
                queueWorker.songAddedToTopOfQueue(songEntity);
            }
        }
    }
}
