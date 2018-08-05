package org.mult.daap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import org.mult.daap.db.AppDatabase;
import org.mult.daap.db.dao.PlaylistDao;
import org.mult.daap.db.entity.PlaylistEntity;

import java.lang.ref.WeakReference;

public class PlaylistBrowser extends AppCompatActivity {
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_playlist_browser);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        new GetPlaylistAsyncTask(this).execute();
    }

    public void onDestroy() {
        super.onDestroy();
        if (pd != null) {
            pd.dismiss();
        }
    }

    /**
     * A listener for click events on the playlist items
     */
    private final RecyclerOnItemClickListener<PlaylistEntity> playlistListOnClickListener = new RecyclerOnItemClickListener<PlaylistEntity>() {
        @Override
        public void onItemClick(PlaylistEntity playlist) {
            Intent intent = new Intent(PlaylistBrowser.this, TabMain.class);
            intent.putExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, playlist.getId());
            startActivityForResult(intent, 1);
        }
    };

    /*
     todo cmiceli
    Toast tst = Toast.makeText(PlaylistBrowser.this, getString(R.string.empty_playlist), Toast.LENGTH_LONG);
    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst.getYOffset() / 2);
    tst.show();
    */

    private void OnPlaylistRetrieved(PlaylistEntity[] playlists) {
        PlaylistAdapter adapter = new PlaylistAdapter(playlists);
        RecyclerView playlistListView = this.findViewById(R.id.playlistList);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(playlistListOnClickListener);
    }

    private static class GetPlaylistAsyncTask extends AsyncTask<Void,Void, PlaylistEntity[]> {
        private final WeakReference<PlaylistBrowser> playlistBrowserWeakReference;

        GetPlaylistAsyncTask(PlaylistBrowser playlistBrowser) {
            this.playlistBrowserWeakReference = new WeakReference<>(playlistBrowser);
        }

        @Override
        protected PlaylistEntity[] doInBackground(Void...voids){
            PlaylistEntity[] result = null;
            PlaylistBrowser playlistBrowser = this.playlistBrowserWeakReference.get();
            if (playlistBrowser != null) {
                PlaylistDao playlistDao = AppDatabase.getInstance(playlistBrowser).playlistDao();
                result = playlistDao.loadPlaylists();
            }

            return result;
        }

        @Override
        protected void onPostExecute(PlaylistEntity[] playlists) {
            super.onPostExecute(playlists);

            PlaylistBrowser playlistBrowser = this.playlistBrowserWeakReference.get();
            if (playlistBrowser != null && !playlistBrowser.isFinishing()) {
                playlistBrowser .OnPlaylistRetrieved(playlists);
            }
        }
    }
}