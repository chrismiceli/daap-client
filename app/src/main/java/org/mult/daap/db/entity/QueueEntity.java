package org.mult.daap.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "queue")
public class QueueEntity {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int entryId;

    @NonNull
    public final int songId;

    public QueueEntity(int songId) {
        this.songId = songId;
    }
}
