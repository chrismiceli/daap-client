package org.mult.daap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SongBrowser extends ListActivity {
    private static final int CONTEXT_QUEUE = 0;
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    public static String ARTIST_FILTER_KEY = "__ARTIST_FILTER_KEY__";
    public static String ALBUM_FILTER_KEY = "__ALBUM_FILTER_KEY__";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_OK);
        setContentView(R.xml.music_browser);
        int playlistId = getIntent().getExtras().getInt(TabMain.PLAYLIST_ID_BUNDLE_KEY);
        new GetSongsAsyncTask(this, playlistId).execute();
    }

    private OnItemClickListener musicGridListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                long id) {
//            if (from.equals("album")) {
//                Contents.setSongPosition(Contents.filteredAlbumSongList,
//                        position);
//            }
//            else if (from.equals("artist")) {
//                Contents.setSongPosition(Contents.filteredArtistSongList,
//                        position);
//            }
//            else {
//                Contents.setSongPosition(Contents.songList, position);
//            }
//            MediaPlayback.clearState();
//            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            if (notificationManager != null) {
//                notificationManager.cancelAll();
//            }
//            Intent intent = new Intent(SongBrowser.this, MediaPlayback.class);
//            startActivityForResult(intent, 1);
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
//        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
//                .getMenuInfo();
//        SongEntity s;
//        if (from.equals("album")) {
//            s = Contents.filteredAlbumSongList.get(menuInfo.position);
//        }
//        else if (from.equals("artist")) {
//            s = Contents.filteredArtistSongList.get(menuInfo.position);
//        }
//        else {
//            s = Contents.songList.get(menuInfo.position);
//        }
//        switch (aItem.getItemId()) {
//            case CONTEXT_QUEUE:
//                if (Contents.queue.contains(s)) { // in
//                                                  // list
//                    Contents.queue.remove(Contents.queue.indexOf(s));
//                    Toast tst = Toast.makeText(SongBrowser.this,
//                            getString(R.string.removed_from_queue),
//                            Toast.LENGTH_SHORT);
//                    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
//                            tst.getYOffset() / 2);
//                    tst.show();
//                    return true;
//                }
//                else {
//                    if (Contents.queue.size() < 9) {
//                        Contents.addToQueue(s);
//                        Toast tst = Toast.makeText(SongBrowser.this,
//                                getString(R.string.added_to_queue),
//                                Toast.LENGTH_SHORT);
//                        tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
//                                tst.getYOffset() / 2);
//                        tst.show();
//                    }
//                    else {
//                        Toast tst = Toast.makeText(SongBrowser.this,
//                                getString(R.string.queue_is_full),
//                                Toast.LENGTH_SHORT);
//                        tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
//                                tst.getYOffset() / 2);
//                        tst.show();
//                        return true;
//                    }
//                }
//        }
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
                intent = new Intent(SongBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(SongBrowser.this, QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }

    class MyArrayAdapter<T> extends ArrayAdapter<T> {
        ArrayList<SongEntity> myElements;
        Context vContext;
        int font_size;

        MyArrayAdapter(Context context, int textViewResourceId,
                List<T> objects) {
            super(context, textViewResourceId, objects);
            SharedPreferences mPrefs = PreferenceManager
                    .getDefaultSharedPreferences(context);
            font_size = Integer.valueOf(mPrefs.getString("font_pref", "18"));
            vContext = context;
            myElements = (ArrayList<SongEntity>) objects;
        }

        @Override
        public int getCount() {
            return myElements.size();
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView tv = new TextView(vContext.getApplicationContext());
            tv.setTextSize(font_size);
            tv.setTextColor(Color.WHITE);
            tv.setText(myElements.get(position).toString());
            return tv;
        }
    }

    private void OnSongsReceived(List<SongEntity> songs) {
        ListView musicList = getListView();
        musicList.setOnItemClickListener(musicGridListener);
        musicList
                .setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle(getString(R.string.options));
                        menu.add(0, CONTEXT_QUEUE, 0,
                                R.string.add_or_remove_from_queue);
                    }
                });
            setListAdapter(new MyArrayAdapter<>(this,
                    R.xml.long_list_text_view, songs));
    }

    private static class GetSongsAsyncTask extends AsyncTask<Void, Void, List<SongEntity>> {
        private final WeakReference<SongBrowser> songBrowserWeakReference;
        private final int playlistId;

        GetSongsAsyncTask(SongBrowser songBrowser, int playlistId) {
            this.songBrowserWeakReference = new WeakReference<>(songBrowser);
            this.playlistId = playlistId;
        }

        @Override
        protected List<SongEntity> doInBackground(Void... voids) {
            List<SongEntity> result = null;
            SongBrowser songBrowser = this.songBrowserWeakReference.get();
            if (songBrowser != null && !songBrowser.isFinishing()) {
                DatabaseHost databaseHost = new DatabaseHost(songBrowser.getApplicationContext());
                result = databaseHost.getSongsForPlaylist(this.playlistId);
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<SongEntity> songs) {
            super.onPostExecute(songs);

            SongBrowser songBrowser = this.songBrowserWeakReference.get();
            if (songBrowser != null && !songBrowser.isFinishing()) {
                songBrowser.OnSongsReceived(songs);
            }
        }
    }
}