package org.mult.daap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
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

import org.mult.daap.client.Song;
import org.mult.daap.client.SongDiscNumComparator;
import org.mult.daap.client.SongTrackComparator;
import org.mult.daap.client.StringIgnoreCaseComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class AlbumBrowser extends ListActivity {
    private ListView albumList;
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    private static final int CONTEXT_PLAY_ALBUM = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_OK);
        if (Contents.address == null) {
            // We got kicked out of memory probably
            MediaPlayback.clearState();
            Contents.clearLists();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        // if we haven't obtained the album list yet
        if (Contents.albumNameList.size() == 0) {
            for (Map.Entry<String, ArrayList<Integer>> entry : Contents.AlbumElements
                    .entrySet()) {
                String key = entry.getKey();
                if (key.length() == 0) {
                    Contents.albumNameList
                            .add(getString(R.string.no_album_name));
                }
                else {
                    Contents.albumNameList.add(key);
                }
            }
            Comparator<String> snicc = new StringIgnoreCaseComparator();
            Collections.sort(Contents.albumNameList, snicc);
        }
        setContentView(R.xml.music_browser);
        createList();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void createList() {
        albumList = findViewById(android.R.id.list);
        MyIndexerAdapter<String> adapter = new MyIndexerAdapter<>(
                getApplicationContext(), R.xml.long_list_text_view,
                Contents.albumNameList);
        setListAdapter(adapter);
        albumList.setOnItemClickListener(musicGridListener);
        albumList.setFastScrollEnabled(true);
        albumList
                .setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle(getString(R.string.options));
                        menu.add(0, CONTEXT_PLAY_ALBUM, 0, R.string.play_album);
                    }
                });
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        switch (aItem.getItemId()) {
            case CONTEXT_PLAY_ALBUM:
                Intent intent = new Intent(AlbumBrowser.this,
                        MediaPlayback.class);
                String albName = new String(
                        Contents.albumNameList.get(menuInfo.position));
                if (albName.equals(getString(R.string.no_album_name))) {
                    albName = "";
                }
                Contents.filteredAlbumSongList.clear();
                for (Song s : Contents.songList) {
                    if (s.album.equals(albName)) {
                        Contents.filteredAlbumSongList.add(s);
                    }
                }
                TreeMap<Short, Short> track_num = new TreeMap<>();
                for (Song s : Contents.filteredAlbumSongList) {
                    if (!track_num.keySet().contains(s.disc_num)) {
                        track_num.put(s.disc_num, (short) 1);
                    }
                    else {
                        track_num.put(s.disc_num,
                                (short) (track_num.get(s.disc_num) + 1));
                    }
                }
                Comparator<Song> sdnc = new SongDiscNumComparator();
                Comparator<Song> stnc = new SongTrackComparator();
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

    private OnItemClickListener musicGridListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                long id) {
            Intent intent = new Intent(AlbumBrowser.this, SongBrowser.class);
            intent.putExtra("from", "album");
            intent.putExtra("albumName", Contents.albumNameList.get(position));
            startActivityForResult(intent, 1);
        }
    };

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
                notificationManager.cancelAll();
                intent = new Intent(AlbumBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(AlbumBrowser.this, QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }
}