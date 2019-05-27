package com.lxfly2000.lxplayer;

import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    public static final String appIdentifier="lxplayer";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsFragment fragment=new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,fragment).commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragment.SetExtraChangeListener(((preference, o) -> {
            Toast.makeText(SettingsActivity.this,R.string.message_settings_saved,Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            return true;
        }));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:finish();return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
