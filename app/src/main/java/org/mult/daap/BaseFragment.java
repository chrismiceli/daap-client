package org.mult.daap;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {
    protected static final int MENU_PLAY_QUEUE = 1;
    protected static final int MENU_VIEW_QUEUE = 2;
    protected static final int MENU_SEARCH = 3;
    protected static final int CONTEXT_PLAY_ALBUM = 4;
    protected static final int CONTEXT_QUEUE = 5;

    public static final String PLAYLIST_ID_BUNDLE_KEY = "__PLAYLIST_ID__";
    public static final String ITEM_MODE_KEY = "__ITEM_MODE__";
    public static final int ITEM_MODE_ALBUM = 0;
    public static final int ITEM_MODE_ARTIST = 1;

    @Override
    public void onStart() {
        super.onStart();
        ((DrawerActivity) getActivity()).setSearchRequestedCallback(new DrawerActivity.SearchRequestedCallback() {
            @Override
            public void onSearchRequested() {
                Contents.searchResult = null;
                getActivity().startSearch(null, false, null, false);
            }
        });
    }

    @Override
    public void onStop() {
        ((DrawerActivity) getActivity()).setSearchRequestedCallback(null);
        super.onStop();
    }
}