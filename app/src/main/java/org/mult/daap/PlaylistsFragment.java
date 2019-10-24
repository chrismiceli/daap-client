package org.mult.daap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.PlaylistEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PlaylistsFragment extends BaseFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        new GetPlaylistsAsyncTask(this).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    private class OnClickListener implements RecyclerOnItemClickListener<PlaylistEntity> {
        private final PlaylistsFragment playlistsFragment;

        OnClickListener(PlaylistsFragment itemsFragment) {
            this.playlistsFragment = itemsFragment;
        }

        @Override
        public void onItemClick(PlaylistEntity item) {
            SongsFragment allSongsFragment = new SongsFragment();
            Bundle args = new Bundle();
            args.putInt(TabMain.PLAYLIST_ID_BUNDLE_KEY, item.getId());
            allSongsFragment.setArguments(args);

            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.remove(this.playlistsFragment);
            ft.add(R.id.content_frame, allSongsFragment);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    private void OnItemsReceived(List<PlaylistEntity> playlists) {
        PlaylistAdapter adapter = new PlaylistAdapter(playlists);
        RecyclerView playlistListView = this.getActivity().findViewById(R.id.music_list);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnClickListener(this));
    }

    private static class GetPlaylistsAsyncTask extends AsyncTask<Void, Void, List<PlaylistEntity>> {
        private final WeakReference<PlaylistsFragment> playlistsFragmentWeakReference;

        GetPlaylistsAsyncTask(PlaylistsFragment playlistsFragment) {
            this.playlistsFragmentWeakReference = new WeakReference<>(playlistsFragment);
        }

        @Override
        protected List<PlaylistEntity> doInBackground(Void... voids) {
            PlaylistsFragment itemsFragment = this.playlistsFragmentWeakReference.get();
            if (itemsFragment != null && !itemsFragment.isRemoving()) {
                DatabaseHost databaseHost = new DatabaseHost(itemsFragment.getContext().getApplicationContext());
                return databaseHost.getPlaylists();
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<PlaylistEntity> playlists) {
            super.onPostExecute(playlists);

            PlaylistsFragment playlistsFragment = this.playlistsFragmentWeakReference.get();
            if (playlistsFragment != null && !playlistsFragment.isRemoving()) {
                playlistsFragment.OnItemsReceived(playlists);
            }
        }
    }
}