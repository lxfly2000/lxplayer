package com.lxfly2000.lxplayer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private ListDataHelper dbHelper;
    ServiceConnection mConnection;
    private PlayerService playerService=null;
    private TextView textTitle,textTime;
    ImageButton buttonPlay,buttonForward,buttonBackward;
    private ImageView imageView;
    private ToggleButton toggleLoop,toggleRandom;
    private SeekBar seekTime;
    FloatingActionButton fab;
    private MenuItem menuKeepSpeed,menuKeepTime,menuKeepPitch;
    private boolean serviceIsBound=false;
    private boolean needLoadPreferences =false;
    private TimeInfo timeInfo=new TimeInfo();
    private Timer seekTimer=null;
    private boolean seekBarIsSeeking=false;
    private boolean listIsLoading=false;
    private MediaSession lineControlSession;
    private long lastMediaButtonTime=0;
    private long doubleMediaButtonTime;
    private static final String keyKeepSpeed="keep_speed",keyKeepTime="keep_time",keyKeepPitch="keep_pitch";
    private static final String keyLastPlayIndex="play_index";
    private static final int requestCodeReadStorage=0;
    private SharedPreferences playerPreferences;

    private long GetDoubleMediaButtonTime(){
        return getSharedPreferences(SettingsActivity.appIdentifier,MODE_PRIVATE).getLong(SettingsActivity.keyDoubleClickDelta,
                SettingsActivity.vdDoubleClickDelta);
    }

    private boolean IsServiceStarted(String serviceName){
        ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo eachService:manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(eachService.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(mainClickListener);

        textTitle = (TextView) findViewById(R.id.textViewTitle);
        textTime = (TextView) findViewById(R.id.textViewTime);
        buttonPlay = (ImageButton) findViewById(R.id.imageButtonPlay);
        buttonPlay.setOnClickListener(mainClickListener);
        buttonForward = (ImageButton) findViewById(R.id.imageButtonForward);
        buttonForward.setOnClickListener(mainClickListener);
        buttonBackward = (ImageButton) findViewById(R.id.imageButtonBackward);
        buttonBackward.setOnClickListener(mainClickListener);
        imageView = (ImageView) findViewById(R.id.imageView);
        toggleLoop = (ToggleButton) findViewById(R.id.toggleLoop);
        toggleLoop.setOnClickListener(mainClickListener);
        toggleRandom = (ToggleButton) findViewById(R.id.toggleRandom);
        toggleRandom.setOnClickListener(mainClickListener);
        seekTime = (SeekBar) findViewById(R.id.seekBar);
        seekTime.setOnSeekBarChangeListener(seekListener);
        dbHelper = ListDataHelper.getInstance(getApplicationContext());
        playerPreferences = getPreferences(MODE_PRIVATE);
        doubleMediaButtonTime = GetDoubleMediaButtonTime();

        AppInit(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppInit(false);
    }

    private void AppInit(boolean needRequestPermissions){
        //检查权限设置
        if(checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(needRequestPermissions){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},requestCodeReadStorage);
            }else {
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.app_name);
                about.setMessage(R.string.message_error_permission_exstorage);
                about.setPositiveButton(android.R.string.ok, null);
                about.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        ExitApplication(true);
                    }
                });
                about.show();
            }
            return;
        }

        //启动服务，初始化播放列表
        if(dbHelper.GetDataCount()==0){
            listIsLoading=true;
            Intent intent=new Intent(this,PlaylistActivity.class);
            intent.putExtra("ShouldFinish",true);
            startActivityForResult(intent,R.layout.activity_playlist&0xFFFF);
        }else {
            CanBindService();
        }
        //注册播放列表广播接收器
        registerReceiver(playerReceiver,new IntentFilter(getPackageName()));

        //注册线控回调
        lineControlSession=new MediaSession(this,getString(R.string.app_name));
        lineControlSession.setCallback(lineControlCallback);
        lineControlSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        lineControlSession.setActive(true);

        //注册通知栏广播接收器
        IntentFilter fiNotification=new IntentFilter();
        fiNotification.addAction(PlayerService.ACTION_UPDATE_MAIN_INTERFACE);
        registerReceiver(notificationReceiver,fiNotification);

        //注册检查更新广播接收器
        IntentFilter fiUpdate=new IntentFilter();
        fiUpdate.addAction(ACTION_CHECK_UPDATE_RESULT);
        registerReceiver(checkUpdateReceiver,fiUpdate);
        //检测更新
        CheckForUpdate(true);
    }

    private MediaSession.Callback lineControlCallback=new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            if(Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())){
                KeyEvent event=mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if(event.getAction()==KeyEvent.ACTION_UP) {
                    if(event.getEventTime()-lastMediaButtonTime<doubleMediaButtonTime) {
                        OnButtonPlay(false);
                        OnChangeMusic(true);
                        Toast.makeText(getApplicationContext(),R.string.button_forward,Toast.LENGTH_LONG).show();
                    }else if(event.getEventTime()-event.getDownTime()>doubleMediaButtonTime) {
                        OnChangeMusic(false);
                        Toast.makeText(getApplicationContext(),R.string.button_backward,Toast.LENGTH_LONG).show();
                    }else{
                        OnButtonPlay(false);
                        lastMediaButtonTime=event.getEventTime();
                    }
                }
                return true;
            }
            return super.onMediaButtonEvent(mediaButtonIntent);
        }
    };

    private BroadcastReceiver notificationReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateInterfaces(-1);
        }
    };

    private void CanBindService(){
        mConnection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                playerService=((PlayerService.LocalBinder)iBinder).getService();
                if(needLoadPreferences) {
                    playerService.SetKeepSpeed(playerPreferences.getBoolean(keyKeepSpeed, false));
                    playerService.SetPlayIndex(playerPreferences.getInt(keyLastPlayIndex, -1));
                }
                UpdateInterfaces(-1);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                playerService=null;
            }
        };
        if(!serviceIsBound) {
            Intent intent=new Intent(this,PlayerService.class);
            if(!IsServiceStarted(getPackageName()+".PlayerService")) {
                startService(intent);
                needLoadPreferences=true;
            }
            bindService(intent, mConnection, BIND_AUTO_CREATE);

            serviceIsBound = true;
        }
    }

    /**
     * 更新界面显示
     * @param musicIndex -1为自动查找，否则使用指定值。
     */
    private void UpdateInterfaces(int musicIndex){
        toggleLoop.setChecked(playerService.IsLoopOn());
        toggleRandom.setChecked(playerService.IsRandomOn());
        if(playerService.IsPlaying())buttonPlay.setImageResource(android.R.drawable.ic_media_pause);
        else buttonPlay.setImageResource(android.R.drawable.ic_media_play);
        timeInfo.SetTotal(playerService.GetPlayingTotalTime_Ms());
        seekTime.setMax(playerService.GetPlayingTotalTime_Ms());
        UpdateSeekBar();
        if(playerService.IsPlaying())SetTimerOn(true);
        if(dbHelper.GetDataCount()>0) {
            textTitle.setText(dbHelper.GetTitleByIndex(musicIndex == -1 ? playerService.GetListCurrentPos() : musicIndex));
            imageView.setImageBitmap(ArtworkUtils.getArtwork(getApplicationContext(), dbHelper.GetTitleByIndex(playerService.GetListCurrentPos()),
                    dbHelper.GetIdByIndex(playerService.GetListCurrentPos()), dbHelper.GetAlbumIdByIndex(playerService.GetListCurrentPos()), true));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //http://blog.csdn.net/maojudong/article/details/7010210
        menuKeepSpeed=menu.findItem(R.id.action_keep_speed);
        menuKeepTime=menu.findItem(R.id.action_change_speed_keep_time);
        menuKeepPitch=menu.findItem(R.id.action_change_speed_keep_pitch);
        if(playerService==null) {
            menuKeepSpeed.setChecked(false);
            menuKeepTime.setChecked(false);
            menuKeepPitch.setChecked(false);
            menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed), 1.0f));
        }else {
            menuKeepSpeed.setChecked(playerService.IsKeepSpeed());
            menuKeepTime.setChecked(playerService.IsKeepTime());
            menuKeepPitch.setChecked(playerService.IsKeepPitch());
            menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            //noinspection SimplifiableIfStatement
            case R.id.action_goto_playlist:gotoPlaylist();return true;
            case R.id.action_stop:OnStopPlayer();return true;
            case R.id.action_about:OnAbout();return true;
            case R.id.action_speedUp:OnChangePlaybackSpeed(true);return true;
            case R.id.action_speedDown:OnChangePlaybackSpeed(false);return true;
            case R.id.action_exitApp:ExitApplication(false);return true;
            case R.id.action_keep_speed:OnToggleKeepSpeed();return true;
            case R.id.action_change_speed_keep_time:OnToggleKeepTime();return true;
            case R.id.action_change_speed_keep_pitch:OnToggleKeepPitch();return true;
            case R.id.action_check_update:CheckForUpdate(false);return true;
            case R.id.action_settings:GotoSettings();return true;
            case R.id.action_set_speed:SetSpeedDialog();return true;
        }

        return super.onOptionsItemSelected(item);
    }

    SeekBar seekSetSpeed;
    private void SetSpeedDialog(){
        final float accuracy=100.0f;
        AlertDialog dlg=new AlertDialog.Builder(this)
                .setTitle(R.string.menu_custom_playback_speed)
                .setView(R.layout.dialog_set_speed)
                .setNeutralButton(R.string.button_restore_speed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        playerService.SetPlaybackSpeed(1.0f);
                        menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
                    }
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        playerService.SetPlaybackSpeed((float)(Math.pow(2,(seekSetSpeed.getProgress()-accuracy)/accuracy)));
                        menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
                    }
                }).show();
        seekSetSpeed=(SeekBar)dlg.findViewById(R.id.seekSetSpeed);
        seekSetSpeed.setMax((int)(2*accuracy));
        final TextView textSpeed=(TextView)dlg.findViewById(R.id.textSpeed);
        seekSetSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textSpeed.setText(String.format("%.3f",Math.pow(2,(seekBar.getProgress()-accuracy)/accuracy)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Nothing here.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Nothing here.
            }
        });
        //Sp=2^((Pg-100)/100)
        //Pg=log(2,Sp)*100+100
        seekSetSpeed.setProgress((int)(Math.log(playerService.GetPlaybackSpeed())/Math.log(2)*accuracy+accuracy));
    }

    private void GotoSettings(){
        startActivityForResult(new Intent(this,SettingsActivity.class),R.layout.activity_settings&0xFFFF);
    }

    private void gotoPlaylist(){
        startActivityForResult(new Intent(this,PlaylistActivity.class),R.layout.activity_playlist&0xFFFF);
    }

    private View.OnClickListener mainClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.fab:gotoPlaylist();return;
                case R.id.imageButtonPlay:OnButtonPlay(false);return;
                case R.id.imageButtonBackward:OnChangeMusic(false);return;
                case R.id.imageButtonForward:OnChangeMusic(true);return;
                case R.id.toggleLoop:OnSwitchLoop();return;
                case R.id.toggleRandom:OnSwitchRandom();return;
            }
            Snackbar.make(view,R.string.message_unknownOperation,Snackbar.LENGTH_SHORT).show();
        }
    };

    private SeekBar.OnSeekBarChangeListener seekListener=new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            textTime.setText(timeInfo.GetFormattedString(seekBar.getProgress(),getString(R.string.label_playtime)));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBarIsSeeking=true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBarIsSeeking=false;
            playerService.SetPlayingPos_Ms(seekTime.getProgress());
        }
    };

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        switch (requestCode){
            case R.layout.activity_playlist&0xFFFF:
                UpdateCounts();
                if(listIsLoading) {
                    listIsLoading = false;
                    CanBindService();
                }
                if(resultCode==RESULT_OK){
                    PlayerPlayMusic(data.getIntExtra("SelectedIndex",0));
                }
                break;
            case R.layout.activity_settings&0xFFFF:
                if(resultCode==RESULT_OK) {
                    doubleMediaButtonTime = GetDoubleMediaButtonTime();
                }
                break;
        }
    }

    private void PlayerPlayMusic(int musicIndex){
        if(dbHelper.GetDataCount()==0)return;
        playerService.SetPlayIndex(musicIndex);
        playerService.Play();
        UpdateInterfaces(-1);
        SetTimerOn(true);
        OnCurrentPlayingChanged();
    }

    private void OnButtonPlay(boolean stop){
        if(stop||playerService.IsPlaying()){
            playerService.Pause(stop);
            UpdateInterfaces(-1);
            SetTimerOn(false);
        }else {
            PlayerPlayMusic(-1);
        }
    }

    private void OnSwitchLoop(){
        playerService.SetLoop(toggleLoop.isChecked());
    }

    private void OnSwitchRandom(){
        playerService.SetRandom(toggleRandom.isChecked());
        if(playerService.IsRandomOn()&&playerService.IsLoopOn())
            Snackbar.make(toggleRandom,R.string.message_error_randomOnLoop,Snackbar.LENGTH_SHORT).show();
    }

    private void UpdateCounts(){
        dbHelper.UpdateDataCounts();
    }

    private void OnChangeMusic(boolean isForward){
        playerService.SetNextMusic(isForward);
        OnCurrentPlayingChanged();
    }

    private void OnStopPlayer(){
        OnButtonPlay(true);
    }

    private void OnAbout(){
        String infoAbout=String.format(getString(R.string.message_about),
                BuildConfig.VERSION_NAME,BuildConfig.BUILD_DATE,getString(R.string.url_author));
        AlertDialog.Builder about=new AlertDialog.Builder(this);
        about.setTitle(R.string.app_name);
        about.setMessage(infoAbout);
        about.setPositiveButton(android.R.string.ok,null);
        about.setNeutralButton(R.string.button_visitUrl, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentOpenUrl=new Intent(Intent.ACTION_VIEW);
                intentOpenUrl.setData(Uri.parse(getString(R.string.url_author)));
                startActivity(intentOpenUrl);
            }
        });
        about.show();
    }

    private void OnChangePlaybackSpeed(boolean isSpeedUp){
        playerService.ChangePlaybackSpeed(isSpeedUp);
        menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
    }

    private void OnCurrentPlayingChanged(){
        playerPreferences.edit().putInt(keyLastPlayIndex,playerService.GetListCurrentPos()).apply();
    }

    private BroadcastReceiver playerReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateInterfaces(intent.getIntExtra("SelectedIndex",0));
            OnCurrentPlayingChanged();
        }
    };

    @Override
    public void onPause(){
        SetTimerOn(false);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(playerService!=null)
            SetTimerOn(playerService.IsPlaying());
    }

    private void SetTimerOn(boolean run){
        if(!run){
            if(seekTimer!=null) {
                seekTimer.cancel();
                seekTimer.purge();
                seekTimer = null;
            }
        }else {
            if(seekTimer==null) {
                seekTimer = new Timer();
                seekTimer.schedule(new SeekTimerTask(), 0, 1000);
            }
        }
    }

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case R.id.seekBar:
                    UpdateSeekBar();break;
            }
            super.handleMessage(msg);
        }
    };

    class SeekTimerTask extends TimerTask{
        @Override
        public void run() {
            Message msg=new Message();
            msg.what=R.id.seekBar;
            handler.sendMessage(msg);
        }
    }

    private void UpdateSeekBar(){
        if(!seekBarIsSeeking){
            seekTime.setProgress(playerService.GetPlayingPos_Ms());
            textTime.setText(timeInfo.GetFormattedString(playerService.GetPlayingPos_Ms(),getString(R.string.label_playtime)));
        }
    }

    private void ExitApplication(boolean activityOnly){
        if(!activityOnly) {
            OnStopPlayer();
            if(notificationReceiver!=null){
                unregisterReceiver(notificationReceiver);
                notificationReceiver=null;
            }
            if(playerReceiver!=null) {
                unregisterReceiver(playerReceiver);
                playerReceiver=null;
            }
            if(checkUpdateReceiver!=null){
                unregisterReceiver(checkUpdateReceiver);
                checkUpdateReceiver=null;
            }
            lineControlSession.release();
            playerService.ReleaseService();
            unbindService(mConnection);
            stopService(new Intent(this, PlayerService.class));
        }
        finish();
    }

    private void OnToggleKeepSpeed(){
        playerService.SetKeepSpeed(!playerService.IsKeepSpeed());
        menuKeepSpeed.setChecked(playerService.IsKeepSpeed());
        playerPreferences.edit().putBoolean(keyKeepSpeed,playerService.IsKeepSpeed()).apply();
    }

    private void OnToggleKeepTime(){
        playerService.SetKeepTime(!playerService.IsKeepTime());
        menuKeepTime.setChecked(playerService.IsKeepTime());
        playerPreferences.edit().putBoolean(keyKeepTime,playerService.IsKeepTime()).apply();
    }

    private void OnToggleKeepPitch(){
        playerService.SetKeepPitch(!playerService.IsKeepPitch());
        menuKeepPitch.setChecked(playerService.IsKeepPitch());
        playerPreferences.edit().putBoolean(keyKeepPitch,playerService.IsKeepPitch()).apply();
    }

    private static final String INTENT_EXTRA_UPDATE_ONLY="onlyUpdate";
    private static final String INTENT_EXTRA_FOUND_UPDATE="foundUpdate";
    private static final String ACTION_CHECK_UPDATE_RESULT=BuildConfig.APPLICATION_ID+".CheckUpdateResult";
    private UpdateChecker updateChecker=null;

    private BroadcastReceiver checkUpdateReceiver=new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean onlyReportNewVersion=intent.getBooleanExtra(INTENT_EXTRA_UPDATE_ONLY,true);
            boolean foundNewVersion=intent.getBooleanExtra(INTENT_EXTRA_FOUND_UPDATE,false);
            AlertDialog.Builder msgBox=new AlertDialog.Builder(context);//这里不能用getApplicationContext.
            msgBox.setPositiveButton(android.R.string.ok,null);
            msgBox.setTitle(R.string.menu_check_update);
            if (foundNewVersion) {
                String msg = String.format(getString(R.string.message_new_version), BuildConfig.VERSION_NAME, updateChecker.GetUpdateVersionName());
                msgBox.setMessage(msg);
                msgBox.setIcon(android.R.drawable.ic_dialog_info);
                msgBox.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent=new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(getString(R.string.url_author)));
                        startActivity(intent);
                    }
                });
                msgBox.setNegativeButton(android.R.string.cancel,null);
            }else if (onlyReportNewVersion){
                return;
            }else if (updateChecker.IsError()){
                msgBox.setMessage(R.string.error_check_update);
                msgBox.setIcon(android.R.drawable.ic_dialog_alert);
            }else {
                msgBox.setMessage(R.string.message_no_update);
            }
            msgBox.show();
        }
    };

    private void CheckForUpdate(boolean onlyReportNewVersion) {
        if (updateChecker == null) {
            updateChecker = new UpdateChecker().SetCheckURL(getString(R.string.url_check_update));
        }
        updateChecker.SetResultHandler(new UpdateChecker.ResultHandler(this) {
            @Override
            protected void OnReceive(boolean foundNewVersion) {
                Intent intent = new Intent(ACTION_CHECK_UPDATE_RESULT);
                intent.putExtra(INTENT_EXTRA_FOUND_UPDATE, foundNewVersion);
                intent.putExtra(INTENT_EXTRA_UPDATE_ONLY, GetOnlyReportUpdate());
                HandlerSendBroadcast(intent);
            }
        }.SetOnlyReportUpdate(onlyReportNewVersion));
        if (checkCallingOrSelfPermission("android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED) {
            if (onlyReportNewVersion)
                return;
            AlertDialog.Builder msgBox = new AlertDialog.Builder(this)
                    .setTitle(R.string.menu_check_update)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(R.string.error_permission_network);
            msgBox.show();
        } else {
            updateChecker.CheckForUpdate();
        }
    }
}

class TimeInfo{
    private int total_min,total_sec;
    TimeInfo(){
        total_min=total_sec=0;
    }
    public TimeInfo(int _total_ms){
        SetTotal(_total_ms);
    }
    public void SetTotal(int _total_ms){
        _total_ms/=1000;
        total_min=_total_ms/60;
        total_sec=_total_ms%60;
    }
    public String GetFormattedString(int cur_ms,final String fmtString){
        cur_ms/=1000;
        return String.format(fmtString,cur_ms/60,cur_ms%60,total_min,total_sec);
    }
}