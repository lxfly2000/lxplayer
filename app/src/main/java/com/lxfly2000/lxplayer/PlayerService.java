package com.lxfly2000.lxplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

public class PlayerService extends Service {
    private MediaPlayer player;
    private boolean isRandom=false,isLoop=false;
    private ListDataHelper dh;
    private final IBinder mBinder=new LocalBinder();
    private int playlistCurrentPos=0;
    private boolean keepSpeed=false;
    private float playbackSpeed=1.0f;
    private MediaPlayer.OnCompletionListener nextMusicListener=new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if(!IsLoopOn())SetNextMusic(true);
            Play();
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
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        //参考：http://blog.csdn.net/yyingwei/article/details/8509402
        Intent notificationIntent=new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,0);
        Notification notification=new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.message_notification))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(startId,notification);
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy(){
        ReleaseService();
        super.onDestroy();
    }

    public void ReleaseService(){
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
        }catch (java.io.IOException e){
            //return;
        }
    }

    public void SetPlayId(int id){
        try {
            player.setDataSource(dh.GetPathById(id));
        }catch (java.io.IOException e){
            //return;
        }
    }

    public void Play(){
        player.start();
        if(keepSpeed)SetPlaybackSpeed(GetPlaybackSpeed());
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
            player.setPlaybackParams(player.getPlaybackParams().setSpeed(playbackSpeed).setPitch(playbackSpeed));
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
}
