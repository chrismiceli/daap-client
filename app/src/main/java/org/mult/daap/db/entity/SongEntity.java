package org.mult.daap.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
@SuppressWarnings({"WeakerAccess", "CanBeFinal"}) /* needed for entity */
public class SongEntity {
    @PrimaryKey
    public int id;
    public String name;
    public int time;
    public String album;
    public String artist;
    public short track;
    public short discNum;
    public String format;
    public int size;

    public SongEntity(
            int id,
            String name,
            int time,
            String album,
            String artist,
            short track,
            short discNum,
            String format,
            int size) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.album = album;
        this.artist = artist;
        this.track = track;
        this.discNum = discNum;
        this.format = format;
        this.size = size;
    }

    public SongEntity(SongEntity song) {
        this.id = song.id;
        this.name = song.name;
        this.time = song.time;
        this.album = song.album;
        this.artist = song.artist;
        this.track = song.track;
        this.discNum = song.discNum;
        this.format = song.format;
        this.size = song.size;
    }

    @Override
    @NonNull
    public String toString() {
        return artist + (artist.length() > 0 ? " - " : "") + name;
    }
}