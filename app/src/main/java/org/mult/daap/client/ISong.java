package org.mult.daap.client;

public interface ISong {
    String getName();

    int getId();

    int getTime();

    String getAlbum();

    String getArtist();

    short getTrack();

    short getDiscNum();

    String getFormat();

    int getSize();

    @Override
    String toString();

    /*
    public int compareTo(@NonNull Object another) {
        if (another instanceof Song) {
            return (this.id - ((Song) another).id);
        } else if (another instanceof Integer) {
            return (this.id - (Integer) another);
        }

        return 0; // all the same if can't compare
    }*/
}