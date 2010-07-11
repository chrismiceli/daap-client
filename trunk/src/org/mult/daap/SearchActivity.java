package org.mult.daap;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.mult.daap.background.SearchThread;
import org.mult.daap.client.Song;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class SearchActivity extends ListActivity implements Observer {
   private ArrayList<Song> srList = null;
   private ProgressDialog pd = null;
   private final static int CONTEXT_QUEUE = 0;
   private final static int MENU_PLAY_QUEUE = 1;
   private final static int MENU_VIEW_QUEUE = 2;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setResult(Activity.RESULT_OK);
      if (Contents.address == null) {
         // We got kicked out of memory probably
         Contents.clearState();
         Contents.clearLists();
         NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
         notificationManager.cancelAll();
         setResult(Activity.RESULT_CANCELED);
         finish();
         return;
      }
      final Intent queryIntent = getIntent();
      final String queryAction = queryIntent.getAction();
      if (Intent.ACTION_SEARCH.equals(queryAction)) {
         String searchKeywords = queryIntent
               .getStringExtra(SearchManager.QUERY);
         setTitle(getString(R.string.search_result_title) + " "
               + searchKeywords);
         setContentView(R.xml.music_browser);
         SearchThread sr = Contents.searchResult;
         if (sr == null) {
            // A new search
            sr = new SearchThread(searchKeywords);
            Contents.searchResult = sr;
            pd = ProgressDialog.show(this, getString(R.string.searching_title),
                  getString(R.string.search_result_caption) + " \""
                        + searchKeywords + "\"", true, false);
            sr.addObserver(this);
            Thread thread = new Thread(sr);
            thread.start();
         } else {
            srList = sr.getLastMessage();
            if (srList == null) {
               // We haven't finished yet
               sr.addObserver(this);
               pd = ProgressDialog.show(this,
                     getString(R.string.searching_title),
                     getString(R.string.search_result_caption) + " \""
                           + searchKeywords + "\"", true, false);
            } else {
               // We finished, use the results
               update(sr, srList);
            }
         }
      }
   }

   public void onDestroy() {
      super.onDestroy();
      if (pd != null) {
         pd.dismiss();
      }
      if (Contents.searchResult != null) {
         Contents.searchResult.deleteObservers();
      }
   }

   @Override
   public boolean onContextItemSelected(MenuItem aItem) {
      AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
            .getMenuInfo();
      switch (aItem.getItemId()) {
         case CONTEXT_QUEUE:
            Song s = Contents.songList.get(Contents.songList.indexOf(srList
                  .get(menuInfo.position)));
            if (Contents.queue.contains(s)) { // in
               // list
               Contents.queue.remove(Contents.queue.indexOf(s));
               Toast tst = Toast
                     .makeText(SearchActivity.this,
                           getString(R.string.removed_from_queue),
                           Toast.LENGTH_SHORT);
               tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                     .getYOffset() / 2);
               tst.show();
               return true;
            } else {
               if (Contents.queue.size() < 9) {
                  Contents.addToQueue(s);
                  Toast tst = Toast.makeText(SearchActivity.this,
                        getString(R.string.added_to_queue), Toast.LENGTH_SHORT);
                  tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                        .getYOffset() / 2);
                  tst.show();
               } else {
                  Toast tst = Toast.makeText(SearchActivity.this,
                        getString(R.string.queue_is_full), Toast.LENGTH_SHORT);
                  tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                        .getYOffset() / 2);
                  tst.show();
                  return true;
               }
            }
      }
      return false;
   }

   private void createList() {
      ListView searchResultsList = (ListView) findViewById(android.R.id.list);
      setListAdapter(new ArrayAdapter<Song>(this, R.xml.long_list_text_view,
            srList));
      searchResultsList.setOnItemClickListener(songListListener);
      searchResultsList
            .setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
               public void onCreateContextMenu(ContextMenu menu, View v,
                     ContextMenuInfo menuInfo) {
                  menu.setHeaderTitle(getString(R.string.options));
                  menu.add(0, CONTEXT_QUEUE, 0,
                        R.string.add_or_remove_from_queue);
               }
            });
      searchResultsList.setTextFilterEnabled(true);
      searchResultsList.setFastScrollEnabled(true);
   }

   private OnItemClickListener songListListener = new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View v, int position,
            long id) {
         Contents.setSongPosition(Contents.songList, Contents.songList
               .indexOf(srList.get(position)));
         Contents.clearState();
         NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
         notificationManager.cancelAll();
         Intent intent = new Intent(SearchActivity.this, MediaPlayback.class);
         startActivityForResult(intent, 1);
      }
   };

   @SuppressWarnings("unchecked")
   public void update(Observable observable, Object data) {
      if (data != null) {
         srList = (ArrayList<Song>) data;
         searchHandler.sendEmptyMessage(0);
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      menu.add(0, MENU_PLAY_QUEUE, 0, getString(R.string.play_queue)).setIcon(
            R.drawable.ic_menu_play);
      menu.add(0, MENU_VIEW_QUEUE, 0, getString(R.string.view_queue)).setIcon(
            R.drawable.ic_menu_list);
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
   public boolean onOptionsItemSelected(MenuItem item) {
      Intent intent;
      switch (item.getItemId()) {
         case MENU_PLAY_QUEUE:
            Contents.setSongPosition(Contents.queue, 0);
            Contents.clearState();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            intent = new Intent(SearchActivity.this, MediaPlayback.class);
            startActivityForResult(intent, 1);
            return true;
         case MENU_VIEW_QUEUE:
            intent = new Intent(SearchActivity.this, QueueListBrowser.class);
            startActivityForResult(intent, 1);
      }
      return false;
   }

   private Handler searchHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
         createList();
         if (pd != null)
            pd.dismiss();
         if (srList.size() == 0) {
            Toast tst = Toast.makeText(SearchActivity.this,
                  getString(R.string.no_search_results), Toast.LENGTH_LONG);
            tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst
                  .getYOffset() / 2);
            tst.show();
            finish();
         }
      }
   };

   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == Activity.RESULT_CANCELED) {
         setResult(Activity.RESULT_CANCELED);
         finish();
      }
   }
}