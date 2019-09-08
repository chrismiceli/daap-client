package org.mult.daap;

/**
 * RecyclerViews do not have native support for OnItemClickListeners.
 * This allows click handlers
 *
 * @param <T>
 */
interface RecyclerOnItemClickListener<T> {
    /**
     * Handles a click of an item from a list of data
     *
     * @param item the item in the list that was clicked
     */
    void onItemClick(T item);
}
