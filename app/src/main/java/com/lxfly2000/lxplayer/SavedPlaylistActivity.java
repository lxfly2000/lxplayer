package com.lxfly2000.lxplayer;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.lxfly2000.utilities.FileUtility.ReadFile;
import static com.lxfly2000.utilities.FileUtility.WriteFile;

public class SavedPlaylistActivity extends AppCompatActivity {
    public static final String EXTRA_NEED_REFRESH_LIST="EXTRA_NEED_REFRESH_LIST";
    RecyclerView recyclerView;
    FloatingActionButton fabDelete;
    String savingName="";
    MyItemRecyclerViewAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_playlist);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        recyclerView=findViewById(R.id.recyclerView);
        fabDelete=findViewById(R.id.fabDelete);

        fabDelete.setOnClickListener(view -> {
            playlistAdapter.DeleteCheckedItems((pos,name,value) -> {
                File f=new File(value);
                if(f.exists()&&f.isFile()){
                    f.delete();
                }
            });
            OnDeleteList(true);
        });

        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        playlistAdapter=new MyItemRecyclerViewAdapter();
        playlistAdapter.SetOnPlaylistClickListener((pos,name,value) -> ChooseList(name,value));
        recyclerView.setAdapter(playlistAdapter);
        OnDeleteList(true);
        //填充初始数据
        File plDir=new File(GetPlaylistPath());
        if(plDir.exists()&&plDir.isDirectory()){
            for(File f:plDir.listFiles()){
                if(f.isFile()){
                    String fn=f.getName();
                    if(fn.endsWith(".m3u")){
                        playlistAdapter.AddList(fn.substring(0,fn.lastIndexOf('.')),f.getAbsolutePath());
                    }
                }
            }
        }
        //文件名列表实际上不需要拖动排序，而是要按名称或修改时间等排序
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saved_playlist,menu);
        for(int i=0;i<menu.size();i++){
            Drawable drawable=menu.getItem(i).getIcon();
            if(drawable!=null)
                drawable.setTint(0xFFFFFFFF);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:finish();return true;
            case R.id.action_saved_playlist_save:OnSaveList();break;
            case R.id.action_saved_playlist_delete:OnDeleteList(false);break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void OnSaveList(){
        OnDeleteList(true);
        EditText editText=new EditText(this);
        AlertDialog dlg=new AlertDialog.Builder(this)
                .setTitle(R.string.title_enter_name)
                .setPositiveButton(android.R.string.ok,(dialogInterface, i) -> SaveList(editText.getText().toString()))
                .setNegativeButton(android.R.string.cancel,(dialogInterface, i) -> savingName="")
                .setCancelable(false)
                .setView(editText)
                .create();
        if(!savingName.isEmpty()){
            editText.setText(savingName);
        }else{
            //https://www.runoob.com/java/java-date-time.html
            editText.setText(getString(R.string.file_playlist_default_name,new SimpleDateFormat("yyyy-MM-dd hh_mm_ss").format(new Date())));
        }
        editText.setSingleLine();
        editText.setFocusableInTouchMode(true);
        editText.setSelectAllOnFocus(true);
        dlg.setOnShowListener(dialog -> {
            editText.requestFocus();
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.R){
                editText.getWindowInsetsController().show(WindowInsets.Type.ime());
            }
        });
        dlg.show();
        editText.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_UP){
                dlg.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
    }

    static final String invalidChar="\\/:*?\"<>|\r\n\t";

    private void SaveList(String name){
        savingName=name;
        //检查空
        if(savingName.isEmpty()){
            Toast.makeText(this,R.string.msg_cannot_be_empty,Toast.LENGTH_SHORT).show();
            OnSaveList();
            return;
        }
        //检查重复
        File plDir=new File(GetPlaylistPath());
        if(plDir.exists()&&plDir.isDirectory()) {
            for (File f : plDir.listFiles()) {
                String fn = f.getName();
                if (savingName.compareTo(fn.substring(0, fn.lastIndexOf('.'))) == 0) {
                    Toast.makeText(this, R.string.msg_save_playlist_fail_exists, Toast.LENGTH_SHORT).show();
                    OnSaveList();
                    return;
                }
            }
        }
        //检查非法字符
        for(int i=0;i<invalidChar.length();i++) {
            if(savingName.indexOf(invalidChar.charAt(i))!=-1){
                Toast.makeText(this,R.string.msg_save_playlist_fail_invalid_char,Toast.LENGTH_SHORT).show();
                OnSaveList();
                return;
            }
        }
        String path=GetPlaylistPath()+"/"+savingName+".m3u";
        playlistAdapter.AddList(savingName,path);
        //保存到列表
        StringBuilder sb=new StringBuilder();
        ListDataHelper dh=ListDataHelper.getInstance();
        dh.UpdateDataCounts();
        for(int i=0;i<dh.GetDataCount();i++){
            sb.append(dh.GetPathByIndex(i)).append('\n');
        }
        if(!WriteFile(path,sb.toString()))
            Toast.makeText(this,R.string.msg_save_playlist_fail,Toast.LENGTH_SHORT).show();
        savingName="";
    }

    private void OnDeleteList(boolean resetToFalse){
        playlistAdapter.SetIsChoosing(resetToFalse?false:!playlistAdapter.GetIsChoosing());
        if(playlistAdapter.GetIsChoosing()){
            fabDelete.show();
        }else {
            fabDelete.hide();
        }
    }

    private void ChooseList(String name,String value){
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.baseline_warning_24)
                .setTitle(R.string.msg_warning_replace_playlist)
                .setMessage(name+"\n"+value)
                .setPositiveButton(android.R.string.ok,(dialogInterface, i) -> {
                    //替换
                    File f=new File(value);
                    if(f.exists()&&f.isFile()) {
                        ListDataHelper dh=ListDataHelper.getInstance();
                        dh.DelAll();
                        String data=ReadFile(f);
                        for (String path:data.split("\n")) {
                            ContentValues values = new ContentValues();
                            String[] parts = path.split("/");
                            values.put(MediaStore.Audio.Media.TITLE, parts[parts.length - 1]);
                            values.put(MediaStore.Audio.Media.DATA, path);
                            dh.insertValue(values);
                        }
                    }
                    Intent intent=new Intent();
                    intent.putExtra(EXTRA_NEED_REFRESH_LIST,true);
                    setResult(RESULT_OK,intent);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel,null)
                .show();
    }

    private String GetPlaylistPath(){
        return getExternalFilesDir(null).getPath()+"/playlists";
    }
}
