package org.mult.daap;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.mult.daap.background.GetSongsForPlaylist;
import org.mult.daap.client.daap.DaapPlaylist;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlaylistBrowser extends Activity implements Observer {
    private ListView playlistList;
    private int count;
    public final static int START = 0;
    public final static int FINISHED = 1;
    public final static int EMPTY = 2;
    public final static int INITIALIZED = 3;
    public final static int MENU_STOP = 4;
    private ArrayList<DaapPlaylist> l;
    private ProgressDialog pd = null;

    @SuppressWarnings("unchecked")
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
        this.setContentView(R.xml.playlist_browser);
        l = new ArrayList<DaapPlaylist>(Contents.daapHost.getPlaylists());
        l.add(0, new DaapPlaylist(Contents.daapHost,
                getString(R.string.all_songs), true));
        count = l.size();
        playlistList = (ListView) findViewById(R.id.playlistList);
        playlistList.setAdapter(new ProfilesAdapter(getApplicationContext()));
        playlistList.setOnItemClickListener(playlistGridListener);
    }

    @Override
    public void onResume() {
        super.onResume(); // this.position = position;
        GetSongsForPlaylist gsfp = Contents.getSongsForPlaylist;
        if (gsfp != null) {
            gsfp.addObserver(this);
            // Since lm is not null, we have to create a new pd
            Integer lastMessage = gsfp.getLastMessage();
            if (lastMessage == INITIALIZED) {
                update(gsfp, START);
            }
            else {
                update(gsfp, lastMessage);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (pd != null) {
            pd.dismiss();
        }
        if (Contents.getSongsForPlaylist != null) {
            Contents.getSongsForPlaylist.deleteObserver(this);
        }
    }

    private OnItemClickListener playlistGridListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position,
                long id) {
            try {
                if (Contents.playlist_position != position) {
                    // clicking new list
                    GetSongsForPlaylist gsfp = new GetSongsForPlaylist(
                            l.get(position));
                    grabSongs(gsfp);
                    Contents.playlist_position = (short) position;
                }
                else {
                    uiHandler.sendEmptyMessage(FINISHED);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void update(Observable observable, Object data) {
        if (((Integer) data).compareTo(START) == 0) {
            pd = ProgressDialog.show(this,
                    getString(R.string.fetching_music_title),
                    getString(R.string.fetching_music_detail), true, false);
        }
        else if (((Integer) data).compareTo(FINISHED) == 0) {
            uiHandler.sendEmptyMessage(FINISHED);
        }
        else if (((Integer) data).compareTo(EMPTY) == 0) {
            uiHandler.sendEmptyMessage(EMPTY);
        }
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FINISHED) { // Finished
                if (pd != null) {
                    pd.dismiss();
                }
                Contents.getSongsForPlaylist = null;
                Intent intent = new Intent(PlaylistBrowser.this, TabMain.class);
                startActivityForResult(intent, 1);
            }
            else if (msg.what == EMPTY) {
                if (pd != null) {
                    pd.dismiss();
                }
                Contents.getSongsForPlaylist = null;
                Toast tst = Toast.makeText(PlaylistBrowser.this,
                        getString(R.string.empty_playlist), Toast.LENGTH_LONG);
                tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2,
                        tst.getYOffset() / 2);
                tst.show();
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    public void grabSongs(GetSongsForPlaylist gsfp) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Contents.clearLists();
        MediaPlayback.clearState();
        Contents.getSongsForPlaylist = gsfp;
        gsfp.addObserver(this);
        Thread thread = new Thread(gsfp);
        thread.start();
        update(gsfp, START);
    }

    public class ProfilesAdapter extends BaseAdapter {
        private Context vContext;

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
            tv.setTextSize(24);
            // tv.setTextColor(Color.rgb(0x00, 0x00, 0x00));
            tv.setText(l.get(position).getName());
            return tv;
        }
    }
}