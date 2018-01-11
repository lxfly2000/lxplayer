package com.lxfly2000.lxplayer;

public class UpdateChecker {
    private String fileURL;
    private boolean errorOccured=true;
    public UpdateChecker(String url){
        SetCheckURL(url);
    }

    public void SetCheckURL(String url){
        fileURL=url;
    }

    public boolean CheckForUpdate(){
        if(GetUpdateVersionCodeFromStream()>BuildConfig.VERSION_CODE)
            return true;
        return false;
    }

    public boolean IsError(){
        return errorOccured;
    }

    public int GetUpdateVersionCodeFromStream(){
        //todo
        return 1;
    }

    public String GetUpdateVersionNameFromStream(){
        //todo
        return "1.0";
    }

    private void DownloadFileToStream(){
        //todo
    }
}
