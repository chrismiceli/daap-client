package org.mult.daap.db.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import org.mult.daap.client.ISong;

@Entity(tableName = "songs")
public class SongEntity implements ISong {
    @PrimaryKey
    private int id;
    private String name;
    private int time;
    private String album;
    private String artist;
    private short track;
    private short discNum;
    private String format;
    private int size;

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

    public SongEntity(ISong song) {
        this.id = song.getId();
        this.name = song.getName();
        this.time = song.getTime();
        this.album = song.getAlbum();
        this.artist = song.getArtist();
        this.track = song.getTrack();
        this.discNum = song.getDiscNum();
        this.format = song.getFormat();
        this.size = song.getSize();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getTime() {
        return this.time;
    }

    @Override
    public String getAlbum() {
        return this.album;
    }

    @Override
    public String getArtist() {
        return this.artist;
    }

    @Override
    public short getTrack() {
        return this.track;
    }

    @Override
    public short getDiscNum() {
        return this.discNum;
    }

    @Override
    public String getFormat() {
        return this.format;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public String toString() {
        return artist + (artist.length() > 0 ? " - " : "") + name;
    }
}