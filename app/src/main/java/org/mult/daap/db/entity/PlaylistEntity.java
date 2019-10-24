package org.mult.daap.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey
    @NonNull
    private int id;

    private String name;

    private boolean isSongsRetrieved;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean getIsSongsRetrieved() {
        return this.isSongsRetrieved;
    }

    public void setIsSongsRetrieved(boolean isSongsRetrieved) {
        this.isSongsRetrieved = isSongsRetrieved;
    }
}