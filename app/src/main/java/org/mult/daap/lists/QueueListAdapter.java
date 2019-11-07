package org.mult.daap.lists;

import java.util.List;

import androidx.annotation.Nullable;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class QueueListAdapter<T extends IFlexible> extends FlexibleAdapter<T> {
    public QueueListAdapter(@Nullable List<T> items) {
        super(items);
    }

    @Override
    public String onCreateBubbleText(int position) {
        IFlexible iFlexible = this.getItem(position);
        return ((QueueListItem) iFlexible).getSong().name.substring(0, 1).toUpperCase();
    }
}
