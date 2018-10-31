package org.mult.daap.client;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import org.mult.daap.R;
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

    public void setServer(Host host, AppCompatActivity activity) {
        ServerDao serverDao = AppDatabase.getInstance(this.applicationContxt).serverDao();
        SongDao songDao = AppDatabase.getInstance(applicationContxt).songDao();
        PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
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
        if(playlistId == -1) {
            SongDao songDao = AppDatabase.getInstance(applicationContxt).songDao();
            return songDao.loadArtists();
        } else {
            PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
            return playlistDao.loadArtistsForPlaylist(playlistId);
        }
    }

    public List<AlbumEntity> getAlbumsForPlaylist(int playlistId) {
        if(playlistId == -1) {
            SongDao songDao = AppDatabase.getInstance(applicationContxt).songDao();
            return songDao.loadAlbums();
        } else {
            PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
            return playlistDao.loadAlbumsForPlaylist(playlistId);
        }
    }

    public List<SongEntity> getSongsForPlaylist(int playlistId, String artistFilter, String albumFilter) {
        if(playlistId == -1) {
            SongDao songDao = AppDatabase.getInstance(applicationContxt).songDao();
            if (artistFilter != null) {
                return songDao.loadArtistSongs(artistFilter);
            } else if (albumFilter != null) {
                return songDao.loadAlbumSongs(albumFilter);
            } else {
                return songDao.loadSongs();
            }
        } else {
            PlaylistDao playlistDao = AppDatabase.getInstance(applicationContxt).playlistDao();
            if (artistFilter != null) {
                return playlistDao.loadArtistSongsForPlaylist(playlistId, artistFilter);
            } else if (albumFilter != null) {
                return playlistDao.loadAlbumSongsForPlaylist(playlistId, albumFilter);
            } else {
                return playlistDao.loadSongsForPlaylist(playlistId);
            }
        }
    }
}
