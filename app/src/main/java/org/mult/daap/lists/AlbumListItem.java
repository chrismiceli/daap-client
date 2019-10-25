package org.mult.daap.lists;

import android.app.Application;
import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.mult.daap.AlbumsFragment;
import org.mult.daap.DrawerActivity;
import org.mult.daap.PlaylistActivity;
import org.mult.daap.R;

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

    public AlbumListItem(String albumName) {
        this.albumName = albumName;
    }

    public String getId() {
        return String.valueOf(this.albumName);
    }

    public String getText() {
        return this.albumName;
    }

    /**
     * When an item is equals to another?
     * Write your own concept of equals, mandatory to implement or use
     * default java implementation (return this == o;) if you don't have unique IDs!
     * This will be explained in the "Item interfaces" Wiki page.
     */
    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof AlbumListItem) {
            AlbumListItem inItem = (AlbumListItem) inObject;
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
        holder.label.setText(this.albumName);

        // albumName appears disabled if item is disabled
        holder.label.setEnabled(isEnabled());
    }

    /**
     * The ViewHolder used by this item.
     * Extending from FlexibleViewHolder is recommended especially when you will use
     * more advanced features.
     */
    public class MyViewHolder extends FlexibleViewHolder {

        public TextView label;

        public MyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            label = view.findViewById(R.id.simple_row_text);
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
                            return true;
                        default:
                            return false;
                    }
                }
            });

            return super.onLongClick(view);
        }
    }
}
