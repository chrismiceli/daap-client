package org.mult.daap;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import org.mult.daap.mediaplayback.MediaPlaybackActivity;
import org.mult.daap.client.DatabaseHost;
import org.mult.daap.client.IQueueWorker;
import org.mult.daap.db.entity.SongEntity;
import org.mult.daap.lists.SongListAdapter;
import org.mult.daap.lists.SongListItem;

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

public class SearchActivity extends AppCompatActivity implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, IQueueWorker {
    private SongListAdapter<SongListItem> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_search_results);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        final Intent queryIntent = getIntent();
        final String queryAction = queryIntent.getAction();

        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            String searchKeywords = queryIntent.getStringExtra(SearchManager.QUERY);
            new SearchActivity.GetSearchResultsAsyncTask(this, searchKeywords).execute();
        }
    }

    @Override
    public void onItemLongClick(int position) {
        // purposefully empty, the adapter's view holder intercept's the long click
    }

    private void OnSongsReceived(List<SongEntity> songs) {
        List<SongListItem> songItems = new ArrayList<>();
        for (SongEntity song : songs) {
            songItems.add(new SongListItem(song));
        }

        this.mAdapter = new SongListAdapter<>(songItems);

        RecyclerView songListView = this.findViewById(R.id.search_results_list);
        songListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        songListView.setLayoutManager(layoutManager);
        songListView.setAdapter(mAdapter);

        FastScroller fastScroller = this.findViewById(R.id.fast_scroller);

        fastScroller.setMinimumScrollThreshold(70);
        fastScroller.setBubbleAndHandleColor(Color.parseColor("#4DB6AC"));

        this.mAdapter.setFastScroller(fastScroller);
        this.mAdapter.setMode(SelectableAdapter.Mode.SINGLE);
        this.mAdapter.addListener(this);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        SongListItem listItem = this.mAdapter.getItem(position);
        MediaPlaybackActivity.clearState();

        DatabaseHost host = new DatabaseHost(this);
        host.pushSongTopOfQueue(listItem.getSong(), this);

        return true;
    }

    public void songsAddedToQueue(List<SongEntity> songs) {
        Intent intent = new Intent(this.getApplicationContext(), MediaPlaybackActivity.class);
        startActivityForResult(intent, 1);
    }

    public void songsRemovedFromQueue() {
        // should not be able to remove/toggle a song from the fragment itself,
        // if toggled via long press on list item, then song will be removed in the
        // ListItem's ViewHolder
    }

    private static class GetSearchResultsAsyncTask extends AsyncTask<Void, Void, List<SongEntity>> {
        private final WeakReference<SearchActivity> searchActivityWeakReference;
        private final String searchFilter;

        GetSearchResultsAsyncTask(SearchActivity songsActivity, String searchFilter) {
            this.searchActivityWeakReference = new WeakReference<>(songsActivity);
            this.searchFilter = searchFilter;
        }

        @Override
        protected List<SongEntity> doInBackground(Void... voids) {
            List<SongEntity> result = null;
            SearchActivity searchActivity = this.searchActivityWeakReference.get();
            if (searchActivity != null) {
                DatabaseHost databaseHost = new DatabaseHost(searchActivity.getApplicationContext());
                result = databaseHost.getMatchingSongs(this.searchFilter);
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<SongEntity> songs) {
            super.onPostExecute(songs);

            SearchActivity searchActivity = this.searchActivityWeakReference.get();
            if (searchActivity != null) {
                searchActivity.OnSongsReceived(songs);
            }
        }
    }
}