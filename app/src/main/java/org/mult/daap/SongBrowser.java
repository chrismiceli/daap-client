package org.mult.daap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.client.Song;
import org.mult.daap.client.SongDiscNumComparator;
import org.mult.daap.client.SongTrackComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SongBrowser extends ListActivity {
    private static final int CONTEXT_QUEUE = 0;
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    private static String from = null;

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
        setContentView(R.layout.music_browser);
        Bundle b = getIntent().getExtras();
        from = b.getString("from");
        createList();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getListView() != null) {
            getListView().clearTextFilter();
        }
        Bundle b = getIntent().getExtras();
        from = b.getString("from");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void createList() {
        ListView musicList = getListView();
        musicList.setOnItemClickListener(musicGridListener);
        musicList
                .setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                    menu.setHeaderTitle(getString(R.string.options));
                    menu.add(0, CONTEXT_QUEUE, 0,
                            R.string.add_or_remove_from_queue);
                });
        if (from.equals("album")) {
            musicList.setFastScrollEnabled(true);
            Contents.filteredAlbumSongList.clear();
            Bundle b = getIntent().getExtras();
            String albName = b.getString("albumName");
            setTitle(albName);
            if (albName.equals(getString(R.string.no_album_name))) {
                albName = "";
            }
            for (Song s : Contents.songList) {
                if (s.album.equals(albName)) {
                    Contents.filteredAlbumSongList.add(s);
                }
            }
            TreeMap<Short, Short> track_num = new TreeMap<>();
            for (Song s : Contents.filteredAlbumSongList) {
                Short count = track_num.get(s.disc_num);
                if (count == null) {
                    track_num.put(s.disc_num, (short) 1);
                } else {
                    track_num.put(s.disc_num, (short) (count + 1));
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
            // Can't use myIndexAdapter because it sorts name, not by track
            setListAdapter(new MyArrayAdapter<>(this,
                    R.xml.long_list_text_view, Contents.filteredAlbumSongList));
        } else if (from.equals("artist")) {
            musicList.setFastScrollEnabled(true);
            Contents.filteredArtistSongList.clear();
            Bundle b = getIntent().getExtras();
            String artistName = b.getString("artistName");
            setTitle(artistName);
            if (artistName.equals(getString(R.string.no_artist_name))) {
                artistName = "";
            }
            for (Song s : Contents.songList) {
                if (s.artist.equals(artistName)) {
                    Contents.filteredArtistSongList.add(s);
                }
            }
            // Can't use myIndexAdapter because it sorts name, not by track
            setListAdapter(new MyArrayAdapter<>(this,
                    R.xml.long_list_text_view, Contents.filteredArtistSongList));
        } else {
            musicList.setFastScrollEnabled(true);
            MyIndexerAdapter<String> adapter = new MyIndexerAdapter<>(
                    this, R.xml.long_list_text_view,
                    Contents.stringElements);
            setListAdapter(adapter);
        }
    }

    private final OnItemClickListener musicGridListener = (parent, v, position, id) -> {
        if (from.equals("album")) {
            Contents.setSongPosition(Contents.filteredAlbumSongList,
                    position);
        } else if (from.equals("artist")) {
            Contents.setSongPosition(Contents.filteredArtistSongList,
                    position);
        } else {
            Contents.setSongPosition(Contents.songList, position);
        }
        MediaPlayback.clearState();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Intent intent = new Intent(SongBrowser.this, MediaPlayback.class);
        startActivityForResult(intent, 1);
    };

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        Song s;
        if (from.equals("album")) {
            s = Contents.filteredAlbumSongList.get(menuInfo.position);
        } else if (from.equals("artist")) {
            s = Contents.filteredArtistSongList.get(menuInfo.position);
        } else {
            s = Contents.songList.get(menuInfo.position);
        }
        if (aItem.getItemId() == CONTEXT_QUEUE) {
            if (Contents.queue.contains(s)) { // in
                // list
                Contents.queue.remove(s);
                Toast tst = Toast.makeText(SongBrowser.this,
                        getString(R.string.removed_from_queue),
                        Toast.LENGTH_SHORT);
                tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                        tst.getYOffset() / 2);
                tst.show();
                return true;
            } else {
                if (Contents.queue.size() < 9) {
                    Contents.addToQueue(s);
                    Toast tst = Toast.makeText(SongBrowser.this,
                            getString(R.string.added_to_queue),
                            Toast.LENGTH_SHORT);
                    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                            tst.getYOffset() / 2);
                    tst.show();
                } else {
                    Toast tst = Toast.makeText(SongBrowser.this,
                            getString(R.string.queue_is_full),
                            Toast.LENGTH_SHORT);
                    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                            tst.getYOffset() / 2);
                    tst.show();
                    return true;
                }
            }
        }
        return false;
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
        // return super.onSearchRequested();
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
                intent = new Intent(SongBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(SongBrowser.this, QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }

    static class MyArrayAdapter<T> extends ArrayAdapter<T> {
        final ArrayList<Song> myElements;
        final Context vContext;

        public MyArrayAdapter(Context context, int textViewResourceId,
                              List<T> objects) {
            super(context, textViewResourceId, objects);
            vContext = context;
            myElements = (ArrayList<Song>) objects;
        }

        @Override
        public int getCount() {
            return myElements.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(vContext.getApplicationContext());
            tv.setTextSize(18);
            tv.setText(myElements.get(position).toString());
            return tv;
        }
    }
}