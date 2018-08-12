package org.mult.daap.client;

import android.content.Context;

import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.dao.ServerDao;
import org.mult.daap.db.dao.SongDao;
import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.PlaylistSongEntity;
import org.mult.daap.db.entity.ServerEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHost {
    private final Context applicationContxt;

    public DatabaseHost(Context applicationContext) {
        this.applicationContxt = applicationContext;
    }

    public ServerEntity getServer() {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContxt).serverDao();
        return serverDao.loadServer();
    }

    public List<PlaylistEntity> getPlaylists() {
        PlaylistDao playlistDao = AppDatabase.getInstance(this.applicationContxt).playlistDao();
        return playlistDao.loadPlaylists();
    }

    public void setServer(Host host) {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContxt).serverDao();
        SongDao songDao = AppDatabase.getInstance(applicationContxt).songDao();
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
        serverDao.setDaapServer(new ServerEntity(host.getAddress(), host.getPassword()));
        songDao.setSongs(host.fetchSongs());
        playlistDao.setPlaylists(host.fetchPlaylists());
    }

    public PlaylistEntity fetchSinglePlaylist(Host host, int playlistId) {
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
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

    public List<ArtistEntity> getArtistsForPlaylist(int playlistId) {
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
        return playlistDao.loadArtistsForPlaylist(playlistId);
    }

    public List<AlbumEntity> getAlbumsForPlaylist(int playlistId) {
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
        return playlistDao.loadAlbumsForPlaylist(playlistId);
    }

    public List<SongEntity> getSongsForPlaylist(int playlistId) {
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
        return playlistDao.loadSongsForPlaylist(playlistId);
    }
}
