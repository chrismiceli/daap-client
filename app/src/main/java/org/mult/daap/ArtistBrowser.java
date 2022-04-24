package org.mult.daap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.mult.daap.client.Song;
import org.mult.daap.client.StringIgnoreCaseComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class ArtistBrowser extends ListActivity {
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    private static final int CONTEXT_PLAY_ARTIST = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // System.out.println("onCreate");
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
        if (Contents.artistNameList.size() == 0) {
            for (Map.Entry<String, ArrayList<Integer>> entry : Contents.ArtistElements
                    .entrySet()) {
                String key = entry.getKey();
                if (key.length() == 0) {
                    Contents.artistNameList
                            .add(getString(R.string.no_artist_name));
                } else {
                    Contents.artistNameList.add(key);
                }
            }
            Comparator<String> snicc = new StringIgnoreCaseComparator();
            Collections.sort(Contents.artistNameList, snicc);
        }
        setContentView(R.layout.music_browser);
        createList();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void createList() {
        ListView artistList = findViewById(android.R.id.list);
        MyIndexerAdapter<String> adapter = new MyIndexerAdapter<>(
                getApplicationContext(), R.xml.long_list_text_view,
                Contents.artistNameList);
        setListAdapter(adapter);
        artistList.setOnItemClickListener(musicGridListener);
        artistList.setFastScrollEnabled(true);
        artistList
                .setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                    menu.setHeaderTitle(getString(R.string.options));
                    menu.add(0, CONTEXT_PLAY_ARTIST, 0,
                            R.string.play_artist);
                });
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        if (aItem.getItemId() == CONTEXT_PLAY_ARTIST) {
            Intent intent = new Intent(ArtistBrowser.this,
                    MediaPlayback.class);
            String albName = Contents.artistNameList.get(menuInfo.position);
            if (albName.equals(getString(R.string.no_artist_name))) {
                albName = "";
            }
            Contents.filteredArtistSongList.clear();
            for (Song s : Contents.songList) {
                if (s.artist.equals(albName)) {
                    Contents.filteredArtistSongList.add(s);
                }
            }
            Contents.setSongPosition(Contents.filteredArtistSongList, 0);
            MediaPlayback.clearState();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            startActivityForResult(intent, 1);
            return true;
        }
        return false;
    }

    private final OnItemClickListener musicGridListener = (parent, v, position, id) -> {
        String artist = Contents.artistNameList.get(position);
        Contents.ArtistAlbumElements.clear();
        for (Song song : Contents.songList) {
            if (song.artist.equals(artist)) {
                if (Contents.ArtistAlbumElements.containsKey(song.album)) {
                    Contents.ArtistAlbumElements.get(song.album).add(
                            song.id);
                } else {
                    ArrayList<Integer> t = new ArrayList<>();
                    t.add(song.id);
                    Contents.ArtistAlbumElements.put(song.album, t);
                    Contents.artistAlbumNameList.add(song.album);
                }
            }
        }
        Contents.artistAlbumNameList.clear();
        Intent intent = new Intent(ArtistBrowser.this,
                ArtistAlbumBrowser.class);
        intent.putExtra("from", "artist");
        intent.putExtra("artistName", Contents.artistNameList.get(position));
        startActivityForResult(intent, 1);
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
        } else {
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
                intent = new Intent(ArtistBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(ArtistBrowser.this, QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }
}