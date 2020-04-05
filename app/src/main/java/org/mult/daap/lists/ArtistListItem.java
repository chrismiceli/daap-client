package org.mult.daap.lists;

import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.mediaplayback.MediaPlaybackActivity;
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
 * A list item for listing artists to use with the flexible adapter
 */
public class ArtistListItem extends AbstractFlexibleItem<ArtistListItem.MyViewHolder> {
    private final String artistName;
    private final int playlistId;

    public ArtistListItem(String artistName, int playlistId) {
        this.artistName = artistName;
        this.playlistId = playlistId;
    }

    private String getId() {
        return String.valueOf(this.artistName);
    }

    public String getText() {
        return this.artistName;
    }

    /**
     * When an item is equals to another?
     * Write your own concept of equals, mandatory to implement or use
     * default java implementation (return this == o;) if you don't have unique IDs!
     * This will be explained in the "Item interfaces" Wiki page.
     */
    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof ArtistListItem) {
            ArtistListItem inItem = (ArtistListItem) inObject;
            return this.getId().equals(inItem.getId());
        }
        return false;
    }

    /**
     * You should implement also this method if equals() is implemented.
     * This method, if implemented, has several implications that Adapter handles better:
     * - The Hash, increases performance in big list during Update & Filter operations.
     * - You might want to activate stable ids via Constructor for RV, if your id
     * is unique (read more in the wiki page: "Setting Up Advanced") you will benefit
     * of the animations also if notifyDataSetChanged() is invoked.
     */
    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    /**
     * For the item type we need an int value: the layoutResID is sufficient.
     */
    @Override
    public int getLayoutRes() {
        return R.layout.simple_row_item;
    }

    /**
     * Delegates the creation of the ViewHolder to the user (AutoMap).
     * The inflated view is already provided as well as the Adapter.
     */
    @Override
    public MyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new MyViewHolder(view, adapter);
    }

    /**
     * The Adapter and the Payload are provided to perform and get more specific
     * information.
     */
    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder,
                               int position,
                               List<Object> payloads) {
        holder.label.setText(this.artistName);
        holder.setArtistName(this.artistName);
        holder.setPlaylistId(this.playlistId);

        // text appears disabled if item is disabled
        holder.label.setEnabled(isEnabled());
    }

    public static class MyViewHolder extends FlexibleViewHolder implements IQueueWorker {
        final TextView label;
        private String artistName;
        private int playlistId;

        MyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            label = view.findViewById(R.id.simple_row_text);
        }

        void setArtistName(String artistName) {
            this.artistName = artistName;
        }

        void setPlaylistId(int playlistId) {
            this.playlistId = playlistId;
        }

        @Override
        public boolean onLongClick(final View view) {
            PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.CENTER);
            popup.inflate(R.menu.artist_popup_menu);
            popup.show();

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.play_artist) {// queue up entire artistName, then start playing
                    DatabaseHost host = new DatabaseHost(view.getContext());
                    host.queueArtist(artistName, playlistId, MyViewHolder.this);
                    return true;
                }
                return false;
                }
            });

            return super.onLongClick(view);
        }

        @Override
        public void songsAddedToQueue(List<SongEntity> songs) {
            if (!songs.isEmpty()) {
                Intent intent = new Intent(this.getContentView().getContext(), MediaPlaybackActivity.class);
                this.getContentView().getContext().startActivity(intent);
            }
        }

        @Override
        public void songsRemovedFromQueue() {
            // this shouldn't be possible because you can only queue artists, not remove an artist from a queue
            Toast.makeText(this.getContentView().getContext(), "Artist Removed From Queue", Toast.LENGTH_LONG).show();
        }
    }
}
