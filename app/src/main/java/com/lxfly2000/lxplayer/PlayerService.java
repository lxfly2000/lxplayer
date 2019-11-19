package com.lxfly2000.lxplayer;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.RequiresApi;

public class PlayerService extends Service {
    private MediaPlayer player=null;
    private boolean isRandom=false,isLoop=false;
    private ListDataHelper dh;
    private final IBinder mBinder=new LocalBinder();
    private int playlistCurrentPos=0;
    private boolean keepSpeed=false,keepTime=false,keepPitch=false;
    private float playbackSpeed=1.0f;
    private static final String ACTION_BACKWARD =BuildConfig.APPLICATION_ID+".Backward";
    private static final String ACTION_FORWARD =BuildConfig.APPLICATION_ID+".Forward";
    private static final String ACTION_TOGGLE_PLAY =BuildConfig.APPLICATION_ID+".TogglePlay";
    public static final String ACTION_UPDATE_MAIN_INTERFACE=BuildConfig.APPLICATION_ID+".UpdateInterface";
    private NotificationManager notificationManager;
    private int notifyId=0;
    private MediaPlayer.OnCompletionListener nextMusicListener=new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if(!IsLoopOn())SetNextMusic(true);
            Play();
            UpdateNotificationBar(false);
        }
    };

    private BroadcastReceiver notificationReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(ACTION_BACKWARD)){
                SetNextMusic(false);
            }else if(action.equals(ACTION_FORWARD)){
                SetNextMusic(true);
            }else if(action.equals(ACTION_TOGGLE_PLAY)){
                if(IsPlaying()){
                    Pause(false);
                }else {
                    Play();
                }
            }
            UpdateNotificationBar(true);
        }
    };

    public class LocalBinder extends Binder{
        PlayerService getService(){
            return PlayerService.this;
        }
    }

    public PlayerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        dh=ListDataHelper.getInstance(getApplicationContext());
        player=new MediaPlayer();
        if(dh.GetDataCount()>0)SetPlayIndex(playlistCurrentPos);
        player.setOnCompletionListener(nextMusicListener);
        IntentFilter fiNotification=new IntentFilter();
        fiNotification.addAction(ACTION_FORWARD);
        fiNotification.addAction(ACTION_BACKWARD);
        fiNotification.addAction(ACTION_TOGGLE_PLAY);
        registerReceiver(notificationReceiver,fiNotification);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        notifyId=startId;
        notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        UpdateNotificationBar(false);
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy(){
        ReleaseService();
        super.onDestroy();
    }

    public void ReleaseService(){
        if(notificationReceiver!=null) {
            unregisterReceiver(notificationReceiver);
            notificationReceiver=null;
        }
        player.stop();
        stopForeground(true);
    }

    public void SetPlayIndex(int index){
        if(index==-1)return;
        try {
            player.reset();
            player.setDataSource(dh.GetPathByIndex(index));
            player.prepare();
            playlistCurrentPos=index;
            UpdateNotificationBar(false);
        }catch (java.io.IOException e){
            //return;
        }
    }

    public void SetPlayId(int id){
        try {
            player.setDataSource(dh.GetPathById(id));
            UpdateNotificationBar(false);
        }catch (java.io.IOException e){
            //return;
        }
    }

    public void Play(){
        player.start();
        if(keepSpeed)SetPlaybackSpeed(GetPlaybackSpeed());
        UpdateNotificationBar(false);
    }

    /**
     * 暂停播放
     * @param stop 为 true 时表示停止。
     */
    public void Pause(boolean stop){
        if(IsPlaying())
            player.pause();
        if(stop)
            SetPlayingPos_Ms(0);
        UpdateNotificationBar(false);
    }

    public void SetLoop(boolean loop){
        isLoop=loop;
    }

    public void SetRandom(boolean random){
        isRandom=random;
    }

    public boolean IsPlaying(){
        return player.isPlaying();
    }

    public boolean IsLoopOn(){
        return isLoop;
    }

    public boolean IsRandomOn(){
        return isRandom;
    }

    public int GetListCurrentPos(){
        return playlistCurrentPos;
    }

    public void SetNextMusic(boolean isForward){
        if(dh.GetDataCount()==0)return;
        boolean playing=IsPlaying();//记录之前的状态，不可省略
        if(isRandom){
            SetPlayIndex((int)(Math.random()*dh.GetDataCount()));
        }else {
            if(isForward)SetPlayIndex((playlistCurrentPos+1)%dh.GetDataCount());
            else SetPlayIndex((playlistCurrentPos+dh.GetDataCount()-1)%dh.GetDataCount());
        }
        if(playing)Play();
        Intent intent=new Intent(getPackageName());
        intent.putExtra("SelectedIndex",playlistCurrentPos);
        sendBroadcast(intent);
    }

    private String notifyChannelId=BuildConfig.APPLICATION_ID;

    private void RegisterNotifyIdChannel(){
        //https://blog.csdn.net/qq_15527709/article/details/78853048
        String notifyChannelName = "LxPlayer Channel";
        NotificationChannel notificationChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(notifyChannelId,
                    notifyChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null,null);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    private void UpdateNotificationBar(boolean updateMainActivity){
        ListDataHelper dbHelper=ListDataHelper.getInstance(getApplicationContext());
        //参考：http://blog.csdn.net/yyingwei/article/details/8509402
        Intent notificationIntent=new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,0);
        Intent iBackward=new Intent(ACTION_BACKWARD);
        Intent iTogglePlay=new Intent(ACTION_TOGGLE_PLAY);
        Intent iForward=new Intent(ACTION_FORWARD);
        PendingIntent piBackward=PendingIntent.getBroadcast(this,0,iBackward,0);
        PendingIntent piTogglePlay=PendingIntent.getBroadcast(this,0,iTogglePlay,0);
        PendingIntent piForward=PendingIntent.getBroadcast(this,0,iForward,0);
        Notification.MediaStyle style=new Notification.MediaStyle()
                .setShowActionsInCompactView(1);
        Notification.Builder notifBuilder=new Notification.Builder(this);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            notifBuilder.setChannelId(notifyChannelId);
        notifBuilder.setContentText(getText(R.string.app_name));
        notifBuilder.setSmallIcon(R.mipmap.ic_launcher);
        if(dbHelper.GetDataCount()>0) {
            notifBuilder.setContentTitle(dbHelper.GetTitleByIndex(GetListCurrentPos()));
            notifBuilder.setLargeIcon(ArtworkUtils.getArtwork(getApplicationContext(), dbHelper.GetTitleByIndex(GetListCurrentPos()),
                    dbHelper.GetIdByIndex(GetListCurrentPos()), dbHelper.GetAlbumIdByIndex(GetListCurrentPos()), true));
        }
        notifBuilder.setContentIntent(pendingIntent);
        notifBuilder.addAction(android.R.drawable.ic_media_previous,getString(R.string.button_backward),piBackward);
        if(player!=null&&IsPlaying()) {
            notifBuilder.addAction(android.R.drawable.ic_media_pause, getString(R.string.button_pause), piTogglePlay);
        }else {
            notifBuilder.addAction(android.R.drawable.ic_media_play, getString(R.string.button_play), piTogglePlay);
        }
        notifBuilder.addAction(android.R.drawable.ic_media_next,getString(R.string.button_forward),piForward);
        notifBuilder.setStyle(style);
        Notification notification=notifBuilder.build();
        if(player==null) {
            RegisterNotifyIdChannel();
            startForeground(notifyId, notification);
        }else {
            notificationManager.notify(notifyId,notification);
        }
        if(updateMainActivity){
            sendBroadcast(new Intent(ACTION_UPDATE_MAIN_INTERFACE));
        }
    }

    public int GetPlayingPos_Ms(){
        return player.getCurrentPosition();
    }

    public void SetPlayingPos_Ms(int ms){
        player.seekTo(ms);
    }

    public int GetPlayingTotalTime_Ms(){
        return player.getDuration();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void SetPlaybackSpeed(float newSpeed){
        playbackSpeed=newSpeed;
        //https://stackoverflow.com/questions/10849961/speed-control-of-mediaplayer-in-android
        if(IsPlaying())
            player.setPlaybackParams(player.getPlaybackParams().setSpeed(keepTime?1.0f:playbackSpeed).setPitch(keepPitch?1.0f:playbackSpeed));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void ChangePlaybackSpeed(boolean isSpeedUp){
        if(isSpeedUp){
            SetPlaybackSpeed(Math.min(2.0f,GetPlaybackSpeed()*2.0f));
        }else {
            SetPlaybackSpeed(Math.max(0.5f,GetPlaybackSpeed()*0.5f));
        }
    }

    public float GetPlaybackSpeed(){
        return playbackSpeed;
    }

    public void SetKeepSpeed(boolean keep){
        keepSpeed=keep;
    }

    public boolean IsKeepSpeed(){
        return keepSpeed;
    }

    public void SetKeepTime(boolean keep){
        keepTime=keep;
    }

    public boolean IsKeepTime(){
        return keepTime;
    }

    public void SetKeepPitch(boolean keep){
        keepPitch=keep;
    }

    public boolean IsKeepPitch(){
        return keepPitch;
    }
}
