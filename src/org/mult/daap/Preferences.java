package org.mult.daap;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity
{
    private CheckBoxPreference saveToSDCheckBox;
    private CheckBoxPreference streamingCheckBox;
    private SharedPreferences mPrefs;
    private Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        builder = new AlertDialog.Builder(this);
        addPreferencesFromResource(R.layout.preferences_screen);
        saveToSDCheckBox = (CheckBoxPreference) getPreferenceScreen()
                .findPreference("sd_as_cache");
        streamingCheckBox = (CheckBoxPreference) getPreferenceScreen()
                .findPreference("streaming_pref");
        saveToSDCheckBox
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue)
                    {
                        if ((Boolean) newValue == true
                                && isSDPresent() == false)
                        {
                            saveToSDCheckBox.setChecked(false);
                            SharedPreferences.Editor ed = mPrefs.edit();
                            ed.putBoolean("sd_as_cache", false);
                            ed.commit();
                            builder.setTitle(R.string.error_title);
                            builder
                                    .setMessage(R.string.no_sdcard_error_message);
                            builder
                                    .setPositiveButton(android.R.string.ok,
                                            null);
                            builder.show();
                        }
                        else
                        {
                            saveToSDCheckBox.setChecked((Boolean) newValue);
                            SharedPreferences.Editor ed = mPrefs.edit();
                            ed.putBoolean("sd_as_cache", saveToSDCheckBox
                                    .isChecked());
                            ed.commit();
                        }
                        return false;
                    }
                });
        streamingCheckBox
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue)
                    {
                        streamingCheckBox.setChecked((Boolean) newValue);
                        SharedPreferences.Editor ed = mPrefs.edit();
                        ed.putBoolean("streaming_pref", streamingCheckBox
                                .isChecked());
                        ed.commit();
                        return false;
                    }
                });
    }

    public boolean isSDPresent()
    {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

}