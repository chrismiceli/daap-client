package org.mult.daap.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import org.mult.daap.db.entity.QueueEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

@Dao
public interface QueueDao {
    @Query("SELECT songs.* FROM queue, songs WHERE songs.id = queue.songId ORDER BY queue.entryId DESC")
    List<SongEntity> loadQueue();

    @Insert
    void add(QueueEntity queueEntity);

    @Query("SELECT songs.* FROM queue, songs WHERE songs.id = queue.songId ORDER BY queue.entryId DESC LIMIT 1")
    SongEntity element();

    @Delete
    void remove(QueueEntity queueEntity);
}
