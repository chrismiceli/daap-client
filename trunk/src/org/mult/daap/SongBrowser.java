package org.mult.daap;

import java.util.Collections;
import java.util.Comparator;

import org.mult.daap.client.Song;
import org.mult.daap.client.SongTrackComparator;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SongBrowser extends ListActivity
{
    private ListView musicList;
    private static final int CONTEXT_QUEUE = 0;
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;
    private static String from = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_OK);
        if (Contents.address == null)
        {
            // We got kicked out of memory probably
            Contents.clearState();
            Contents.clearLists();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        setContentView(R.xml.music_browser);
        Bundle b = getIntent().getExtras();
        from = b.getString("from");
        createList();
    }

    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (getListView() != null)
        {
            getListView().clearTextFilter();
        }
        Bundle b = getIntent().getExtras();
        from = b.getString("from");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_CANCELED)
        {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void createList()
    {
        // musicList = (ListView) findViewById(android.R.id.list);
        musicList = getListView();
        musicList.setOnItemClickListener(musicGridListener);
        musicList
                .setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo)
                    {
                        menu.setHeaderTitle(getString(R.string.options));
                        menu.add(0, CONTEXT_QUEUE, 0,
                                R.string.add_or_remove_from_queue);
                    }
                });
        if (from.equals("album"))
        {
            musicList.setTextFilterEnabled(true);
            musicList.setFastScrollEnabled(true);
            Contents.filteredAlbumSongList.clear();
            Bundle b = getIntent().getExtras();
            String albName = b.getString("albumName");
            setTitle(albName);
            if (albName.equals(getString(R.string.no_album_name)))
            {
                albName = "";
            }
            for (Song s : Contents.songList)
            {
                if (s.album.equals(albName))
                {
                    Contents.filteredAlbumSongList.add(s);
                }
            }
            Comparator<Song> stnc = new SongTrackComparator();
            Collections.sort(Contents.filteredAlbumSongList, stnc);
            // Can't use myIndexAdapter because it sorts name, not by track
            setListAdapter(new ArrayAdapter<Song>(this,
                    R.xml.long_list_text_view, Contents.filteredAlbumSongList));
        }
        else if (from.equals("artist"))
        {
            musicList.setTextFilterEnabled(true);
            musicList.setFastScrollEnabled(true);
            Contents.filteredArtistSongList.clear();
            Bundle b = getIntent().getExtras();
            String artistName = b.getString("artistName");
            setTitle(artistName);
            if (artistName.equals(getString(R.string.no_artist_name)))
            {
                artistName = "";
            }
            for (Song s : Contents.songList)
            {
                if (s.artist.equals(artistName))
                {
                    Contents.filteredArtistSongList.add(s);
                }
            }
            // Can't use myIndexAdapter because it sorts name, not by track
            setListAdapter(new ArrayAdapter<Song>(this,
                    R.xml.long_list_text_view, Contents.filteredArtistSongList));
        }
        else
        {
            musicList.setTextFilterEnabled(true);
            musicList.setFastScrollEnabled(true);
            // Collections.sort(Contents.songList);
            MyIndexerAdapter<String> adapter = new MyIndexerAdapter<String>(
                    getApplicationContext(), R.xml.long_list_text_view,
                    Contents.stringElements);
            setListAdapter(adapter);
        }
    }

    private OnItemClickListener musicGridListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                long id)
        {
            if (from.equals("album"))
            {
                Contents.setSongPosition(Contents.filteredAlbumSongList,
                        position);
            }
            else if (from.equals("artist"))
            {
                Contents.setSongPosition(Contents.filteredArtistSongList,
                        position);
            }
            else
            {
                Contents.setSongPosition(Contents.songList, position);
            }
            Contents.clearState();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            Intent intent = new Intent(SongBrowser.this, MediaPlayback.class);
            startActivityForResult(intent, 1);
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem aItem)
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        Song s;
        if (from.equals("album"))
        {
            s = Contents.filteredAlbumSongList.get(menuInfo.position);
        }
        else if (from.equals("artist"))
        {
            s = Contents.filteredArtistSongList.get(menuInfo.position);
        }
        else
        {
            s = Contents.songList.get(menuInfo.position);
        }
        switch (aItem.getItemId())
        {
            case CONTEXT_QUEUE:
                if (Contents.queue.contains(s))
                { // in
                    // list
                    Contents.queue.remove(Contents.queue.indexOf(s));
                    Toast tst = Toast.makeText(SongBrowser.this,
                            getString(R.string.removed_from_queue),
                            Toast.LENGTH_SHORT);
                    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                            .getYOffset() / 2);
                    tst.show();
                    return true;
                }
                else
                {
                    if (Contents.queue.size() < 9)
                    {
                        Contents.addToQueue(s);
                        Toast tst = Toast.makeText(SongBrowser.this,
                                getString(R.string.added_to_queue),
                                Toast.LENGTH_SHORT);
                        tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                                tst.getYOffset() / 2);
                        tst.show();
                    }
                    else
                    {
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_SEARCH, 0, getString(R.string.search)).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(0, MENU_PLAY_QUEUE, 0, getString(R.string.play_queue))
                .setIcon(R.drawable.ic_menu_play);
        menu.add(0, MENU_VIEW_QUEUE, 0, getString(R.string.view_queue))
                .setIcon(R.drawable.ic_menu_list);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        if (Contents.queue.size() == 0)
        {
            menu.findItem(MENU_PLAY_QUEUE).setEnabled(false);
            menu.findItem(MENU_VIEW_QUEUE).setEnabled(false);
        }
        else
        {
            menu.findItem(MENU_PLAY_QUEUE).setEnabled(true);
            menu.findItem(MENU_VIEW_QUEUE).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onSearchRequested()
    {
        Contents.searchResult = null;
        startSearch(null, false, null, false);
        return true;
        // return super.onSearchRequested();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        switch (item.getItemId())
        {
            case MENU_SEARCH:
                onSearchRequested();
                return true;
            case MENU_PLAY_QUEUE:
                Contents.setSongPosition(Contents.queue, 0);
                Contents.clearState();
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
}