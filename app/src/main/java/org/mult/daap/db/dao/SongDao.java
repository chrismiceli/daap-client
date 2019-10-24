package org.mult.daap.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

@Dao
public interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setSongs(List<SongEntity> songs);

    @Query("SELECT songs.* FROM songs WHERE songs.album = :albumFilter ORDER BY songs.album, songs.track, songs.artist")
    List<SongEntity> loadAlbumSongs(String albumFilter);

    @Query("SELECT songs.* FROM songs WHERE songs.artist = :artistFilter ORDER BY songs.artist, songs.album, songs.track")
    List<SongEntity> loadArtistSongs(String artistFilter);

    @Query("SELECT DISTINCT songs.album FROM songs ORDER BY album ASC")
    List<AlbumEntity> loadAlbums();

    @Query("SELECT DISTINCT songs.artist FROM songs ORDER BY album ASC")
    List<ArtistEntity> loadArtists();

    @Query("SELECT songs.* FROM songs ORDER BY songs.artist, songs.album, songs.track")
    List<SongEntity> loadSongs();
}
