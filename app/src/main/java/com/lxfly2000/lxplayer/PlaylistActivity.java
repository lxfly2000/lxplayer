package com.lxfly2000.lxplayer;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                PlaySelectedItem(position);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                selectedListItemIndex=position;
                return false;
            }
        });
        dh=ListDataHelper.getInstance(this);
        if(getIntent().getBooleanExtra("ShouldFinish",false))ReloadList(true);
        DisplayList();

        //处理Intent
        HandleIntent(getIntent());
        //初始化搜索建议相关变量
        final String[]suggestionKeys=new String[]{"title"};
        final int[]suggestionsIds=new int[]{android.R.id.text1};
        suggestionsAdapter=new SimpleCursorAdapter(this,android.R.layout.simple_list_item_1,null,suggestionKeys,
                suggestionsIds, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
    }

    SimpleCursorAdapter suggestionsAdapter;

    @Override
    protected void onNewIntent(Intent intent){
        HandleIntent(intent);
    }

    private void HandleIntent(Intent intent){
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String queryWord=intent.getStringExtra(SearchManager.QUERY);
            //跳转至搜索的番剧名称处
            for(int i=0;i<dh.GetDataCount();i++){
                if(dh.GetTitleByIndex(i).equals(queryWord)){
                    Toast.makeText(this,String.format(getString(R.string.message_anime_jumping),queryWord),Toast.LENGTH_SHORT).show();
                    listView.setSelection(i);
                    return;
                }
            }
            Toast.makeText(this,String.format(getString(R.string.message_anime_not_found),queryWord),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        getMenuInflater().inflate(R.menu.menu_playlist_popup,menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_playlist, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        //https://developer.android.google.cn/training/search/setup
        //设置搜索属性
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int i) {
                return onSuggestionClick(i);
            }

            @Override
            public boolean onSuggestionClick(int i) {
                //查询选择的是什么建议
                //https://stackoverflow.com/a/50385750（答案有误）
                MatrixCursor c=(MatrixCursor)searchView.getSuggestionsAdapter().getCursor();
                c.moveToPosition(i);
                searchView.setQuery(c.getString(1),true);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                UpdateSuggestionAdapter(s);
                return false;
            }
        });
        return true;
    }

    private void  UpdateSuggestionAdapter(String queryStr){
        final MatrixCursor c=new MatrixCursor(new String[]{BaseColumns._ID,"title"});
        for(int i=0;i<dh.GetDataCount();i++){
            String title=dh.GetTitleByIndex(i);
            if(title.toLowerCase().contains(queryStr.toLowerCase()))
                c.addRow(new Object[]{i,title});
        }
        suggestionsAdapter.changeCursor(c);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_playlist_add:return OnAddFile();
            case R.id.action_playlist_refresh:return OnRefresh();
            case R.id.action_playlist_clear:return OnClearList();
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
    private boolean OnAddFile(){
        //参考：http://www.bkjia.com/Androidjc/1075240.html
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.title_chooseFile)), R.id.action_playlist_add & 0xFFFF);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode!=RESULT_OK)return;
        switch (requestCode){
            case R.id.action_playlist_add&0xFFFF:AddFile(data);break;
        }
    }

    private void AddFile(Intent data){
        Uri uri=data.getData();
        String path=FileUtils.getPath(this,uri);
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

    private int GetIdOfChosenItem(int position){
        Cursor c=(Cursor)listView.getItemAtPosition(position);
        return c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
    }
}

class FileUtils {
    static String getPath(Context context, Uri uri) {

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Audio.Media.DATA };
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
                            uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
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
                // Eat it
                Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }

        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}