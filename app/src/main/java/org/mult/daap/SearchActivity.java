package org.mult.daap;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import org.mult.daap.client.DatabaseHost;
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

public class SearchActivity extends AppCompatActivity implements FlexibleAdapter.OnItemClickListener {
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

    public void songAddedToTopOfQueue(SongEntity songEntity) {
        Intent intent = new Intent(this, MediaPlaybackActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        return false;
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

//    @Override
//    public boolean onContextItemSelected(MenuItem aItem) {
//        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
//                .getMenuInfo();
//        if (aItem.getItemId() == CONTEXT_QUEUE) {
//            SongEntity s = Contents.songList.get(Contents.songList.indexOf(srList
//                    .get(menuInfo.position)));
//            if (Contents.queue.contains(s)) { // in
//                // list
//                Contents.queue.remove(s);
//                Toast tst = Toast.makeText(SearchActivity.this,
//                        getString(R.string.removed_from_queue),
//                        Toast.LENGTH_SHORT);
//                tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
//                        tst.getYOffset() / 2);
//                tst.show();
//                return true;
//            } else {
//                if (Contents.queue.size() < 9) {
//                    Contents.addToQueue(s);
//                    Toast tst = Toast.makeText(SearchActivity.this,
//                            getString(R.string.added_to_queue),
//                            Toast.LENGTH_SHORT);
//                    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
//                            tst.getYOffset() / 2);
//                    tst.show();
//                } else {
//                    Toast tst = Toast.makeText(SearchActivity.this,
//                            getString(R.string.queue_is_full),
//                            Toast.LENGTH_SHORT);
//                    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
//                            tst.getYOffset() / 2);
//                    tst.show();
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(0, MENU_PLAY_QUEUE, 0, getString(R.string.play_queue))
//                .setIcon(R.drawable.ic_menu_play);
//        menu.add(0, MENU_VIEW_QUEUE, 0, getString(R.string.view_queue))
//                .setIcon(R.drawable.ic_menu_list);
//        return true;
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        super.onPrepareOptionsMenu(menu);
//        if (Contents.queue.size() == 0) {
//            menu.findItem(MENU_PLAY_QUEUE).setEnabled(false);
//            menu.findItem(MENU_VIEW_QUEUE).setEnabled(false);
//        }
//        else {
//            menu.findItem(MENU_PLAY_QUEUE).setEnabled(true);
//            menu.findItem(MENU_VIEW_QUEUE).setEnabled(true);
//        }
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Intent intent;
//        switch (item.getItemId()) {
//            case MENU_PLAY_QUEUE:
//                Contents.setSongPosition(Contents.queue, 0);
//                MediaPlaybackActivity.clearState();
//                intent = new Intent(SearchActivity.this, MediaPlaybackActivity.class);
//                startActivityForResult(intent, 1);
//                return true;
//            case MENU_VIEW_QUEUE:
//                intent = new Intent(SearchActivity.this, QueueListBrowser.class);
//                startActivityForResult(intent, 1);
//        }
//        return false;
//    }
}