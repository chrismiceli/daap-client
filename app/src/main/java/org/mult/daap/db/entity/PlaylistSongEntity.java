package org.mult.daap.db.entity;

import android.arch.persistence.room.Entity;

@Entity(primaryKeys = {"playlistId", "songId"}, tableName = "playlist_song")
public class PlaylistSongEntity {
    public final int playlistId;

    public final int songId;

    public PlaylistSongEntity(int playlistId, int songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
}
