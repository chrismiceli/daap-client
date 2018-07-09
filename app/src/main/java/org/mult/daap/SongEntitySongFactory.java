package org.mult.daap;

import org.mult.daap.client.ISong;
import org.mult.daap.client.ISongFactory;
import org.mult.daap.db.entity.SongEntity;

public class SongEntitySongFactory implements ISongFactory {

    @Override
    public ISong createSong(
        int id,
        String name,
        int time,
        String album,
        String artist,
        short track,
        short discNum,
        String format,
        int size) {
        return new SongEntity(id, name, time, album, artist, track, discNum, format, size);
    }
}
