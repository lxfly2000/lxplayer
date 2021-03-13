package com.lxfly2000.lxplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    private Preference.OnPreferenceChangeListener extraPrefListener;
    SharedPreferences perfApp;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_application);
        perfApp=getContext().getSharedPreferences(SettingsActivity.appIdentifier,Context.MODE_PRIVATE);
        CheckBoxPreference prefEnableLineControl =(CheckBoxPreference) findPreference(getString(R.string.key_enable_line_control));
        prefEnableLineControl.setChecked(perfApp.getBoolean(getString(R.string.key_enable_line_control),true));
        prefEnableLineControl.setOnPreferenceChangeListener((preference, o) -> {
            perfApp.edit().putBoolean(getString(R.string.key_enable_line_control),(Boolean) o).apply();
            if(extraPrefListener!=null)
                return extraPrefListener.onPreferenceChange(preference,o);
            return true;
        });
    }

    public void SetExtraChangeListener(Preference.OnPreferenceChangeListener listener){
        extraPrefListener=listener;
    }
}
