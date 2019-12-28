package org.mult.daap.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int entryId;

    public final int songId;

    public HistoryEntity(int songId) {
        this.songId = songId;
    }
}
