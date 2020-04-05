package org.mult.daap.client;

import android.content.Context;

import org.mult.daap.R;
import org.mult.daap.client.daap.SongQueueManagementTask;
import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.HistoryDao;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.QueueDao;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.HistoryEntity;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.PlaylistSongEntity;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class DatabaseHost {
    private final Context applicationContext;

    public DatabaseHost(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ServerEntity getServer() {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContext).serverDao();
        return serverDao.loadServer();
    }

    public void clearServer() {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContext).serverDao();
        SongDao songDao = AppDatabase.getInstance(this.applicationContext).songDao();
        PlaylistDao playlistDao = AppDatabase.getInstance(this.applicationContext).playlistDao();
        HistoryDao historyDao = AppDatabase.getInstance(this.applicationContext).historyDao();
        QueueDao queueDao = AppDatabase.getInstance(this.applicationContext).queueDao();
        historyDao.clearHistory();
        queueDao.clearQueue();
        playlistDao.clearPlaylistSongs();
        playlistDao.clearPlaylists();
        songDao.clearSongs();
        serverDao.clearDaapServer();
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

    public void fetchSinglePlaylist(Host host, int playlistId) {
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContext).playlistDao();
        PlaylistEntity playlistEntity = playlistDao.loadPlaylist(playlistId);
        if (!playlistEntity.getIsSongsRetrieved()) {
            List<Integer> songIds = host.fetchSongIdsForPlaylist(host, playlistId);
            List<PlaylistSongEntity> playlistSongEntities = new ArrayList<>();
            for (Integer songId : songIds) {
                playlistSongEntities.add(new PlaylistSongEntity(playlistId, songId));
            }
            playlistEntity.setIsSongsRetrieved(true);
            playlistDao.setPlaylist(playlistEntity);
            playlistDao.setSongsForPlaylist(playlistSongEntities);
        }
    }


    /**
     * Retrieves the list of songs from the queue in order
     * @return A list of songs
     */
    public List<SongEntity> getQueue() {
        QueueDao queueDao = AppDatabase.getInstance(this.applicationContext).queueDao();
        return queueDao.loadQueue();
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

    public List<SongEntity> getMatchingSongs(String searchFilter) {
        SongDao songDao = AppDatabase.getInstance(applicationContext).songDao();
        searchFilter = "%" + searchFilter + "%";
        return songDao.loadMatchingSongs(searchFilter);
    }

    public void toggleSongInQueue(SongEntity songEntity, IQueueWorker queueWorker) {
        new SongQueueManagementTask(this.applicationContext, queueWorker, songEntity, SongQueueManagementTask.SongQueueCommand.TOGGLE).execute();
    }

    public void queueAlbum(String albumName, int playlistId, IQueueWorker queueWorker) {
        new SongQueueManagementTask(this.applicationContext, queueWorker, playlistId, albumName, SongQueueManagementTask.ObjectTypeToQueue.ALBUM).execute();
    }

    public void queueArtist(String artistName, int playlistId, IQueueWorker queueWorker) {
        new SongQueueManagementTask(this.applicationContext, queueWorker, playlistId, artistName, SongQueueManagementTask.ObjectTypeToQueue.ARTIST).execute();
    }

    public void pushSongTopOfQueue(SongEntity songEntity, IQueueWorker queueWorker) {
        new SongQueueManagementTask(this.applicationContext, queueWorker, songEntity, SongQueueManagementTask.SongQueueCommand.UNSHIFT).execute();
    }

    public void addPreviousSong(SongEntity songEntity) {
        HistoryDao historyDao = AppDatabase.getInstance(applicationContext).historyDao();
        historyDao.addEntry(new HistoryEntity(songEntity.id));
        historyDao.removeOldEntries();
    }

    public SongEntity getPreviousSong() {
        AppDatabase appDatabase = AppDatabase.getInstance(applicationContext);
        HistoryDao historyDao = appDatabase.historyDao();
        HistoryEntity historyEntity = historyDao.getPreviousEntry();
        if (historyEntity != null) {
            historyDao.deleteEntry(historyEntity);
            SongDao songDao = appDatabase.songDao();
            return songDao.loadSongById(historyEntity.songId);
        }

        return null;
    }
}
