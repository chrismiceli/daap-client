package org.mult.daap;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class SongsDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private SearchRequestedCallback mSearchRequestedCallback;

    private int playlistId;

    public void setSearchRequestedCallback(SearchRequestedCallback callback) {
        mSearchRequestedCallback = callback;
    }

    public SearchRequestedCallback getSearchRequestedCallback() {
        return mSearchRequestedCallback;
    }

    public interface SearchRequestedCallback {
        void onSearchRequested();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs_drawer);
        Toolbar toolbar = this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = this.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = this.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        this.playlistId = this.getIntent().getIntExtra(TabMain.PLAYLIST_ID_BUNDLE_KEY, -1);

        Fragment newFragment = new SongsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(TabMain.PLAYLIST_ID_BUNDLE_KEY, playlistId);
        newFragment.setArguments(bundle);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame, newFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = this.findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.songs_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        Bundle args = new Bundle();
        if (id == R.id.nav_all_songs) {
            // TODO what to do when a playlist isn't chosen
            args.putInt(TabMain.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            fragment = new SongsFragment();
        } else if (id == R.id.nav_playlists) {
            Intent intent = new Intent(this, PlaylistActivity.class);
            startActivityForResult(intent, 1);
            return true;
        } else if (id == R.id.nav_artists) {
            // TODO what to do when a playlist isn't chosen
            args.putInt(TabMain.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            args.putInt(ItemsFragment.ITEM_MODE_KEY, ItemsFragment.ITEM_MODE_ARTIST);
            fragment = new ItemsFragment();
        } else if (id == R.id.nav_albums) {
            // TODO what to do when a playlist isn't chosen
            args.putInt(TabMain.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            args.putInt(ItemsFragment.ITEM_MODE_KEY, ItemsFragment.ITEM_MODE_ALBUM);
            fragment = new ItemsFragment();
        }

        DrawerLayout drawer = this.findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        if (fragment != null) {
            fragment.setArguments(args);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();
        }

        return true;
    }
}