package org.mult.daap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.PlaylistEntity;

import java.lang.ref.WeakReference;
import java.util.List;

public class PlaylistsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        new GetPlaylistsAsyncTask(this).execute();
        return inflater.inflate(R.layout.activity_playlist_browser, container, false);
    }

    private static class OnClickListener implements RecyclerOnItemClickListener<PlaylistEntity> {
        private final PlaylistsFragment playlistsFragment;

        OnClickListener(PlaylistsFragment playlistsFragment) {
            this.playlistsFragment = playlistsFragment;
        }

        @Override
        public void onItemClick(PlaylistEntity item) {
            new PlaylistsFragment.GetSinglePlaylistAsyncTask(this.playlistsFragment, item.getId()).execute();
        }
    }

    private final RecyclerOnItemClickListener<PlaylistEntity> playlistEntityRecyclerOnItemClickListener = new OnClickListener(this);

    /*
     todo cmiceli
    Toast tst = Toast.makeText(PlaylistBrowser.this, getString(R.string.empty_playlist), Toast.LENGTH_LONG);
    tst.setGravity(Gravity.CENTER, tst.getXOffset() / 2, tst.getYOffset() / 2);
    tst.show();
    */

    private void OnPlaylistRetrieved(List<PlaylistEntity> playlists) {
        PlaylistAdapter adapter = new PlaylistAdapter(playlists);
        RecyclerView playlistListView = this.getActivity().findViewById(R.id.playlistList);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnClickListener(this));
    }

    private void OnPlaylistLoaded(int playlistId) {
        Fragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        args.putInt(TabMain.PLAYLIST_ID_BUNDLE_KEY, playlistId);
        fragment.setArguments(args);

        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.remove(this);
        ft.add(R.id.content_frame, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private static class GetPlaylistsAsyncTask extends AsyncTask<Void, Void, List<PlaylistEntity>> {
        private final WeakReference<PlaylistsFragment> playlistsFragmentWeakReference;

        GetPlaylistsAsyncTask(PlaylistsFragment playlistsFragment) {
            this.playlistsFragmentWeakReference = new WeakReference<>(playlistsFragment);
        }

        @Override
        protected List<PlaylistEntity> doInBackground(Void... voids) {
            List<PlaylistEntity> result = null;
            PlaylistsFragment playlistsFragment = this.playlistsFragmentWeakReference.get();
            if (playlistsFragment != null) {
                DatabaseHost databaseHost = new DatabaseHost(playlistsFragment.getActivity().getApplicationContext());
                result = databaseHost.getPlaylists();
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<PlaylistEntity> playlists) {
            super.onPostExecute(playlists);

            PlaylistsFragment playlistsFragment = this.playlistsFragmentWeakReference.get();
            if (playlistsFragment != null && !playlistsFragment.isRemoving()) {
                playlistsFragment.OnPlaylistRetrieved(playlists);
            }
        }
    }


    private static class GetSinglePlaylistAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<PlaylistsFragment> playlistsFragmentWeakReference;
        private final int playlistId;

        GetSinglePlaylistAsyncTask(PlaylistsFragment songsDrawerActivity, int playlistId) {
            this.playlistsFragmentWeakReference = new WeakReference<>(songsDrawerActivity);
            this.playlistId = playlistId;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PlaylistsFragment playlistsFragment = this.playlistsFragmentWeakReference.get();
            if (playlistsFragment != null) {
                DatabaseHost databaseHost = new DatabaseHost(playlistsFragment.getActivity().getApplicationContext());
                databaseHost.fetchSinglePlaylist(Contents.daapHost, this.playlistId);
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            PlaylistsFragment playlistsFragment = this.playlistsFragmentWeakReference.get();
            if (playlistsFragment != null && !playlistsFragment.isRemoving()) {
                playlistsFragment.OnPlaylistLoaded(this.playlistId);
            }
        }
    }
}
