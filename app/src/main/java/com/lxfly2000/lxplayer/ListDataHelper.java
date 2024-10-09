package com.lxfly2000.lxplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;

//用于存储播放列表
public class ListDataHelper extends SQLiteOpenHelper{
    private static final String szFileDB="list.db";
    private static final String szTableName="List";
    private SQLiteDatabase listDatabase;
    private int dataCounts=0;
    private static ListDataHelper stHelper;

    private ListDataHelper(Context context){
        super(context,szFileDB,null,1);
        UpdateDataCounts();
    }

    //单例模式：https://zhidao.baidu.com/question/402613189.html
    public static ListDataHelper getInstance(Context context){
        if(stHelper==null)
            stHelper=new ListDataHelper(context);
        return stHelper;
    }

    public static ListDataHelper getInstance(){
        return stHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        listDatabase=db;
        listDatabase.execSQL("Create Table "+szTableName+
                "("+ MediaStore.Audio.Media._ID+" Integer Default '1' Not null Primary key Autoincrement,"+
                MediaStore.Audio.Media.TITLE+" Text,"+
                MediaStore.Audio.Media.ALBUM_ID+" Integer Default '0',"+
                MediaStore.Audio.Media.DATA+" Text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }

    public void insertValue(ContentValues values){
        SQLiteDatabase db=getWritableDatabase();
        db.insert(szTableName,null,values);
    }

    public Cursor queryList(){
        SQLiteDatabase db=getWritableDatabase();
        Cursor c=db.query(szTableName,null,null,null,null,null,MediaStore.Audio.Media._ID);
        return c;
    }

    public void delId(int id){
        SQLiteDatabase db=getWritableDatabase();
        String[]sa={String.valueOf(id)};
        db.delete(szTableName,MediaStore.Audio.Media._ID+"=?",sa);
    }

    public void DelAll(){
        SQLiteDatabase db=getWritableDatabase();
        db.delete(szTableName,null,null);
    }

    @Override
    public void close(){
        super.close();
        listDatabase.close();
    }

    public boolean ifExists(String path){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media.DATA};
        String[]args={path};
        Cursor c=db.query(szTableName,cols,MediaStore.Audio.Media.DATA+"=?",args,null,null,MediaStore.Audio.Media._ID);
        boolean r=c.getCount()>0;
        c.close();
        return r;
    }

    public String GetPathById(int id){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA};
        String[]args={String.valueOf(id)};
        Cursor c=db.query(szTableName,cols,MediaStore.Audio.Media._ID+"=?",args,null,null,MediaStore.Audio.Media._ID);
        String path=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        c.close();
        return path;
    }

    public String GetPathByIndex(int index){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media.DATA};
        Cursor c=db.query(szTableName,cols,null,null,null,null,MediaStore.Audio.Media._ID);
        c.moveToPosition(index);
        String path=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        c.close();
        return path;
    }

    public void UpdateDataCounts(){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media.DATA};
        dataCounts=db.query(szTableName,cols,null,null,null,null,MediaStore.Audio.Media._ID).getCount();
    }

    public int GetDataCount(){
        return dataCounts;
    }

    public String GetTitleByIndex(int index){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media.TITLE};
        Cursor c=db.query(szTableName,cols,null,null,null,null,MediaStore.Audio.Media._ID);
        c.moveToPosition(index);
        String title=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        c.close();
        return title;
    }

    public int GetIdByIndex(int index){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media._ID};
        Cursor c=db.query(szTableName,cols,null,null,null,null,MediaStore.Audio.Media._ID);
        c.moveToPosition(index);
        int id=c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        c.close();
        return id;
    }

    public long GetAlbumIdByIndex(int index){
        SQLiteDatabase db=getWritableDatabase();
        String[]cols={MediaStore.Audio.Media.ALBUM_ID};
        Cursor c=db.query(szTableName,cols,null,null,null,null,MediaStore.Audio.Media._ID);
        c.moveToPosition(index);
        long id=c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        c.close();
        return id;
    }

    public void SwapIndex(int a,int b){
        SQLiteDatabase db=getWritableDatabase();
        Cursor c=db.query(szTableName,null,null,null,null,null,MediaStore.Audio.Media._ID);
        c.moveToPosition(a);
        long idA=c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        String titleA=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        long albumIdA=c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        String dataA=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        c.moveToPosition(b);
        long idB=c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        String titleB=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        long albumIdB=c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        String dataB=c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        c.close();
        ContentValues contentValues=new ContentValues();
        contentValues.put(MediaStore.Audio.Media.TITLE,titleB);
        contentValues.put(MediaStore.Audio.Media.ALBUM_ID,albumIdB);
        contentValues.put(MediaStore.Audio.Media.DATA,dataB);
        db.update(szTableName,contentValues,MediaStore.Audio.Media._ID+"=?",new String[]{String.valueOf(idA)});
        contentValues.clear();
        contentValues.put(MediaStore.Audio.Media.TITLE,titleA);
        contentValues.put(MediaStore.Audio.Media.ALBUM_ID,albumIdA);
        contentValues.put(MediaStore.Audio.Media.DATA,dataA);
        db.update(szTableName,contentValues,MediaStore.Audio.Media._ID+"=?",new String[]{String.valueOf(idB)});
    }
}
