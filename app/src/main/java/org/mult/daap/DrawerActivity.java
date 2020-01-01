package org.mult.daap;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class DrawerActivity extends AppCompatActivity
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
        setContentView(R.layout.activity_drawer);
        final Toolbar toolbar = this.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = this.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                 this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = this.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button playlistsButton = navigationView.getHeaderView(0).findViewById(R.id.btn_back_to_playlists);
        playlistsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(DrawerActivity.this, PlaylistActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivityForResult(intent, 1);
            }
        });

        this.playlistId = this.getIntent().getIntExtra(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, -1);

        Fragment newFragment = new SongsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, playlistId);
        newFragment.setArguments(bundle);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame, newFragment);
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
        this.getMenuInflater().inflate(R.menu.drawer_context_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {
            this.onSearchRequested();
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
            args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            fragment = new SongsFragment();
        } else if (id == R.id.nav_artists) {
            args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            fragment = new ArtistsFragment();
        } else if (id == R.id.nav_albums) {
            args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            fragment = new AlbumsFragment();
        }
        else if (id == R.id.nav_queue) {
            fragment = new QueueFragment();
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