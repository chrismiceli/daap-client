package org.mult.daap.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

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
