package org.mult.daap;

import android.app.NotificationManager;
import android.content.Context;
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
import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ItemsFragment extends Fragment {
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    private static final int CONTEXT_PLAY_ALBUM = 4;
    private static final int CONTEXT_QUEUE = 5;
    public static String ARTIST_FILTER_KEY = "__ARTIST_FILTER_KEY__";
    public static String ALBUM_FILTER_KEY = "__ALBUM_FILTER_KEY__";

    public static final String ITEM_MODE_KEY = "__ITEM_MODE__";
    public static final int ITEM_MODE_ALBUM = 0;
    public static final int ITEM_MODE_ARTIST = 1;

    public int itemMode;
    public int playlistId;

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

        this.playlistId = getArguments().getInt(TabMain.PLAYLIST_ID_BUNDLE_KEY);
        this.itemMode = getArguments().getInt(ItemsFragment.ITEM_MODE_KEY);

        new GetItemsAsyncTask(this, this.playlistId, this.itemMode).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CONTEXT_PLAY_ALBUM:
//                Intent intent = new Intent(getActivity(), MediaPlayback.class);
//                String albName = Contents.albumNameList.get(menuInfo.position);
//                if (albName.equals(getString(R.string.no_album_name))) {
//                    albName = "";
//                }
//
//                Contents.filteredAlbumSongList.clear();
//                for (SongEntity s : Contents.songList) {
//                    if (s.album.equals(albName)) {
//                        Contents.filteredAlbumSongList.add(s);
//                    }
//                }
//                TreeMap<Short, Short> track_num = new TreeMap<>();
//                for (SongEntity s : Contents.filteredAlbumSongList) {
//                    if (!track_num.keySet().contains(s.discNum)) {
//                        track_num.put(s.discNum, (short) 1);
//                    }
//                    else {
//                        track_num.put(s.discNum, (short) (track_num.get(s.discNum) + 1));
//                    }
//                }
//                Comparator<SongEntity> sdnc = new SongDiscNumComparator();
//                Comparator<SongEntity> stnc = new SongTrackComparator();
//                Collections.sort(Contents.filteredAlbumSongList, sdnc);
//                // sorted by disc number now, but not within the disc
//                int pos = 0;
//                Short max_num_track;
//                for (Map.Entry<Short, Short> entry : track_num.entrySet()) {
//                    max_num_track = entry.getValue();
//                    Collections.sort(Contents.filteredAlbumSongList.subList(pos,
//                            (int) max_num_track + pos), stnc);
//                    pos += max_num_track;
//                }
//                Contents.setSongPosition(Contents.filteredAlbumSongList, 0);
//                MediaPlayback.clearState();
//                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                notificationManager.cancelAll();
//                startActivityForResult(intent, 1);
        }

        return false;
    }

    private class OnClickListener implements RecyclerOnItemClickListener<String> {
        private final ItemsFragment itemsFragment;

        OnClickListener(ItemsFragment itemsFragment) {
            this.itemsFragment = itemsFragment;
        }

        @Override
        public void onItemClick(String item) {
            Intent intent = new Intent(getActivity(), TabMain.class);
            intent.putExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, playlistId);

            if (ItemsFragment.ITEM_MODE_ALBUM == itemMode) {
                intent.putExtra(ItemsFragment.ALBUM_FILTER_KEY, item);
            }

            startActivityForResult(intent, 1);
        }
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
        Intent intent;
        switch (item.getItemId()) {
            case MENU_SEARCH:
                ((SongsDrawerActivity) getActivity()).getSearchRequestedCallback().onSearchRequested();
                return true;
            case MENU_PLAY_QUEUE:
                Contents.setSongPosition(Contents.queue, 0);
                MediaPlayback.clearState();
                NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
                intent = new Intent(getActivity(), MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(getActivity(), QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }

    private void OnItemsReceived(int playlistId, List<String> items) {
        ItemAdapter adapter = new ItemAdapter(items);
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
                menu.add(0, ItemsFragment.CONTEXT_QUEUE, 0,
                        R.string.add_or_remove_from_queue);
            }
        });
    }

    private static class GetItemsAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<ItemsFragment> itemBrowserWeakReference;
        private final int playlistId;
        private final int itemMode;

        GetItemsAsyncTask(ItemsFragment itemBrowser, int playlistId, int itemMode) {
            this.itemBrowserWeakReference = new WeakReference<>(itemBrowser);
            this.playlistId = playlistId;
            this.itemMode = itemMode;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> result = new ArrayList<>();
            ItemsFragment itemsFragment = this.itemBrowserWeakReference.get();
            if (itemsFragment != null && !itemsFragment.isRemoving()) {
                DatabaseHost databaseHost = new DatabaseHost(itemsFragment.getContext().getApplicationContext());
                if (ItemsFragment.ITEM_MODE_ALBUM == this.itemMode) {
                    List<AlbumEntity> albums = databaseHost.getAlbumsForPlaylist(this.playlistId);
                    for (AlbumEntity album : albums) {
                        result.add(album.album);
                    }
                } else if (ItemsFragment.ITEM_MODE_ARTIST == this.itemMode) {
                    List<ArtistEntity> artists = databaseHost.getArtistsForPlaylist(this.playlistId);
                    for (ArtistEntity artist : artists) {
                        result.add(artist.artist);
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<String> items) {
            super.onPostExecute(items);

            ItemsFragment itemBrowser = this.itemBrowserWeakReference.get();
            if (itemBrowser != null && !itemBrowser.isRemoving()) {
                itemBrowser.OnItemsReceived(this.playlistId, items);
            }
        }
    }
}