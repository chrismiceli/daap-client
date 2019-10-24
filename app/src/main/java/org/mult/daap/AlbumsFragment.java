package org.mult.daap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mult.daap.client.DatabaseHost;
import org.mult.daap.db.entity.AlbumEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AlbumsFragment extends BaseFragment {
    private int playlistId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.playlistId = getArguments().getInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, -1);
        new GetAlbumsAsyncTask(this, this.playlistId).execute();
        return inflater.inflate(R.layout.music_browser, container, false);
    }

    private class OnClickListener implements RecyclerOnItemClickListener<String> {
        private final AlbumsFragment albumsFragment;
        private final int playlistId;

        OnClickListener(AlbumsFragment albumsFragment, int playlistId) {
            this.albumsFragment = albumsFragment;
            this.playlistId = playlistId;
        }

        @Override
        public void onItemClick(String album) {
            SongsFragment albumSongsFragment = new SongsFragment();
            Bundle args = new Bundle();
            args.putInt(BaseFragment.PLAYLIST_ID_BUNDLE_KEY, this.playlistId);
            args.putString(SongsFragment.ARTIST_FILTER_KEY, null);
            args.putString(SongsFragment.ALBUM_FILTER_KEY, album);
            albumSongsFragment.setArguments(args);

            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.remove(this.albumsFragment);
            ft.add(R.id.content_frame, albumSongsFragment);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    private void OnItemsReceived(List<String> albums) {
        ItemAdapter adapter = new ItemAdapter(albums);
        RecyclerView playlistListView = this.getActivity().findViewById(R.id.music_list);
        playlistListView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        playlistListView.setLayoutManager(layoutManager);
        playlistListView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnClickListener(this, this.playlistId));
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