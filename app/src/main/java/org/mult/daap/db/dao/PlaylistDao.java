package org.mult.daap.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.db.entity.PlaylistSongEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY playlists.name")
    List<PlaylistEntity> loadPlaylists();

    @Query("SELECT * FROM playlists WHERE id = :id")
    PlaylistEntity loadPlaylist(int id);

    @Query("SELECT songs.* FROM playlist_song, songs WHERE playlist_song.playlistId = :playlistId AND songs.id = playlist_song.songId ORDER BY songs.artist, songs.album, songs.track")
    List<SongEntity> loadSongsForPlaylist(int playlistId);

    @Query("SELECT songs.* FROM playlist_song, songs WHERE playlist_song.playlistId = :playlistId AND songs.id = playlist_song.songId AND songs.artist = :albumFilter ORDER BY songs.artist, songs.album, songs.track")
    List<SongEntity> loadAlbumSongsForPlaylist(int playlistId, String albumFilter);

    @Query("SELECT songs.* FROM playlist_song, songs WHERE playlist_song.playlistId = :playlistId AND songs.id = playlist_song.songId AND songs.artist = :artistFilter ORDER BY songs.artist, songs.album, songs.track")
    List<SongEntity> loadArtistSongsForPlaylist(int playlistId, String artistFilter);

    @Query("SELECT DISTINCT songs.artist FROM playlist_song, songs where playlist_song.playlistId = :playlistId AND songs.id = playlist_song.songId ORDER BY artist ASC")
    List<ArtistEntity> loadArtistsForPlaylist(int playlistId);

    @Query("SELECT DISTINCT songs.album FROM playlist_song, songs where playlist_song.playlistId = :playlistId AND songs.id = playlist_song.songId ORDER BY album ASC")
    List<AlbumEntity> loadAlbumsForPlaylist(int playlistId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
        void setPlaylists(List<PlaylistEntity> playlists);

@Insert(onConflict =  OnConflictStrategy.REPLACE)
    void setPlaylist(PlaylistEntity playlist);

@Insert(onConflict =  OnConflictStrategy.REPLACE)
    void setSongsForPlaylist(List<PlaylistSongEntity> playlistSongEntities);
        }
