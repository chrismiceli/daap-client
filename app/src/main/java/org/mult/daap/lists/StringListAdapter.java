package org.mult.daap.lists;

import java.util.List;

import androidx.annotation.Nullable;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class StringListAdapter<T extends IFlexible> extends FlexibleAdapter<T> {
    public StringListAdapter(@Nullable List items) {
        super(items);
    }

    @Override
    public String onCreateBubbleText(int position) {
        IFlexible iFlexible = this.getItem(position);
        return ((StringListItem) iFlexible).getText().substring(0, 1).toUpperCase();
    }
}
