package org.mult.daap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

    private ListPreference fontSizePref;
    private SharedPreferences mPrefs;
    private CheckBoxPreference scrobblerPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.layout.preferences_screen);
        fontSizePref = (ListPreference) getPreferenceScreen().findPreference(
                "font_pref");
        scrobblerPref = (CheckBoxPreference) getPreferenceScreen().findPreference(
        "scrobbler_pref");
        fontSizePref
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        fontSizePref.setValue((String) newValue);
                        SharedPreferences.Editor ed = mPrefs.edit();
                        ed.putInt("font_size",
                                Integer.valueOf((String) newValue));
                        ed.apply();
                        return false;
                    }
                });
        scrobblerPref
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        scrobblerPref.setChecked((Boolean) newValue);
                        SharedPreferences.Editor ed = mPrefs.edit();
                        ed.putBoolean("scrobbler_pref",
                                Boolean.valueOf((Boolean) newValue));
                        ed.apply();
                        return false;
                    }
                });

    }
}