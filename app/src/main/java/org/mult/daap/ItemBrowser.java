package org.mult.daap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.comparator.SongDiscNumComparator;
import org.mult.daap.comparator.SongTrackComparator;
import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ItemBrowser extends ListActivity {
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    private static final int CONTEXT_PLAY_ALBUM = 4;
    public static String ARTIST_FILTER_KEY = "__ARTIST_FILTER_KEY__";
    public static String ALBUM_FILTER_KEY = "__ALBUM_FILTER_KEY__";

    public static final String ITEM_MODE_KEY = "__ITEM_MODE__";
    public static final int ITEM_MODE_ALBUM = 0;
    public static final int ITEM_MODE_ARTIST = 1;
    public static final int ITEM_MODE_ALBUM_ARTIST = 2;

    private int itemMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_OK);
        setContentView(R.xml.music_browser);
        Bundle bundle = getIntent().getExtras();
        int playlistId = bundle.getInt(TabMain.PLAYLIST_ID_BUNDLE_KEY);
        this.itemMode = bundle.getInt(ItemBrowser.ITEM_MODE_KEY);
        new GetItemsAsyncTask(this, playlistId, this.itemMode).execute();
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        switch (aItem.getItemId()) {
            case CONTEXT_PLAY_ALBUM:
                Intent intent = new Intent(ItemBrowser.this,
                        MediaPlayback.class);
                String albName = Contents.albumNameList.get(menuInfo.position);
                if (albName.equals(getString(R.string.no_album_name))) {
                    albName = "";
                }
                Contents.filteredAlbumSongList.clear();
                for (SongEntity s : Contents.songList) {
                    if (s.album.equals(albName)) {
                        Contents.filteredAlbumSongList.add(s);
                    }
                }
                TreeMap<Short, Short> track_num = new TreeMap<>();
                for (SongEntity s : Contents.filteredAlbumSongList) {
                    if (!track_num.keySet().contains(s.discNum)) {
                        track_num.put(s.discNum, (short) 1);
                    }
                    else {
                        track_num.put(s.discNum, (short) (track_num.get(s.discNum) + 1));
                    }
                }
                Comparator<SongEntity> sdnc = new SongDiscNumComparator();
                Comparator<SongEntity> stnc = new SongTrackComparator();
                Collections.sort(Contents.filteredAlbumSongList, sdnc);
                // sorted by disc number now, but not within the disc
                int pos = 0;
                Short max_num_track;
                for (Map.Entry<Short, Short> entry : track_num.entrySet()) {
                    max_num_track = entry.getValue();
                    Collections.sort(Contents.filteredAlbumSongList.subList(pos,
                            (int) max_num_track + pos), stnc);
                    pos += max_num_track;
                }
                Contents.setSongPosition(Contents.filteredAlbumSongList, 0);
                MediaPlayback.clearState();
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                startActivityForResult(intent, 1);
                return true;
        }
        return false;
    }

    private class ItemClickListener implements OnItemClickListener {
        private final int playlistId;
        private final List<String> items;
        private final int itemMode;

        ItemClickListener(int playlistId, int itemMode, List<String> items) {
            this.playlistId = playlistId;
            this.items = items;
            this.itemMode = itemMode;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Intent intent = new Intent(ItemBrowser.this, TabMain.class);
            intent.putExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);

            if (ItemBrowser.ITEM_MODE_ALBUM == this.itemMode) {
                intent.putExtra(ItemBrowser.ALBUM_FILTER_KEY, this.items.get(position));
            }

            startActivityForResult(intent, 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SEARCH, 0, getString(R.string.search)).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(0, MENU_PLAY_QUEUE, 0, getString(R.string.play_queue))
                .setIcon(R.drawable.ic_menu_play);
        menu.add(0, MENU_VIEW_QUEUE, 0, getString(R.string.view_queue))
                .setIcon(R.drawable.ic_menu_list);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (Contents.queue.size() == 0) {
            menu.findItem(MENU_PLAY_QUEUE).setEnabled(false);
            menu.findItem(MENU_VIEW_QUEUE).setEnabled(false);
        }
        else {
            menu.findItem(MENU_PLAY_QUEUE).setEnabled(true);
            menu.findItem(MENU_VIEW_QUEUE).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        Contents.searchResult = null;
        startSearch(null, false, null, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case MENU_SEARCH:
                onSearchRequested();
                return true;
            case MENU_PLAY_QUEUE:
                Contents.setSongPosition(Contents.queue, 0);
                MediaPlayback.clearState();
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
                intent = new Intent(ItemBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(ItemBrowser.this, QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }

    private void OnItemsReceived(int playlistId, List<String> items) {
        ListView itemList = findViewById(android.R.id.list);
        MyIndexerAdapter<String> adapter = new MyIndexerAdapter<>(
                getApplicationContext(), R.xml.long_list_text_view,
                items);
        setListAdapter(adapter);
        itemList.setOnItemClickListener(new ItemClickListener(playlistId, this.itemMode, items));
        itemList.setFastScrollEnabled(true);
        itemList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.setHeaderTitle(getString(R.string.options));
                menu.add(0, CONTEXT_PLAY_ALBUM, 0, R.string.play_album);
            }
        });
    }

    private static class GetItemsAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<ItemBrowser> itemBrowserWeakReference;
        private final int playlistId;
        private final int itemMode;

        GetItemsAsyncTask(ItemBrowser itemBrowser, int playlistId, int itemMode) {
            this.itemBrowserWeakReference = new WeakReference<>(itemBrowser);
            this.playlistId = playlistId;
            this.itemMode = itemMode;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> result = new ArrayList<>();
            ItemBrowser itemBrowser = this.itemBrowserWeakReference.get();
            if (itemBrowser != null && !itemBrowser.isFinishing()) {
                DatabaseHost databaseHost = new DatabaseHost(itemBrowser.getApplicationContext());
                if (ItemBrowser.ITEM_MODE_ALBUM == this.itemMode) {
                    List<AlbumEntity> albums = databaseHost.getAlbumsForPlaylist(this.playlistId);
                    for (AlbumEntity album : albums) {
                        result.add(album.album);
                    }
                } else if (ItemBrowser.ITEM_MODE_ARTIST == this.itemMode) {
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

            ItemBrowser itemBrowser = this.itemBrowserWeakReference.get();
            if (itemBrowser != null && !itemBrowser.isFinishing()) {
                itemBrowser.OnItemsReceived(this.playlistId, items);
            }
        }
    }
}