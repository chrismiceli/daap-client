package org.mult.daap;

import android.app.Activity;
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
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.client.Song;

import java.util.ArrayList;

public class QueueListBrowser extends Activity {
    private ArrayList<Song> s;
    private int count;
    private static final int MENU_PLAY_QUEUE = 0;
    private static final int MENU_CLEAR_QUEUE = 1;
    private static final int REMOVE_FROM_QUEUE = 2;

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
        }
    }

    @Override
    public void onResume() {
        super.onResume(); // this.position = position;
        this.setContentView(R.layout.music_browser);
        s = new ArrayList<>();
        if (Contents.queue.size() == 0) {
            finish();
        }
        s.addAll(Contents.queue);
        ListView queuelistList = findViewById(android.R.id.list);
        count = s.size();
        queuelistList.setAdapter(new ProfilesAdapter(getApplicationContext()));
        queuelistList
                .setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                    menu.setHeaderTitle(getString(R.string.options));
                    menu.add(0, REMOVE_FROM_QUEUE, 0,
                            R.string.remove_from_queue);
                });
        queuelistList.setOnItemClickListener(queuelistGridListener);
    }

    @Override
    public boolean onContextItemSelected(MenuItem aItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
                .getMenuInfo();
        if (aItem.getItemId() == REMOVE_FROM_QUEUE) {
            Contents.queue.remove(menuInfo.position);
            Toast tst = Toast.makeText(QueueListBrowser.this,
                    getString(R.string.removed_from_queue),
                    Toast.LENGTH_SHORT);
            tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                    tst.getYOffset() / 2);
            tst.show();
            if (Contents.queue.size() == 0) {
                finish();
            } else {
                final Intent intent = new Intent(QueueListBrowser.this,
                        QueueListBrowser.class);
                startActivityForResult(intent, 1);
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (Contents.queue.size() != 0) {
            menu.add(0, MENU_PLAY_QUEUE, 0, getString(R.string.play_queue))
                    .setIcon(R.drawable.ic_menu_play);
            menu.add(0, MENU_CLEAR_QUEUE, 0, getString(R.string.clear_queue))
                    .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case MENU_PLAY_QUEUE:
                Contents.setSongPosition(Contents.queue, 0);
                MediaPlayback.clearState();
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                intent = new Intent(QueueListBrowser.this, MediaPlayback.class);
                startActivityForResult(intent, 1);
                return true;
            case MENU_CLEAR_QUEUE:
                Contents.queue.clear();
                s.clear();
                setResult(Activity.RESULT_OK);
                finish();
                return true;
        }
        return false;
    }

    private final OnItemClickListener queuelistGridListener = (parent, v, position, id) -> {
        Contents.setSongPosition(Contents.queue, position);
        MediaPlayback.clearState();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Intent intent = new Intent(QueueListBrowser.this,
                MediaPlayback.class);
        startActivityForResult(intent, 1);
        // Contents.playlist_position = (short) position;
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    public class ProfilesAdapter extends BaseAdapter {
        private final Context vContext;

        public ProfilesAdapter(Context c) {
            vContext = c;
        }

        public int getCount() {
            return count;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = new TextView(vContext.getApplicationContext());
            tv.setTextSize(18);
            tv.setText(s.get(position).name);
            return tv;
        }
    }
}