package org.mult.daap.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import org.mult.daap.db.entity.SongEntity;

import java.util.List;

@Dao
public interface SongDao {
    @Query("SELECT * FROM songs")
    SongEntity[] loadSongs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setSongs(List<SongEntity> songs);
}
