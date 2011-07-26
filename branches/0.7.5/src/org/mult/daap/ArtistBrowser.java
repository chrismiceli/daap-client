package org.mult.daap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.mult.daap.client.StringIgnoreCaseComparator;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ArtistBrowser extends ListActivity
{
    private ListView artistList;
    private static final int MENU_PLAY_QUEUE = 1;
    private static final int MENU_VIEW_QUEUE = 2;
    private static final int MENU_SEARCH = 3;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // System.out.println("onCreate");
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
        if (Contents.artistNameList.size() == 0)
        {
            for (Map.Entry<String, ArrayList<Integer>> entry : Contents.ArtistElements
                    .entrySet())
            {
                String key = entry.getKey();
                if (key.length() == 0)
                {
                    Contents.artistNameList
                            .add(getString(R.string.no_artist_name));
                }
                else
                {
                    Contents.artistNameList.add(key);
                }
            }
            Comparator<String> snicc = new StringIgnoreCaseComparator();
            Collections.sort(Contents.artistNameList, snicc);
            // Contents.artistNameList = new ArrayList<String>(
            // Contents.ArtistElements.keySet());
        }
        setContentView(R.xml.music_browser);
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
        artistList = (ListView) findViewById(android.R.id.list);
        // musicList.setAdapter(new ProfilesAdapter(getApplicationContext()));
        MyIndexerAdapter<String> adapter = new MyIndexerAdapter<String>(
                getApplicationContext(), R.xml.long_list_text_view,
                Contents.artistNameList);
        setListAdapter(adapter);
        artistList.setOnItemClickListener(musicGridListener);
        artistList.setFastScrollEnabled(true);

    }

    private OnItemClickListener musicGridListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                long id)
        {
            Intent intent = new Intent(ArtistBrowser.this, SongBrowser.class);
            intent.putExtra("from", "artist");
            intent
                    .putExtra("artistName", Contents.artistNameList
                            .get(position));
            startActivityForResult(intent, 1);
        }
    };

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