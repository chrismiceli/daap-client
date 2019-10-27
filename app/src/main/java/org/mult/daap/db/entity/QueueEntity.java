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

    @NonNull
    public final int queueOrder;

    public QueueEntity(int songId, int queueOrder) {
        this.songId = songId;
        this.queueOrder = queueOrder;
    }
}
