package org.mult.daap.lists;

import android.view.View;
import android.widget.TextView;

import org.mult.daap.R;
import org.mult.daap.db.entity.SongEntity;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * A list item for listing songs.  To use with the flexible adapter
 */
public class SongListItem extends AbstractFlexibleItem<SongListItem.MyViewHolder> {
    private final SongEntity song;

    public SongListItem(SongEntity song) {
        this.song = song;
    }

    public String getId() {
        return String.valueOf(this.song.id);
    }

    public SongEntity getSong() {
        return this.song;
    }

    /**
     * When an item is equals to another?
     * Write your own concept of equals, mandatory to implement or use
     * default java implementation (return this == o;) if you don't have unique IDs!
     * This will be explained in the "Item interfaces" Wiki page.
     */
    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof SongListItem) {
            SongListItem inItem = (SongListItem) inObject;
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
        holder.label.setText(this.song.name);

        // text appears disabled if item is disabled
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
    }
}
