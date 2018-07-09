package org.mult.daap.client;

public interface ISongFactory {
    ISong createSong(
            int id,
            String name,
            int time,
            String album,
            String artist,
            short track,
            short discNum,
            String format,
            int size);
}
