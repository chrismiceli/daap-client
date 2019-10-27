package org.mult.daap.lists;

import android.app.Application;
import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.AlbumsFragment;
import org.mult.daap.Contents;
import org.mult.daap.DrawerActivity;
import org.mult.daap.MediaPlaybackActivity;
import org.mult.daap.PlaylistActivity;
import org.mult.daap.R;
import org.mult.daap.client.DatabaseHost;
import org.mult.daap.client.IQueueWorker;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

import androidx.appcompat.widget.PopupMenu;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * A list item for listing artists, or albums (any plain string really).
 * To use with the flexible adapter
 */
public class AlbumListItem extends AbstractFlexibleItem<AlbumListItem.MyViewHolder> {
    private final String albumName;
    private final int playlistId;

    public AlbumListItem(String albumName, int playlistId) {
        this.albumName = albumName;
        this.playlistId = playlistId;
    }

    public String getId() {
        return String.valueOf(this.albumName);
    }

    public String getText() {
        return this.albumName;
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof AlbumListItem) {
            AlbumListItem inItem = (AlbumListItem) inObject;
            return this.getId().equals(inItem.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.simple_row_item;
    }

    @Override
    public MyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new MyViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder,
                               int position,
                               List<Object> payloads) {
        holder.label.setText(this.albumName);
        holder.setAlbumName(this.albumName);
        holder.setPlaylistId(this.playlistId);

        // albumName appears disabled if item is disabled
        holder.label.setEnabled(isEnabled());
    }

    public class MyViewHolder extends FlexibleViewHolder implements IQueueWorker {
        public TextView label;
        public String albumName;
        public int playlistId;

        public MyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            label = view.findViewById(R.id.simple_row_text);
        }

        public void setAlbumName(String albumName) {
            this.albumName = albumName;
        }
        public void setPlaylistId(int playlistId) {
            this.playlistId = playlistId;
        }

        @Override
        public boolean onLongClick(final View view) {
            PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.CENTER);
            popup.inflate(R.menu.album_popup_menu);
            popup.show();

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.play_album:
                            // queue up entire album, then start playing
                            DatabaseHost host = new DatabaseHost(view.getContext());
                            host.queueAlbum(albumName, playlistId, MyViewHolder.this);
                            return true;
                        default:
                            return false;
                    }
                }
            });

            return super.onLongClick(view);
        }

        @Override
        public void songsAddedToQueue(List<SongEntity> songs) {
            if (!songs.isEmpty()) {
                Contents.song = songs.get(0);
                Intent intent = new Intent(this.getContentView().getContext(), MediaPlaybackActivity.class);
                this.getContentView().getContext().startActivity(intent);
            }
        }

        @Override
        public void songsRemovedFromQueue(List<SongEntity> songs) {
            // this shouldn't be possible because you can only queue albums, not remove an album from a queue
            Toast.makeText(this.getContentView().getContext(), "Album Removed From Queue", Toast.LENGTH_LONG).show();
        }
    }
}
