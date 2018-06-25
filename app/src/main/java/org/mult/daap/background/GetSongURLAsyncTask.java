package org.mult.daap.background;

import android.os.AsyncTask;

import org.mult.daap.MediaPlayback;
import org.mult.daap.client.Host;
import org.mult.daap.client.ISongUrlConsumer;
import org.mult.daap.client.Song;
import org.mult.daap.client.daap.exception.BadResponseCodeException;
import org.mult.daap.client.daap.exception.PasswordFailedException;
import org.mult.daap.client.daap.request.SongRequest;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;

public class GetSongURLAsyncTask extends AsyncTask<Void,Void, String> {
    Host host;
    Song song;
    WeakReference<ISongUrlConsumer> songUrlConsumerWeakReference;

    public GetSongURLAsyncTask(Host host, Song song, ISongUrlConsumer songUrlConsumer) {
        this.host = host;
        this.song = song;
        this.songUrlConsumerWeakReference = new WeakReference<>(songUrlConsumer);
    }

    @Override
    protected String doInBackground(Void... voids) {
        SongRequest sr = new SongRequest(this.host, this.song);
        String result = null;

        try {
            sr.Execute();
        } catch (BadResponseCodeException e) {
        } catch (PasswordFailedException e) {
        } catch (IOException e) {
        }

        try {
            result = sr.getSongURL().toString();
        } catch (MalformedURLException e) {
        }

        return result;
    }

    @Override
    protected void onPostExecute(String songUrl) {
        ISongUrlConsumer songUrlConsumer = this.songUrlConsumerWeakReference.get();
        if (null != songUrlConsumer) {
            songUrlConsumer.onSongUrlRetrieved(songUrl);
        }
    }
}
