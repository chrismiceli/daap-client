package org.mult.daap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.ArtistEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ArtistsFragment extends BaseFragment {
    private int playlistId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.playlistId = getArguments().getInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, -1);
        new GetArtistsAsyncTask(this, this.playlistId).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    private class OnClickListener implements RecyclerOnItemClickListener<String> {
        private final ArtistsFragment artistsFragment;
        private final int playlistId;

        OnClickListener(ArtistsFragment artistsFragment, int playlistId) {
            this.artistsFragment = artistsFragment;
            this.playlistId = playlistId;
        }

        @Override
        public void onItemClick(String artist) {
            SongsFragment artistSongsFragment = new SongsFragment();
            Bundle args = new Bundle();
            args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            args.putString(SongsFragment.ARTIST_FILTER_KEY, artist);
            args.putString(SongsFragment.ALBUM_FILTER_KEY, null);
            artistSongsFragment.setArguments(args);

            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.remove(this.artistsFragment);
            ft.add(R.id.content_frame, artistSongsFragment);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    private void OnItemsReceived(List<String> artists) {
        ItemAdapter adapter = new ItemAdapter(artists);
        RecyclerView playlistListView = this.getActivity().findViewById(R.id.music_list);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnClickListener(this, this.playlistId));
    }

    private static class GetArtistsAsyncTask extends AsyncTask<Void, Void, List<ArtistEntity>> {
        private final WeakReference<ArtistsFragment> artistsFragmentWeakReference;
        private final int playlistId;

        GetArtistsAsyncTask(ArtistsFragment artistsFragment, int playlistId) {
            this.artistsFragmentWeakReference = new WeakReference<>(artistsFragment);
            this.playlistId = playlistId;
        }

        @Override
        protected List<ArtistEntity> doInBackground(Void... voids) {
            ArtistsFragment itemsFragment = this.artistsFragmentWeakReference.get();
            if (itemsFragment != null && !itemsFragment.isRemoving()) {
                DatabaseHost databaseHost = new DatabaseHost(itemsFragment.getContext().getApplicationContext());
                return databaseHost.getArtists(this.playlistId);
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<ArtistEntity> artists) {
            super.onPostExecute(artists);

            ArtistsFragment artistsFragment = this.artistsFragmentWeakReference.get();
            if (artistsFragment != null && !artistsFragment.isRemoving()) {
                List<String> artistNames = new ArrayList<>();
                for (ArtistEntity artist : artists) {
                    artistNames.add(artist.artist);
                }

                artistsFragment.OnItemsReceived(artistNames);
            }
        }
    }
}