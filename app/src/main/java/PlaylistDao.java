package org.mult.daap.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import org.mult.daap.db.entity.PlaylistEntity;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    PlaylistEntity[] loadPlaylists();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setPlaylists(List<PlaylistEntity> songs);
}
