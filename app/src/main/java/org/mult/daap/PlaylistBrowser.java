package org.mult.daap;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.widget.Toast;

import org.mult.daap.background.GetSongsForPlaylist;
import org.mult.daap.client.Playlist;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class PlaylistBrowser extends AppCompatActivity implements Observer {
    private RecyclerView playlistListView;
    public final static int START = 0;
    public final static int FINISHED = 1;
    public final static int EMPTY = 2;
    public final static int INITIALIZED = 3;
    private ArrayList<Playlist> playlists;
    private ProgressDialog pd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_playlist_browser);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        if (Contents.address == null) {
            // got kicked out of memory probably
            MediaPlayback.clearState();
            Contents.clearLists();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            this.setResult(Activity.RESULT_CANCELED);
            this.finish();
            return;
        }

        this.playlists = new ArrayList<>(Contents.daapHost.getPlaylists());
        this.playlists.add(0, new Playlist(Contents.daapHost, getString(R.string.all_songs)));

        this.playlistListView = this.findViewById(R.id.playlistList);
        this.playlistListView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        this.playlistListView.setLayoutManager(layoutManager);

        PlaylistAdapter adapter = new PlaylistAdapter(this.playlists);
        this.playlistListView.setAdapter(adapter);

        adapter.setOnItemClickListener(playlistListOnClickListener);
    }

    @Override
    public void onResume() {
        super.onResume(); // this.position = position;
        GetSongsForPlaylist getSongsForPlaylist = Contents.getSongsForPlaylist;
        if (getSongsForPlaylist != null) {
            getSongsForPlaylist.addObserver(this);
            // Since lm is not null, we have to create a new pd
            Integer lastMessage = getSongsForPlaylist.getLastMessage();
            if (lastMessage == INITIALIZED) {
                this.update(getSongsForPlaylist, START);
            } else {
                this.update(getSongsForPlaylist, lastMessage);
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

    /**
     * A listener for click events on the playlist items
     */
    private RecyclerOnItemClickListener<Playlist> playlistListOnClickListener = new RecyclerOnItemClickListener<Playlist>() {
        @Override
        public void onItemClick(Playlist playlist) {
            try {
                if (Contents.playlist_id != playlist.getId()) {
                    // clicking new list
                    GetSongsForPlaylist gsfp = new GetSongsForPlaylist(playlist);
                    grabSongs(gsfp);
                    Contents.playlist_id = playlist.getId();
                } else {
                    uiHandler.sendEmptyMessage(FINISHED);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void update(Observable observable, Object data) {
        if (((Integer) data).compareTo(START) == 0) {
            pd = ProgressDialog.show(this, getString(R.string.fetching_music_title), getString(R.string.fetching_music_detail), true, false);
        } else if (((Integer) data).compareTo(FINISHED) == 0) {
            uiHandler.sendEmptyMessage(FINISHED);
        } else if (((Integer) data).compareTo(EMPTY) == 0) {
            uiHandler.sendEmptyMessage(EMPTY);
        }
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FINISHED) {
                if (pd != null) {
                    pd.dismiss();
                }

                Contents.getSongsForPlaylist = null;
                Intent intent = new Intent(PlaylistBrowser.this, TabMain.class);
                startActivityForResult(intent, 1);
            } else if (msg.what == EMPTY) {
                if (pd != null) {
                    pd.dismiss();
                }

                Contents.getSongsForPlaylist = null;
                Toast tst = Toast.makeText(PlaylistBrowser.this, getString(R.string.empty_playlist), Toast.LENGTH_LONG);
                tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst.getYOffset() / 2);
                tst.show();
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            this.setResult(Activity.RESULT_CANCELED);
            this.finish();
        }
    }

    public void grabSongs(GetSongsForPlaylist getSongsForPlaylist) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Contents.clearLists();
        MediaPlayback.clearState();
        Contents.getSongsForPlaylist = getSongsForPlaylist;
        getSongsForPlaylist.addObserver(this);
        Thread thread = new Thread(getSongsForPlaylist);
        thread.start();
        this.update(getSongsForPlaylist, START);
    }
}