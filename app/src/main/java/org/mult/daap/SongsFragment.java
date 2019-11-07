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
import org.mult.daap.lists.SongListAdapter;
import org.mult.daap.lists.SongListItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;

public class SongsFragment extends BaseFragment implements IQueueWorker, FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    private static final int CONTEXT_QUEUE = 0;
    public static final String ARTIST_FILTER_KEY = "__ARTIST_FILTER_KEY__";
    public static final String ALBUM_FILTER_KEY = "__ALBUM_FILTER_KEY__";
    private SongListAdapter<SongListItem> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        int playlistId = getArguments().getInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY);
        String artistFilter = getArguments().getString(SongsFragment.ARTIST_FILTER_KEY);
        String albumFilter = getArguments().getString(SongsFragment.ALBUM_FILTER_KEY);

        new SongsFragment.GetSongsAsyncTask(this, playlistId, artistFilter, albumFilter).execute();

        return inflater.inflate(R.layout.music_browser, container, false);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        SongListItem listItem = this.mAdapter.getItem(position);
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
        List<SongListItem> songItems = new ArrayList<>();
        for (SongEntity song : songs) {
            songItems.add(new SongListItem(song));
        }

        this.mAdapter = new SongListAdapter<>(songItems);

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
                        R.string.add_or_remove_from_queue);
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

    private static class GetSongsAsyncTask extends AsyncTask<Void, Void, List<SongEntity>> {
        private final WeakReference<SongsFragment> songsFragmentWeakReference;
        private final int playlistId;
        private final String artistFilter;
        private final String albumFilter;

        GetSongsAsyncTask(SongsFragment songsFragment, int playlistId, String artistFilter, String albumFilter) {
            this.songsFragmentWeakReference = new WeakReference<>(songsFragment);
            this.playlistId = playlistId;
            this.artistFilter = artistFilter;
            this.albumFilter = albumFilter;
        }

        @Override
        protected List<SongEntity> doInBackground(Void... voids) {
            List<SongEntity> result = null;
            SongsFragment songsFragment = this.songsFragmentWeakReference.get();
            if (songsFragment != null && !songsFragment.isRemoving()) {
                DatabaseHost databaseHost = new DatabaseHost(songsFragment.getContext().getApplicationContext());
                result = databaseHost.getSongsForPlaylist(this.playlistId, this.artistFilter, this.albumFilter);
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<SongEntity> songs) {
            super.onPostExecute(songs);

            SongsFragment songsFragment = this.songsFragmentWeakReference.get();
            if (songsFragment != null && !songsFragment.isRemoving()) {
                songsFragment.OnSongsReceived(songs);
            }
        }
    }
}
