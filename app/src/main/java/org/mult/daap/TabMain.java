package org.mult.daap;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class TabMain extends TabActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_OK);
        setContentView(R.layout.tab_main);
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        Intent intent = new Intent(); // Reusable Intent for each tab
        intent.putExtra("from", "TabMain");
        intent.setClass(this, SongBrowser.class);
        spec = tabHost
                .newTabSpec("songs")
                .setIndicator(getString(R.string.songs)).setContent(intent);
        tabHost.addTab(spec);
        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, ArtistBrowser.class);
        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost
                .newTabSpec("artists")
                .setIndicator(getString(R.string.artists))
                .setContent(intent);
        tabHost.addTab(spec);
        // Do the same for the other tabs
        intent = new Intent().setClass(this, AlbumBrowser.class);
        intent.putExtra("from", "TabMain");
        spec = tabHost
                .newTabSpec("albums")
                .setIndicator(getString(R.string.albums))
                .setContent(intent);
        tabHost.addTab(spec);
        tabHost.setCurrentTab(0);
    }
}