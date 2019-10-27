package org.mult.daap.db.dao;

import org.mult.daap.db.entity.QueueEntity;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface QueueDao {
    @Query("SELECT songs.* FROM queue, songs WHERE songs.id = queue.songId ORDER BY queue.queueOrder ASC")
    List<SongEntity> loadQueue();

    @Query("SELECT queue.* FROM queue WHERE queue.songId = :songId ORDER BY queue.queueOrder DESC LIMIT 1")
    QueueEntity getSongQueueId(int songId);

    @Query("SELECT queue.queueOrder FROM queue ORDER BY queue.queueOrder ASC LIMIT 1")
    int getOrderForFrontOfQueue();

    @Query("SELECT queue.queueOrder FROM queue ORDER BY queue.queueOrder DESC LIMIT 1")
    int getOrderForBackOfQueue();

    @Insert
    void add(QueueEntity queueEntity);

    @Delete
    void remove(QueueEntity queueEntity);
}
