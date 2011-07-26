package org.mult.daap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.SectionIndexer;

class MyIndexerAdapter<T> extends ArrayAdapter<T> implements SectionIndexer,
      Filterable {
   ArrayList<String> myElements;
   HashMap<String, Integer> alphaIndexer;
   ArrayList<String> letterList;

   // String[] sections;

   @SuppressWarnings("unchecked")
   public MyIndexerAdapter(Context context, int textViewResourceId,
         List<T> objects) {
      super(context, textViewResourceId, objects);
      myElements = (ArrayList<String>) objects;
      // Collections.sort(myElements);
      alphaIndexer = new HashMap<String, Integer>();
      // int size = Contents.getSongList().size();
      int size = myElements.size();
      for (int i = size - 1; i >= 0; i--) {
         // String element = Contents.getStringList().get(i);
         String element = myElements.get(i);
         if (element.length() != 0) { // no album/artist
            alphaIndexer.put(element.substring(0, 1).toUpperCase(), i);
         } else {
            alphaIndexer.put(" ", i);
         }
      }
      // Set<String> letters = alphaIndexer.keySet(); // set of letters ...sets
      // (all letters)
      // Iterator<String> it = letters.iterator();
      letterList = new ArrayList<String>(alphaIndexer.keySet()); // list can be
      // sorted
      // while (it.hasNext()) {
      // String key = it.next();
      // letterList.add(key);
      // }
      Collections.sort(letterList);
      // sections = new String[letterList.size()]; // simple conversion to an
      // array of object
      // letterList.toArray(sections);
   }

   public int getPositionForSection(int section) {
      // String letter = sections[section];
      // String letter = letterList.get(section);
      return alphaIndexer.get(letterList.get(section));
   }

   public int getSectionForPosition(int position) {
      return 0;
   }

   public Object[] getSections() {
      return letterList.toArray();
      // return sections; // to string will be called each object, to display
      // the letter
   }
}