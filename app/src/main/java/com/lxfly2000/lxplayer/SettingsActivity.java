package com.lxfly2000.lxplayer;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private EditText editDoubleClickDelta;
    public static final String appIdentifier="lxplayer";
    public static final String keyDoubleClickDelta="double_click_delta";
    public static final long vdDoubleClickDelta=500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.buttonOk).setOnClickListener(buttonCallbacks);
        editDoubleClickDelta=(EditText)findViewById(R.id.editDoubleClickDelta);
        preferences=getSharedPreferences(appIdentifier,MODE_PRIVATE);
        LoadSettings();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:finish();return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private View.OnClickListener buttonCallbacks=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.buttonOk:SaveSettings();break;
            }
        }
    };

    private void LoadSettings(){
        editDoubleClickDelta.setText(String.valueOf(preferences.getLong(keyDoubleClickDelta,vdDoubleClickDelta)));
    }

    private void SaveSettings(){
        SharedPreferences.Editor wPreference=preferences.edit();
        String inputDelta=editDoubleClickDelta.getText().toString();
        long vDelta=vdDoubleClickDelta;
        if(inputDelta.length()>0)
            vDelta=Long.parseLong(inputDelta);
        wPreference.putLong(keyDoubleClickDelta,vDelta);
        wPreference.apply();
        Toast.makeText(this,R.string.message_settings_saved,Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }
}
