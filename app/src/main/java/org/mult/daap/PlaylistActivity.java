package org.mult.daap;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.PlaylistEntity;
import org.mult.daap.lists.PlaylistListAdapter;
import org.mult.daap.lists.PlaylistListItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;

public class PlaylistActivity extends AppCompatActivity implements FlexibleAdapter.OnItemClickListener {
    private PlaylistListAdapter<PlaylistListItem> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_playlist);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        new GetPlaylistsAsyncTask(this).execute();
    }

    @Override
    public boolean onItemClick(View view, int position) {
        PlaylistListItem listItem = this.mAdapter.getItem(position);
        new PlaylistActivity.GetSinglePlaylistAsyncTask(this, Integer.parseInt(listItem.getId())).execute();

        return true;
    }

    private void OnPlaylistRetrieved(List<PlaylistEntity> playlists) {
        List<PlaylistListItem> playlistItems = new ArrayList<>();
        for (PlaylistEntity playlist : playlists) {
            playlistItems.add(new PlaylistListItem(playlist));
        }

        this.mAdapter = new PlaylistListAdapter<>(playlistItems);

        RecyclerView playlistListView = this.findViewById(R.id.playlistList);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(mAdapter);

        FastScroller fastScroller = this.findViewById(R.id.fast_scroller);

        fastScroller.setMinimumScrollThreshold(70);
        fastScroller.setBubbleAndHandleColor(Color.parseColor("#4DB6AC"));

        this.mAdapter.setFastScroller(fastScroller);
        this.mAdapter.setMode(SelectableAdapter.Mode.SINGLE);
        this.mAdapter.addListener(this);
    }

    private void OnPlaylistLoaded(int playlistId) {
        final Intent intent = new Intent(PlaylistActivity.this, DrawerActivity.class);
        intent.putExtra(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, playlistId);
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