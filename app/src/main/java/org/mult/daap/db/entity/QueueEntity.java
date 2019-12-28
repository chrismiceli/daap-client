package org.mult.daap.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue")
@SuppressWarnings("WeakerAccess") /* needed for entity */
public class QueueEntity {
    @PrimaryKey(autoGenerate = true)
    public int entryId;

    public final int songId;

    public final int queueOrder;

    public QueueEntity(int songId, int queueOrder) {
        this.songId = songId;
        this.queueOrder = queueOrder;
    }
}
