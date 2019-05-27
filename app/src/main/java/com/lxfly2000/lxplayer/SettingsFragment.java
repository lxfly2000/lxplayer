package com.lxfly2000.lxplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    public static final String vdDoubleClickDelta="500";
    private Preference.OnPreferenceChangeListener extraPrefListener;
    SharedPreferences perfApp;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_application);
        perfApp=getContext().getSharedPreferences(SettingsActivity.appIdentifier,Context.MODE_PRIVATE);
        EditTextPreference prefDoubleClick=(EditTextPreference)findPreference(getString(R.string.key_double_click_delta));
        prefDoubleClick.setText(perfApp.getString(getString(R.string.key_double_click_delta),vdDoubleClickDelta));
        prefDoubleClick.setSummary(prefDoubleClick.getText());
        prefDoubleClick.setOnPreferenceChangeListener((preference, o) -> {
            preference.setSummary((String)o);
            perfApp.edit().putString(getString(R.string.key_double_click_delta),(String)o).apply();
            if(extraPrefListener!=null)
                return extraPrefListener.onPreferenceChange(preference,o);
            return true;
        });
    }

    public void SetExtraChangeListener(Preference.OnPreferenceChangeListener listener){
        extraPrefListener=listener;
    }
}
