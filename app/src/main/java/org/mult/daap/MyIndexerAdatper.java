package org.mult.daap;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class MyIndexerAdapter<T> extends ArrayAdapter<T> implements SectionIndexer,
        Filterable {
    private final ArrayList<String> myElements;
    private final HashMap<String, Integer> alphaIndexer;
    private final ArrayList<String> letterList;
    private final Context vContext;

    public MyIndexerAdapter(Context context, int textViewResourceId,
            List<T> objects) {
        super(context, textViewResourceId, objects);
        vContext = context;
        myElements = (ArrayList<String>) objects;
        alphaIndexer = new HashMap<>();
        int size = myElements.size();
        for (int i = size - 1; i >= 0; i--) {
            String element = myElements.get(i);
            if (element.length() != 0) { // no album/artist
                alphaIndexer.put(element.substring(0, 1).toUpperCase(), i);
            } else {
                alphaIndexer.put(" ", i);
            }
        }
        letterList = new ArrayList<>(alphaIndexer.keySet()); // list can be
        // sorted
        Collections.sort(letterList);
    }

    @Override
    public int getCount() {
        return myElements.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = new TextView(vContext.getApplicationContext());
        tv.setTextSize(18);
        tv.setTextColor(Color.WHITE);
        tv.setText(myElements.get(position));
        return tv;
    }

    public int getPositionForSection(int section) {
        return alphaIndexer.get(letterList.get(section));
    }

    public int getSectionForPosition(int position) {
        return 0;
    }

    public Object[] getSections() {
        return letterList.toArray();
    }
}