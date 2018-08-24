package org.mult.daap;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.PlaylistEntity;

import java.lang.ref.WeakReference;
import java.util.List;

public class PlaylistBrowser extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_playlist_browser);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        new GetPlaylistsAsyncTask(this).execute();
    }

    private static class OnClickListener implements RecyclerOnItemClickListener<PlaylistEntity> {
        private final PlaylistBrowser playlistBrowser;

        OnClickListener(PlaylistBrowser playlistBrowser) {
            this.playlistBrowser = playlistBrowser;
        }

        @Override
        public void onItemClick(PlaylistEntity item) {
            new GetSinglePlaylistAsyncTask(this.playlistBrowser, item.getId()).execute();
        }
    }

    private final RecyclerOnItemClickListener<PlaylistEntity> playlistEntityRecyclerOnItemClickListener = new OnClickListener(this);

    /*
     todo cmiceli
    Toast tst = Toast.makeText(PlaylistBrowser.this, getString(R.string.empty_playlist), Toast.LENGTH_LONG);
    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst.getYOffset() / 2);
    tst.show();
    */

    private void OnPlaylistRetrieved(List<PlaylistEntity> playlists) {
        PlaylistAdapter adapter = new PlaylistAdapter(playlists);
        RecyclerView playlistListView = this.findViewById(R.id.playlistList);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnClickListener(this));
    }

    private void OnPlaylistLoaded(int playlistId) {
        Intent intent = new Intent(PlaylistBrowser.this, TabMain.class);
        intent.putExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, playlistId);
        startActivityForResult(intent, 1);
    }

    private static class GetPlaylistsAsyncTask extends AsyncTask<Void,Void, List<PlaylistEntity>> {
        private final WeakReference<PlaylistBrowser> playlistBrowserWeakReference;

        GetPlaylistsAsyncTask(PlaylistBrowser playlistBrowser) {
            this.playlistBrowserWeakReference = new WeakReference<>(playlistBrowser);
        }

        @Override
        protected List<PlaylistEntity> doInBackground(Void...voids){
            List<PlaylistEntity> result = null;
            PlaylistBrowser playlistBrowser = this.playlistBrowserWeakReference.get();
            if (playlistBrowser != null) {
                DatabaseHost databaseHost = new DatabaseHost(playlistBrowser.getApplicationContext());
                result = databaseHost.getPlaylists();
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<PlaylistEntity> playlists) {
            super.onPostExecute(playlists);

            PlaylistBrowser playlistBrowser = this.playlistBrowserWeakReference.get();
            if (playlistBrowser != null && !playlistBrowser.isFinishing()) {
                playlistBrowser.OnPlaylistRetrieved(playlists);
            }
        }
    }

    private static class GetSinglePlaylistAsyncTask extends AsyncTask<Void,Void, Boolean> {
        private final WeakReference<PlaylistBrowser> playlistBrowserWeakReference;
        private final int playlistId;

        GetSinglePlaylistAsyncTask(PlaylistBrowser playlistBrowser, int playlistId) {
            this.playlistBrowserWeakReference = new WeakReference<>(playlistBrowser);
            this.playlistId = playlistId;
        }

        @Override
        protected Boolean doInBackground(Void...voids){
            PlaylistBrowser playlistBrowser = this.playlistBrowserWeakReference.get();
            if (playlistBrowser != null) {
                DatabaseHost databaseHost = new DatabaseHost(playlistBrowser.getApplicationContext());
                databaseHost.fetchSinglePlaylist(Contents.daapHost, this.playlistId);
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            PlaylistBrowser playlistBrowser = this.playlistBrowserWeakReference.get();
            if (playlistBrowser != null && !playlistBrowser.isFinishing()) {
                playlistBrowser.OnPlaylistLoaded(this.playlistId);
            }
        }
    }
}