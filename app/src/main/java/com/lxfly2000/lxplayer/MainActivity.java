package com.lxfly2000.lxplayer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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
    private final TimeInfo timeInfo=new TimeInfo();
    private Timer seekTimer=null;
    private boolean seekBarIsSeeking=false;
    private boolean listIsLoading=false;
    private static final String keyKeepSpeed="keep_speed",keyKeepTime="keep_time",keyKeepPitch="keep_pitch";
    private static final int requestCodeReadStorage=0,requestCodeReadAudio=1,requestCodeReadVideo=2,requestCodeNotify=3;
    private SharedPreferences playerPreferences;

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
        if(BuildConfig.DEBUG){
            setTitle(getTitle()+" (Debug)");
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(mainClickListener);

        textTitle = findViewById(R.id.textViewTitle);
        textTime = findViewById(R.id.textViewTime);
        buttonPlay = findViewById(R.id.imageButtonPlay);
        buttonPlay.setOnClickListener(mainClickListener);
        buttonForward = findViewById(R.id.imageButtonForward);
        buttonForward.setOnClickListener(mainClickListener);
        buttonBackward = findViewById(R.id.imageButtonBackward);
        buttonBackward.setOnClickListener(mainClickListener);
        imageView = findViewById(R.id.imageView);
        toggleLoop = findViewById(R.id.toggleLoop);
        toggleLoop.setOnClickListener(mainClickListener);
        toggleRandom = findViewById(R.id.toggleRandom);
        toggleRandom.setOnClickListener(mainClickListener);
        seekTime = findViewById(R.id.seekBar);
        seekTime.setOnSeekBarChangeListener(seekListener);
        dbHelper = ListDataHelper.getInstance(getApplicationContext());
        playerPreferences = getSharedPreferences(SettingsActivity.appIdentifier,MODE_PRIVATE);

        AppInit(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0;i<grantResults.length;i++){
            if(grantResults[i]!=PackageManager.PERMISSION_GRANTED){
                AppInit(false);
                return;
            }
        }
        AppInit(true);
    }

    private void AppInit(boolean needRequestPermissions){
        //检查权限设置
        if(Build.VERSION.SDK_INT<=32&&checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(needRequestPermissions){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},requestCodeReadStorage);
            }else {
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.app_name);
                about.setMessage(R.string.message_error_permission_exstorage);
                about.setPositiveButton(android.R.string.ok, null);
                about.setOnDismissListener(dialogInterface -> ExitApplication(true));
                about.show();
            }
            return;
        }
        if(Build.VERSION.SDK_INT>32&&checkCallingOrSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            if(needRequestPermissions){
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO},requestCodeReadAudio);
            }else {
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.app_name);
                about.setMessage(R.string.message_error_permission_exstorage);
                about.setPositiveButton(android.R.string.ok, null);
                about.setOnDismissListener(dialogInterface -> ExitApplication(true));
                about.show();
            }
            return;
        }
        if(Build.VERSION.SDK_INT>32&&checkCallingOrSelfPermission(Manifest.permission.READ_MEDIA_VIDEO)!=PackageManager.PERMISSION_GRANTED){
            if(needRequestPermissions){
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO},requestCodeReadVideo);
            }else {
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.app_name);
                about.setMessage(R.string.message_error_permission_exstorage);
                about.setPositiveButton(android.R.string.ok, null);
                about.setOnDismissListener(dialogInterface -> ExitApplication(true));
                about.show();
            }
            return;
        }
        if(Build.VERSION.SDK_INT>32&&checkCallingOrSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            if(needRequestPermissions){
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},requestCodeNotify);
            }else {
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.app_name);
                about.setMessage(R.string.message_error_permission_notify);
                about.setPositiveButton(android.R.string.ok, null);
                about.setOnDismissListener(dialogInterface -> ExitApplication(true));
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
        IntentFilter fiPlayer=new IntentFilter();
        fiPlayer.addAction(PlayerService.ACTION_UPDATE_SELECTED_INDEX);
        fiPlayer.addAction(PlayerService.ACTION_UPDATE_BUTTON_PLAY);
        registerReceiver(playerReceiver,fiPlayer,RECEIVER_EXPORTED);

        //注册通知栏广播接收器
        registerReceiver(notificationReceiver,new IntentFilter(PlayerService.ACTION_UPDATE_MAIN_INTERFACE),RECEIVER_EXPORTED);

        //注册检查更新广播接收器
        registerReceiver(checkUpdateReceiver,new IntentFilter(ACTION_CHECK_UPDATE_RESULT),RECEIVER_EXPORTED);
        //检测更新
        CheckForUpdate(true);
    }

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
                    playerService.SetPlayIndex(playerPreferences.getInt(PlayerService.keyLastPlayIndex, -1));
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
            if(musicIndex==-1){
                musicIndex=playerService.GetListCurrentPos();
                if(musicIndex>=dbHelper.GetDataCount())
                    return;
            }
            textTitle.setText(dbHelper.GetTitleByIndex(musicIndex));
            imageView.setImageBitmap(ArtworkUtils.getArtwork(getApplicationContext(), dbHelper.GetTitleByIndex(musicIndex),
                    dbHelper.GetIdByIndex(musicIndex), dbHelper.GetAlbumIdByIndex(musicIndex), true));
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
                .setNeutralButton(R.string.button_restore_speed, (dialogInterface, i) -> {
                    playerService.SetPlaybackSpeed(1.0f);
                    menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
                })
                .setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    playerService.SetPlaybackSpeed((float)(Math.pow(2,(seekSetSpeed.getProgress()-accuracy)/accuracy)));
                    menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
                }).show();
        seekSetSpeed= dlg.findViewById(R.id.seekSetSpeed);
        seekSetSpeed.setMax((int)(2*accuracy));
        final TextView textSpeed= dlg.findViewById(R.id.textSpeed);
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
        //因为onProgressChanged只有在值变为不一样的数值时才会被调用因此用下面两句使onProgressChanged能够被调用
        seekSetSpeed.setProgress(0);
        seekSetSpeed.setProgress((int)(2*accuracy));
        //Sp=2^((Pg-100)/100)
        //Pg=log(2,Sp)*100+100
        seekSetSpeed.setProgress((int)(Math.log(playerService.GetPlaybackSpeed())/Math.log(2)*accuracy+accuracy));
    }

    private void GotoSettings(){
        startActivityForResult(new Intent(this,SettingsActivity.class),R.xml.pref_application&0xFFFF);
    }

    private void gotoPlaylist(){
        startActivityForResult(new Intent(this,PlaylistActivity.class),R.layout.activity_playlist&0xFFFF);
    }

    private final View.OnClickListener mainClickListener= view -> {
        switch (view.getId()){
            case R.id.fab:gotoPlaylist();return;
            case R.id.imageButtonPlay:OnButtonPlay(false);return;
            case R.id.imageButtonBackward:playerService.OnChangeMusic(false);return;
            case R.id.imageButtonForward:playerService.OnChangeMusic(true);return;
            case R.id.toggleLoop:OnSwitchLoop();return;
            case R.id.toggleRandom:OnSwitchRandom();return;
        }
        Snackbar.make(view,R.string.message_unknownOperation,Snackbar.LENGTH_SHORT).show();
    };

    private final SeekBar.OnSeekBarChangeListener seekListener=new SeekBar.OnSeekBarChangeListener() {
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
                    PlayerPlayMusic(data.getIntExtra(PlayerService.keySelectedIndex,0));
                }
                break;
        }
    }

    private void PlayerPlayMusic(int musicIndex){
        playerService.SetPlayIndex(musicIndex);
        if(dbHelper.GetDataCount()==0)
            return;
        playerService.Play();
        UpdateInterfaces(-1);
        SetTimerOn(true);
    }

    public void OnButtonPlay(boolean stop){
        if(stop||playerService.IsPlaying()){
            if(playerService.GetListCurrentPos()>=dbHelper.GetDataCount()){
                playerService.SetPlayIndex(0);
            }
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
        about.setNeutralButton(R.string.button_visitUrl, (dialogInterface, i) -> {
            Intent intentOpenUrl=new Intent(Intent.ACTION_VIEW);
            intentOpenUrl.setData(Uri.parse(getString(R.string.url_author)));
            startActivity(intentOpenUrl);
        });
        about.show();
    }

    private void OnChangePlaybackSpeed(boolean isSpeedUp){
        playerService.ChangePlaybackSpeed(isSpeedUp);
        menuKeepSpeed.setTitle(String.format(getString(R.string.menu_keep_speed),playerService.GetPlaybackSpeed()));
    }

    private BroadcastReceiver playerReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(PlayerService.ACTION_UPDATE_BUTTON_PLAY.equals(intent.getAction())) {
                SetTimerOn(playerService.IsPlaying());
                UpdateInterfaces(-1);
            }else if (PlayerService.ACTION_UPDATE_SELECTED_INDEX.equals(intent.getAction())){
                UpdateInterfaces(intent.getIntExtra(PlayerService.keySelectedIndex,0));
            }
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

    static class SeekHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case R.id.seekBar:
                    if(context!=null){
                        ((MainActivity)context).UpdateSeekBar();
                    }
                    break;
            }
            super.handleMessage(msg);
        }

        private Context context;
        public void SetContext(Context ctx){
            context=ctx;
        }
    }

    private final SeekHandler handler= new SeekHandler();

    class SeekTimerTask extends TimerTask{
        @Override
        public void run() {
            Message msg=new Message();
            msg.what=R.id.seekBar;
            handler.SetContext(MainActivity.this);
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
                msgBox.setIcon(R.drawable.baseline_info_24);
                msgBox.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    Intent intent1 =new Intent(Intent.ACTION_VIEW);
                    intent1.setData(Uri.parse(getString(R.string.url_author)));
                    startActivity(intent1);
                });
                msgBox.setNegativeButton(android.R.string.cancel,null);
            }else if (onlyReportNewVersion){
                return;
            }else if (updateChecker.IsError()){
                msgBox.setMessage(R.string.error_check_update);
                msgBox.setIcon(R.drawable.baseline_warning_24);
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