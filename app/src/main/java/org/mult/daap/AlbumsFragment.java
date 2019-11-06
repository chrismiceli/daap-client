package org.mult.daap;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.AlbumEntity;
import org.mult.daap.lists.AlbumListAdapter;
import org.mult.daap.lists.AlbumListItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;

class AlbumsFragment extends BaseFragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    private int playlistId;
    private AlbumListAdapter<AlbumListItem> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.playlistId = getArguments().getInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, -1);
        new GetAlbumsAsyncTask(this, this.playlistId).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        SongsFragment albumSongsFragment = new SongsFragment();
        AlbumListItem listItem = this.mAdapter.getItem(position);
        Bundle args = new Bundle();
        args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
        args.putString(SongsFragment.ARTIST_FILTER_KEY, null);
        args.putString(SongsFragment.ALBUM_FILTER_KEY, listItem.getText());
        albumSongsFragment.setArguments(args);

        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.remove(this);
        ft.add(R.id.content_frame, albumSongsFragment);
        ft.addToBackStack(null);
        ft.commit();

        return true;
    }

    @Override
    public void onItemLongClick(int position) {
        // purposefully empty, the adapter's view holder intercept's the long click
    }

    private void OnItemsReceived(List<String> albums) {
        List<AlbumListItem> albumItems = new ArrayList<>();
        for (String album : albums) {
            albumItems.add(new AlbumListItem(album, this.playlistId));
        }

        this.mAdapter = new AlbumListAdapter<>(albumItems);

        RecyclerView albumListView = this.getActivity().findViewById(R.id.music_list);
        albumListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        albumListView.setLayoutManager(layoutManager);
        albumListView.setAdapter(mAdapter);

        FastScroller fastScroller = this.getView().findViewById(R.id.fast_scroller);

        fastScroller.setMinimumScrollThreshold(70);
        fastScroller.setBubbleAndHandleColor(Color.parseColor("#4DB6AC"));

        this.mAdapter.setFastScroller(fastScroller);
        this.mAdapter.setMode(SelectableAdapter.Mode.SINGLE);
        this.mAdapter.addListener(this);
    }

    private static class GetAlbumsAsyncTask extends AsyncTask<Void, Void, List<AlbumEntity>> {
        private final WeakReference<AlbumsFragment> albumsFragmentWeakReference;
        private final int playlistId;

        GetAlbumsAsyncTask(AlbumsFragment albumsFragment, int playlistId) {
            this.albumsFragmentWeakReference = new WeakReference<>(albumsFragment);
            this.playlistId = playlistId;
        }

        @Override
        protected List<AlbumEntity> doInBackground(Void... voids) {
            AlbumsFragment itemsFragment = this.albumsFragmentWeakReference.get();
            if (itemsFragment != null && !itemsFragment.isRemoving()) {
                DatabaseHost databaseHost = new DatabaseHost(itemsFragment.getContext().getApplicationContext());
                return databaseHost.getAlbums(this.playlistId);
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<AlbumEntity> albums) {
            super.onPostExecute(albums);

            AlbumsFragment albumsFragment = this.albumsFragmentWeakReference.get();
            if (albumsFragment != null && !albumsFragment.isRemoving()) {
                List<String> albumNames = new ArrayList<>();
                for (AlbumEntity album : albums) {
                    albumNames.add(album.album);
                }

                albumsFragment.OnItemsReceived(albumNames);
            }
        }
    }
}