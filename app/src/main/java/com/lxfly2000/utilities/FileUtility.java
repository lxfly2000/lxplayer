package com.lxfly2000.utilities;

import java.io.*;

public class FileUtility {
    public static String ReadFile(File file){
        StringWriter writer=new StringWriter();
        if(file.canRead()){
            try {
                FileReader reader=new FileReader(file);
                int readSingleByte;
                while (true){
                    readSingleByte=reader.read();
                    if(readSingleByte==-1)
                        break;
                    writer.write(readSingleByte);
                }
                reader.close();
            }catch (IOException e){
                return null;
            }
        }
        return writer.toString();
    }

    //读取文件，成功后返回文件文本，出任何错误则返回null
    public static String ReadFile(String path){
        File file=new File(path);
        return ReadFile(file);
    }

    //保存文本到文件，成功返回true, 失败返回false
    public static boolean WriteFile(String path,String data){
        try {
            FileWriter writer=new FileWriter(CreateFile(path));
            writer.write(data);
            writer.close();
        }catch (IOException e){
            return false;
        }
        return true;
    }

    public static File CreateFile(String path)throws IOException{
        File file=new File(path);
        if(!file.exists()){
            if(!path.contains("/")){
                path=(new File("")).getCanonicalPath()+"/"+path;
            }
            File dir=new File(path.substring(0,path.lastIndexOf('/')));
            if(!dir.exists()&&!dir.mkdirs())
                return null;
            if(!file.createNewFile())
                return null;
        }
        return file;
    }

    public static boolean DeleteFile(String path){
        return new File(path).delete();
    }

    public static boolean WriteStreamToFile(String path,InputStream stream){
        return WriteStreamToFile(path,stream,true);
    }

    public static boolean WriteStreamToFile(String path,InputStream stream,boolean reset){
        try {
            FileOutputStream outputStream = new FileOutputStream(CreateFile(path));
            if(reset)
                stream.reset();
            byte[]buffer=new byte[1024];
            while(stream.read(buffer)!=-1){
                outputStream.write(buffer);
            }
            outputStream.flush();
            outputStream.close();
        }catch (IOException e){
            return false;
        }
        return true;
    }

    public static boolean IsFileExists(String path){
        File file=new File(path);
        return file.exists();
    }

    public static long GetFileSize(String path){
        return new File(path).length();
    }

    public static String ReplaceIllegalPathChar(String path){
        return ReplaceIllegalPathChar(path,"_");
    }

    public static String ReplaceIllegalPathChar(String path,String replacingChar){
        return path.replaceAll("[/\":|<>?*]",replacingChar);
    }
}
