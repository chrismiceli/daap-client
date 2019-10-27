package org.mult.daap;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.client.IQueueWorker;
import org.mult.daap.db.entity.SongEntity;
import org.mult.daap.lists.QueueListAdapter;
import org.mult.daap.lists.QueueListItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;

public class QueueFragment extends BaseFragment implements IQueueWorker, FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    private QueueListAdapter<QueueListItem> mAdapter;
    private static final int CONTEXT_QUEUE = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        new QueueFragment.GetQueueAsyncTask(this).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        QueueListItem listItem = this.mAdapter.getItem(position);
        MediaPlaybackActivity.clearState();

        // TODO don't use contents
        Contents.song = listItem.getSong();

        DatabaseHost host = new DatabaseHost(this.getContext());
        host.pushSongTopOfQueue(listItem.getSong(), this);

        return true;
    }

    @Override
    public void onItemLongClick(int position) {
        // purposefully empty, the adapter's view holder intercept's the long click
    }

    private void OnSongsReceived(List<SongEntity> songs) {
        List<QueueListItem> songItems = new ArrayList<>();
        for (SongEntity song : songs) {
            songItems.add(new QueueListItem(song, this));
        }

        this.mAdapter = new QueueListAdapter<>(songItems);

        RecyclerView songListView = this.getActivity().findViewById(R.id.music_list);
        songListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        songListView.setLayoutManager(layoutManager);
        songListView.setAdapter(mAdapter);

        FastScroller fastScroller = this.getView().findViewById(R.id.fast_scroller);

        fastScroller.setMinimumScrollThreshold(70);
        fastScroller.setBubbleAndHandleColor(Color.parseColor("#4DB6AC"));

        this.mAdapter.setFastScroller(fastScroller);
        this.mAdapter.setMode(SelectableAdapter.Mode.SINGLE);
        this.mAdapter.addListener(this);

        RecyclerView musicList = this.getActivity().findViewById(R.id.music_list);
        musicList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenu.ContextMenuInfo menuInfo) {
                menu.setHeaderTitle(getString(R.string.options));
                menu.add(0, CONTEXT_QUEUE, 0,
                        "Remove From Queue");
            }
        });
    }

    public void songsAddedToQueue(List<SongEntity> songs) {
        Intent intent = new Intent(getContext(), MediaPlaybackActivity.class);
        startActivityForResult(intent, 1);
    }

    public void songsRemovedFromQueue(List<SongEntity> songs) {
        // should not be able to remove/toggle a song from the fragment itself,
        // if toggled via long press on list item, then song will be removed in the
        // ListItem's ViewHolder
    }

    private static class GetQueueAsyncTask extends AsyncTask<Void, Void, List<SongEntity>> {
        private final WeakReference<QueueFragment> queueFragmentWeakReference;

        GetQueueAsyncTask(QueueFragment songsFragment) {
            this.queueFragmentWeakReference = new WeakReference<>(songsFragment);
        }

        @Override
        protected List<SongEntity> doInBackground(Void... voids) {
            List<SongEntity> result = null;
            QueueFragment songsFragment = this.queueFragmentWeakReference.get();
            if (songsFragment != null && !songsFragment.isRemoving()) {
                DatabaseHost databaseHost = new DatabaseHost(songsFragment.getContext().getApplicationContext());
                result = databaseHost.getQueue();
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<SongEntity> songs) {
            super.onPostExecute(songs);

            QueueFragment songsFragment = this.queueFragmentWeakReference.get();
            if (songsFragment != null && !songsFragment.isRemoving()) {
                songsFragment.OnSongsReceived(songs);
            }
        }
    }
}
