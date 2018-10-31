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

public class PlaylistActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_playlist);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        new GetPlaylistsAsyncTask(this).execute();
    }

    private static class OnClickListener implements RecyclerOnItemClickListener<PlaylistEntity> {
        private final PlaylistActivity playlistActivity;

        OnClickListener(PlaylistActivity playlistActivity) {
            this.playlistActivity = playlistActivity;
        }

        @Override
        public void onItemClick(PlaylistEntity item) {
            new PlaylistActivity.GetSinglePlaylistAsyncTask(this.playlistActivity, item.getId()).execute();
        }
    }

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
        final Intent intent = new Intent(PlaylistActivity.this, SongsDrawerActivity.class);
        intent.putExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, playlistId);
        startActivityForResult(intent, 1);
    }

    private static class GetPlaylistsAsyncTask extends AsyncTask<Void, Void, List<PlaylistEntity>> {
        private final WeakReference<PlaylistActivity> playlistActivityWeakReference;

        GetPlaylistsAsyncTask(PlaylistActivity playlistActivity) {
            this.playlistActivityWeakReference = new WeakReference<>(playlistActivity);
        }

        @Override
        protected List<PlaylistEntity> doInBackground(Void... voids) {
            List<PlaylistEntity> result = null;
            PlaylistActivity playlistActivity = this.playlistActivityWeakReference.get();
            if (playlistActivity != null) {
                DatabaseHost databaseHost = new DatabaseHost(playlistActivity.getApplicationContext());
                result = databaseHost.getPlaylists();
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<PlaylistEntity> playlists) {
            super.onPostExecute(playlists);

            PlaylistActivity playlistActivity = this.playlistActivityWeakReference.get();
            if (playlistActivity != null && !playlistActivity.isFinishing()) {
                playlistActivity.OnPlaylistRetrieved(playlists);
            }
        }
    }

    private static class GetSinglePlaylistAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<PlaylistActivity> playlistActivityWeakReference;
        private final int playlistId;

        GetSinglePlaylistAsyncTask(PlaylistActivity songsDrawerActivity, int playlistId) {
            this.playlistActivityWeakReference = new WeakReference<>(songsDrawerActivity);
            this.playlistId = playlistId;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PlaylistActivity playlistActivity = this.playlistActivityWeakReference.get();

            // don't load any playlist songs for 'All Songs' playlist
            if (playlistActivity != null && playlistId != -1) {
                DatabaseHost databaseHost = new DatabaseHost(playlistActivity.getApplicationContext());
                databaseHost.fetchSinglePlaylist(Contents.daapHost, this.playlistId);
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            PlaylistActivity playlistActivity = this.playlistActivityWeakReference.get();
            if (playlistActivity != null && !playlistActivity.isFinishing()) {
                playlistActivity.OnPlaylistLoaded(this.playlistId);
            }
        }
    }
}
