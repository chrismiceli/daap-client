package org.mult.daap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
    private ListPreference fontSizePref;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.layout.preferences_screen);
        fontSizePref = (ListPreference) getPreferenceScreen().findPreference(
                "font_pref");
        fontSizePref
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        fontSizePref.setValue((String) newValue);
                        SharedPreferences.Editor ed = mPrefs.edit();
                        ed.putInt("font_size",
                                Integer.valueOf((String) newValue));
                        ed.commit();
                        return false;
                    }
                });
    }
}