package com.lxfly2000.lxplayer;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class PlaylistActivity extends AppCompatActivity {
    private ListDataHelper dh;
    private ListView listView;
    private int selectedListItemIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        //https://stackoverflow.com/questions/14545139/android-back-button-in-the-title-bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        listView=(ListView)findViewById(R.id.listView);
        registerForContextMenu(listView);
        listView.setOnItemClickListener((adapterView, view, position, l) -> PlaySelectedItem(position));
        listView.setOnItemLongClickListener((adapterView, view, position, l) -> {
            selectedListItemIndex=position;
            return false;
        });
        dh=ListDataHelper.getInstance(this);
        if(getIntent().getBooleanExtra("ShouldFinish",false))ReloadList(true);
        DisplayList();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        getMenuInflater().inflate(R.menu.menu_playlist_popup,menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_playlist,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_playlist_add:return OnAddFile(R.id.action_playlist_add&0xFFFF);
            case R.id.action_playlist_add_dir:return OnAddFile(R.id.action_playlist_add_dir&0xFFFF);
            case R.id.action_playlist_refresh:return OnRefresh();
            case R.id.action_playlist_clear:return OnClearList();
            case R.id.action_view_saved_playlist:return OnViewSavedPlaylist();
            case android.R.id.home:return OnBackButton();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_playlist_popup_play:PlaySelectedItem(selectedListItemIndex);return true;
            case R.id.action_playlist_popup_del:DeleteItemFromList(GetIdOfChosenItem(selectedListItemIndex));return true;
        }
        return false;
    }

    @Override
    public void onContextMenuClosed(Menu menu){
        selectedListItemIndex=0;
    }

    private void PlaySelectedItem(int index){
        Intent intent=new Intent();
        intent.putExtra("SelectedIndex",index);
        setResult(RESULT_OK,intent);
        finish();
    }

    private void DeleteItemFromList(int _id){
        dh.delId(_id);
        DisplayList();
    }


    private boolean OnBackButton(){
        finish();
        return true;
    }

    /** 添加文件
     *
     * @return 成功返回true, 否则为false
     */
    private boolean OnAddFile(int reqCode){
        //参考：http://www.bkjia.com/Androidjc/1075240.html
        //同时指定多种格式：https://blog.csdn.net/a840347/article/details/102602241
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*|video/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"audio/*","video/*"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.title_chooseFile)), reqCode);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this,R.string.message_no_filechooser,Toast.LENGTH_SHORT).show();
            return false;
        }
        DisplayList();
        return true;
    }

    private boolean ReloadList() {
        dh.DelAll();
        String[] proj = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA};
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        ContentValues values = new ContentValues();
        if (c == null) return false;
        else while (c.moveToNext()) {
            values.put(MediaStore.Audio.Media._ID, c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            values.put(MediaStore.Audio.Media.TITLE, c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
            values.put(MediaStore.Audio.Media.ALBUM_ID, c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));
            values.put(MediaStore.Audio.Media.DATA, c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
            dh.insertValue(values);
            values.clear();
        }
        c.close();
        Toast.makeText(getApplicationContext(), R.string.menu_playlist_refresh, Toast.LENGTH_SHORT).show();
        return true;
    }

    private boolean ReloadList(boolean shouldFinish){
        boolean ret=ReloadList();
        if(shouldFinish)finish();
        return ret;
    }

    private void DisplayList(){
        String[]from={MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media._ID};
        int[]to={android.R.id.text1,android.R.id.text2,0};
        listView.setAdapter(new SimpleCursorAdapter(this,android.R.layout.simple_list_item_2,dh.queryList(),from,to,1));
    }

    private boolean OnRefresh(){
        ReloadList();
        DisplayList();
        return true;
    }

    private boolean OnClearList(){
        dh.DelAll();
        DisplayList();
        return true;
    }

    private boolean OnViewSavedPlaylist(){
        startActivityForResult(new Intent(this,SavedPlaylistActivity.class),R.id.action_view_saved_playlist&0xFFFF);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode!=RESULT_OK)return;
        switch (requestCode){
            case R.id.action_playlist_add&0xFFFF:AddFile(data);break;
            case R.id.action_playlist_add_dir&0xFFFF:AddDir(data);break;
            case R.id.action_view_saved_playlist&0xFFFF:OnFinishViewSavedPlaylist(data);break;
        }
    }

    private void OnFinishViewSavedPlaylist(Intent data){
        if(data.getBooleanExtra(SavedPlaylistActivity.EXTRA_NEED_REFRESH_LIST,false)){
            //刷新列表
            DisplayList();
        }
    }

    private void AddFile(Intent data){
        Uri uri=data.getData();
        String path=FileUtils.getAudioPath(this,uri);
        if(path==null)
            path=FileUtils.getVideoPath(this,uri);
        if(path==null){
            Toast.makeText(this,R.string.message_cannot_catchFilepath,Toast.LENGTH_SHORT).show();
            return;
        }
        if(dh.ifExists(path)){
            Toast.makeText(this,R.string.message_file_exists,Toast.LENGTH_SHORT).show();
            return;
        }
        ContentValues values=new ContentValues();
        String[]parts=path.split("/");
        values.put(MediaStore.Audio.Media.TITLE,parts[parts.length-1]);
        values.put(MediaStore.Audio.Media.DATA,path);
        dh.insertValue(values);
        DisplayList();
    }

    private boolean IsAcceptableExtension(String path){
        String[]extensions={"mp3","wav","aiff","m4a","mpg","avi","flac","wma","ape","ogg","mp4"};
        String[]parts=path.split("/");
        if(parts.length<2)
            return false;
        String[]fileParts=parts[parts.length-1].split("\\.");
        if(fileParts.length<2)
            return false;
        String ext=fileParts[fileParts.length-1];
        for(String e:extensions){
            if(ext.toLowerCase(Locale.ROOT).equals(e))
                return true;
        }
        return false;
    }

    private void AddDir(Intent data){
        Uri uri=data.getData();
        String path=FileUtils.getAudioPath(this,uri);
        if(path==null)
            path=FileUtils.getVideoPath(this,uri);
        if(path==null){
            Toast.makeText(this,R.string.message_cannot_catchFilepath,Toast.LENGTH_SHORT).show();
            return;
        }
        String dir=path.substring(0,path.lastIndexOf('/'));
        ContentValues values=new ContentValues();
        File file=new File(dir);
        File[]fs=file.listFiles();
        ArrayList<String>list=new ArrayList<>();
        if(fs!=null&&fs.length>0) {
            for (File f : fs) {
                if (!f.isDirectory()) {
                    path = f.getPath();
                    if (IsAcceptableExtension(path)&&!dh.ifExists(path)) {
                        list.add(path);
                    }
                }
            }
        }
        Collections.sort(list, String::compareTo);
        for(String str:list){
            String[]parts=str.split("/");
            values.put(MediaStore.Audio.Media.TITLE,parts[parts.length-1]);
            values.put(MediaStore.Audio.Media.DATA,str);
            dh.insertValue(values);
        }
        DisplayList();
        Toast.makeText(this,getString(R.string.message_add_count,list.size()),Toast.LENGTH_SHORT).show();
    }

    private int GetIdOfChosenItem(int position){
        Cursor c=(Cursor)listView.getItemAtPosition(position);
        return c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
    }
}

class FileUtils {
    static String getPath(Context context, Uri uri,String keyProjection,Uri qUri) {

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { keyProjection };
            Cursor cursor;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if(cursor!=null) {
                    int column_index = cursor.getColumnIndexOrThrow(projection[0]);
                    if (cursor.moveToFirst()) {
                        String path=cursor.getString(column_index);
                        if(path==null){
                            //Android 4.4 or later.
                            //https://stackoverflow.com/a/41520090
                            String args[]=new String[]{DocumentsContract.getDocumentId(uri).split(":")[1]};
                            uri=qUri;
                            cursor=context.getContentResolver().query(uri,projection,"_id=?",args,null);
                            column_index=cursor.getColumnIndexOrThrow(projection[0]);
                            if(cursor.moveToFirst())
                                path=cursor.getString(column_index);
                        }
                        return path;
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }

        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    static String getAudioPath(Context context,Uri uri){
        return getPath(context,uri,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
    }

    static String getVideoPath(Context context,Uri uri){
        return getPath(context,uri,MediaStore.Video.Media.DATA,MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    }
}