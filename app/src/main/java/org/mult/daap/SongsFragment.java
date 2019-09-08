package org.mult.daap;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.client.IQueueWorker;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.List;

public class SongsFragment extends Fragment implements IQueueWorker {
    private static final int CONTEXT_QUEUE = 0;
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    public static final String ARTIST_FILTER_KEY = "__ARTIST_FILTER_KEY__";
    public static final String ALBUM_FILTER_KEY = "__ALBUM_FILTER_KEY__";

    @Override
    public void onStart() {
        super.onStart();
        ((SongsDrawerActivity) getActivity()).setSearchRequestedCallback(new SongsDrawerActivity.SearchRequestedCallback() {
            @Override
            public void onSearchRequested() {
                Contents.searchResult = null;
                getActivity().startSearch(null, false, null, false);
            }
        });
    }

    @Override
    public void onStop() {
        ((SongsDrawerActivity) getActivity()).setSearchRequestedCallback(null);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        int playlistId = getArguments().getInt(TabMain.PLAYLIST_ID_BUNDLE_KEY);
        String artistFilter = getArguments().getString(SongsFragment.ARTIST_FILTER_KEY);
        String albumFilter = getArguments().getString(SongsFragment.ALBUM_FILTER_KEY);

        new SongsFragment.GetSongsAsyncTask(this, playlistId, artistFilter, albumFilter).execute();

        return inflater.inflate(R.layout.music_browser, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_SEARCH, 0, getString(R.string.search)).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(0, MENU_PLAY_QUEUE, 0, getString(R.string.play_queue))
                .setIcon(R.drawable.ic_menu_play);
        menu.add(0, MENU_VIEW_QUEUE, 0, getString(R.string.view_queue))
                .setIcon(R.drawable.ic_menu_list);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (Contents.queue.size() == 0) {
            menu.findItem(MENU_PLAY_QUEUE).setEnabled(false);
            menu.findItem(MENU_VIEW_QUEUE).setEnabled(false);
        } else {
            menu.findItem(MENU_PLAY_QUEUE).setEnabled(true);
            menu.findItem(MENU_VIEW_QUEUE).setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SEARCH:
                ((SongsDrawerActivity) getActivity()).getSearchRequestedCallback().onSearchRequested();
                return true;
            case MENU_PLAY_QUEUE:
                Contents.setSongPosition(Contents.queue, 0);
                MediaPlayback.clearState();

                // TODO update fragment
//                intent = new Intent(SongsFragment.this, MediaPlayback.class);
//                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                // TODO update fragment
//                intent = new Intent(SongsFragment.this, QueueListBrowser.class);
//                startActivityForResult(intent, 1);
        }
        return false;
    }

    private void OnSongsReceived(List<SongEntity> songs) {
        SongAdapter adapter = new SongAdapter(songs);
        RecyclerView playlistListView = this.getActivity().findViewById(R.id.music_list);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnClickListener(this));

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

    private class OnClickListener implements RecyclerOnItemClickListener<SongEntity> {
        private final SongsFragment songsFragment;

        OnClickListener(SongsFragment songsFragment) {
            this.songsFragment = songsFragment;
        }

        @Override
        public void onItemClick(SongEntity item) {
            MediaPlayback.clearState();

            // TODO don't use contents
            Contents.song = item;
            DatabaseHost host = new DatabaseHost(this.songsFragment.getContext());
            host.addSongToTopOfQueueAsync(item, this.songsFragment);
        }
    }

    public void songAddedToTopOfQueue(SongEntity songEntity) {
        Intent intent = new Intent(getContext(), MediaPlayback.class);
        startActivityForResult(intent, 1);
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
