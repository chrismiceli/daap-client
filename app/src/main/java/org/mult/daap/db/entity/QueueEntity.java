package org.mult.daap.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

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
