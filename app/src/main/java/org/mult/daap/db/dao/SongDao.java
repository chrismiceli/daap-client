package org.mult.daap.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

@Dao
public interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setSongs(List<SongEntity> songs);

    @Query("SELECT songs.* FROM songs WHERE songs.album = :albumFilter ORDER BY songs.track, songs.name")
    List<SongEntity> loadAlbumSongs(String albumFilter);

    @Query("SELECT songs.* FROM songs WHERE songs.artist = :artistFilter ORDER BY songs.track, songs.name")
    List<SongEntity> loadArtistSongs(String artistFilter);

    @Query("SELECT DISTINCT songs.album FROM songs ORDER BY album ASC")
    List<AlbumEntity> loadAlbums();

    @Query("SELECT DISTINCT songs.artist FROM songs ORDER BY album ASC")
    List<ArtistEntity> loadArtists();

    @Query("SELECT songs.* FROM songs ORDER BY songs.name ASC")
    List<SongEntity> loadSongs();

    @Query("SELECT songs.* FROM songs WHERE songs.id = :songId LIMIT 1")
    SongEntity loadSongById(int songId);

    @Query("SELECT songs.* FROM songs WHERE songs.name LIKE :searchFilter OR songs.artist LIKE :searchFilter OR songs.album LIKE :searchFilter ORDER BY songs.name")
    List<SongEntity> loadMatchingSongs(String searchFilter);

    @Query("DELETE FROM songs")
    void clearSongs();
}
