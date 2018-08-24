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
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.db.entity.SongEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
        setContentView(R.xml.music_browser);
        new GetArtistsAsyncTask(this, this.getIntent().getIntExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, -1)).execute();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        switch (aItem.getItemId()) {
            case CONTEXT_PLAY_ARTIST:
                Intent intent = new Intent(ArtistBrowser.this,
                        MediaPlayback.class);
                String albName = Contents.artistNameList.get(menuInfo.position);
                if (albName.equals(getString(R.string.no_artist_name))) {
                    albName = "";
                }
                Contents.filteredArtistSongList.clear();
                for (SongEntity s : Contents.songList) {
                    if (s.artist.equals(albName)) {
                        Contents.filteredArtistSongList.add(s);
                    }
                }
                Contents.setSongPosition(Contents.filteredArtistSongList, 0);
                MediaPlayback.clearState();
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
                startActivityForResult(intent, 1);
                return true;
        }
        return false;
    }

    private OnItemClickListener musicGridListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                long id) {
            String artist = Contents.artistNameList.get(position);
            Contents.ArtistAlbumElements.clear();
            for (SongEntity song : Contents.songList) {
                if (song.artist.equals(artist)) {
                    if (Contents.ArtistAlbumElements.containsKey(song.album)) {
                        Contents.ArtistAlbumElements.get(song.album).add(
                                song.id);
                    }
                    else {
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
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
                intent = new Intent(ArtistBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_VIEW_QUEUE:
                intent = new Intent(ArtistBrowser.this, QueueListBrowser.class);
                startActivityForResult(intent, 1);
        }
        return false;
    }

    private void OnArtistsReceived(List<ArtistEntity> artists) {
        if (artists != null) {
            ListView artistList = findViewById(android.R.id.list);
            List<String> artistsStringList = new ArrayList<>();
            for(ArtistEntity artist : artists) {
                artistsStringList.add(artist.artist);
            }
            MyIndexerAdapter<String> adapter = new MyIndexerAdapter<>(
                getApplicationContext(), R.xml.long_list_text_view,
                artistsStringList);
            setListAdapter(adapter);
            artistList.setOnItemClickListener(musicGridListener);
            artistList.setFastScrollEnabled(true);
            artistList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                    menu.setHeaderTitle(getString(R.string.options));
                    menu.add(0, CONTEXT_PLAY_ARTIST, 0, R.string.play_artist);
                }
            });
        }
    }

    private static class GetArtistsAsyncTask extends AsyncTask<Void, Void, List<ArtistEntity>> {
        private final WeakReference<ArtistBrowser> artistBrowserWeakReference;
        private final int playlistId;

        GetArtistsAsyncTask(ArtistBrowser artistBrowser, int playlistId) {
            this.artistBrowserWeakReference = new WeakReference<>(artistBrowser);
            this.playlistId = playlistId;
        }

        @Override
        protected List<ArtistEntity> doInBackground(Void... voids) {
            List<ArtistEntity> result = null;
            ArtistBrowser artistBrowser = this.artistBrowserWeakReference.get();
            if (artistBrowser != null) {
                DatabaseHost databaseHost = new DatabaseHost(artistBrowser.getApplicationContext());
                result = databaseHost.getArtistsForPlaylist(this.playlistId);
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<ArtistEntity> artists) {
            super.onPostExecute(artists);

            ArtistBrowser artistBrowser = this.artistBrowserWeakReference.get();
            if (artistBrowser != null && !artistBrowser.isFinishing()) {
                artistBrowser.OnArtistsReceived(artists);
            }
        }
    }
}