package org.mult.daap;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import org.mult.daap.background.DBAdapter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerEditorActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    public class CursorPreferenceHack implements SharedPreferences {
        protected final String table;
        protected final long id;

        protected final Map<String, String> values = new HashMap<>();

        public CursorPreferenceHack(String table, long id) {
            this.table = table;
            this.id = id;

            cacheValues();
        }

        protected final void cacheValues() {
            // fill a cursor and cache the values locally
            // this makes sure we dont have any floating cursor to dispose later
            db.open();
            Cursor cursor = db.getServer((int) id);

            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String key = cursor.getColumnName(i);
                    String value = cursor.getString(i);
                    values.put(key, value);
                }
            }
            cursor.close();
            db.close();
        }

        public boolean contains(String key) {
            return values.containsKey(key);
        }

        public class Editor implements SharedPreferences.Editor {

            private ContentValues update = new ContentValues();

            public SharedPreferences.Editor clear() {
                update = new ContentValues();
                return this;
            }

            public boolean commit() {
                db.open();
                db.updateServer(update, (int) id);
                db.close();

                // make sure we refresh the parent cached values
                cacheValues();

                // and update any listeners
                for (OnSharedPreferenceChangeListener listener : listeners) {
                    listener.onSharedPreferenceChanged(
                            CursorPreferenceHack.this, null);
                }

                return true;
            }

            public android.content.SharedPreferences.Editor putBoolean(
                    String key, boolean value) {
                return this.putString(key, Boolean.toString(value));
            }

            public android.content.SharedPreferences.Editor putFloat(
                    String key, float value) {
                return this.putString(key, Float.toString(value));
            }

            public android.content.SharedPreferences.Editor putInt(String key,
                                                                   int value) {
                return this.putString(key, Integer.toString(value));
            }

            public android.content.SharedPreferences.Editor putLong(String key,
                                                                    long value) {
                return this.putString(key, Long.toString(value));
            }

            public android.content.SharedPreferences.Editor putString(
                    String key, String value) {
                update.put(key, value);
                return this;
            }

            public android.content.SharedPreferences.Editor remove(String key) {
                update.remove(key);
                return this;
            }

            // Gingerbread compatibility
            public void apply() {
                commit();
            }

            public android.content.SharedPreferences.Editor putStringSet(
                    String arg0, Set<String> arg1) {
                // TODO Auto-generated method stub
                return null;
            }

        }

        public Editor edit() {
            // Log.d(this.getClass().toString(), "edit()");
            return new Editor();
        }

        public Map<String, ?> getAll() {
            return values;
        }

        public boolean getBoolean(String key, boolean defValue) {
            return this.getString(key, Boolean.toString(defValue)).equals("1");
        }

        public float getFloat(String key, float defValue) {
            return Float.parseFloat(this.getString(key, Float.toString(defValue)));
        }

        public int getInt(String key, int defValue) {
            return Integer.parseInt(this.getString(key,
                    Integer.toString(defValue)));
        }

        public long getLong(String key, long defValue) {
            return Long.parseLong(this.getString(key, Long.toString(defValue)));
        }

        public String getString(String key, String defValue) {
            // Log.d(this.getClass().toString(),
            // String.format("getString(key=%s, defValue=%s)", key, defValue));
            if (!values.containsKey(key))
                return defValue;
            return values.get(key);
        }

        protected final List<OnSharedPreferenceChangeListener> listeners = new LinkedList<>();

        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            listeners.add(listener);
        }

        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            listeners.remove(listener);
        }

        public Set<String> getStringSet(String arg0, Set<String> arg1) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return this.pref;
    }

    protected static final String TAG = ServerEditorActivity.class.getName();
    protected DBAdapter db = null;
    private CursorPreferenceHack pref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        long rowId = this.getIntent().getIntExtra(Intent.EXTRA_TITLE, -1);
        this.db = new DBAdapter(this);
        this.pref = new CursorPreferenceHack(DBAdapter.DATABASE_TABLE, rowId);
        this.pref.registerOnSharedPreferenceChangeListener(this);
        this.addPreferencesFromResource(R.xml.server_prefs);
        this.updateSummaries();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (this.db == null)
            this.db = new DBAdapter(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (this.db != null) {
            this.db.close();
            this.db = null;
        }
    }

    private void updateSummaries() {
        // for all text preferences, set hint as current database value
        for (String key : this.pref.values.keySet()) {
            Preference pref = this.findPreference(key);
            if (pref == null)
                continue;
            if (pref instanceof CheckBoxPreference)
                continue;
            CharSequence value = this.pref.getString(key, "");

            if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                int entryIndex = listPref.findIndexOfValue((String) value);
                if (entryIndex >= 0)
                    value = listPref.getEntries()[entryIndex];
            }
            pref.setSummary(value);
        }

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // update values on changed preference
        this.updateSummaries();

    }

}