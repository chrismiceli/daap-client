package org.mult.daap;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.ArtistEntity;
import org.mult.daap.lists.StringListAdapter;
import org.mult.daap.lists.StringListItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;

public class ArtistsFragment extends BaseFragment implements FlexibleAdapter.OnItemClickListener {
    private int playlistId;
    private StringListAdapter<StringListItem> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.playlistId = getArguments().getInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, -1);
        new GetArtistsAsyncTask(this, this.playlistId).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        SongsFragment artistSongsFragment = new SongsFragment();
        StringListItem listItem = this.mAdapter.getItem(position);
        Bundle args = new Bundle();
        args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
        args.putString(SongsFragment.ARTIST_FILTER_KEY, listItem.getText());
        args.putString(SongsFragment.ALBUM_FILTER_KEY, null);
        artistSongsFragment.setArguments(args);

        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.remove(this);
        ft.add(R.id.content_frame, artistSongsFragment);
        ft.addToBackStack(null);
        ft.commit();

        return true;
    }

    private void OnItemsReceived(List<String> artists) {
        List<StringListItem> artistItems = new ArrayList<>();
        for (String artist : artists) {
            artistItems.add(new StringListItem(artist));
        }

        this.mAdapter = new StringListAdapter<>(artistItems);

        RecyclerView artistListView = this.getActivity().findViewById(R.id.music_list);
        artistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        artistListView.setLayoutManager(layoutManager);
        artistListView.setAdapter(mAdapter);

        FastScroller fastScroller = this.getView().findViewById(R.id.fast_scroller);

        fastScroller.setMinimumScrollThreshold(70);
        fastScroller.setBubbleAndHandleColor(Color.parseColor("#4DB6AC"));

        this.mAdapter.setFastScroller(fastScroller);
        this.mAdapter.setMode(SelectableAdapter.Mode.SINGLE);
        this.mAdapter.addListener(this);
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