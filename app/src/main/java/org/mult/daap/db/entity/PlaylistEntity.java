package org.mult.daap.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Relation;
import android.support.annotation.NonNull;


import java.util.ArrayList;
import java.util.List;

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