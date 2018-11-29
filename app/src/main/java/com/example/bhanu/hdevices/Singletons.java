package com.example.bhanu.hdevices;

import android.content.Context;
import android.util.Log;

public class Singletons {
    private static final String TAG = Singletons.class.getSimpleName();
    private static Singletons instance = null;

    private DataHolder dataHolder;
    private SocketHandler socketHandler;
    private FileManager fileManager;
    private FtpUploader ftpUploader;
    private DatabaseHandler databaseHandler;

    private Singletons(Context context) {
        dataHolder = DataHolder.getInstance();
        socketHandler = SocketHandler.getInstance(context);
        fileManager = FileManager.getInstance();
        ftpUploader = ApacheFtpUploader.getInstance();
        databaseHandler = DatabaseHandler.getInstance(context);
    }

    public static Singletons getInstance(Context context) {
        if (instance == null) {
            instance = new Singletons(context);
        }

        return instance;
    }

    public static Singletons getInstance() {
        if (instance == null) {
            Log.e(TAG, "Invoked Singleton.getInstnace() for the first time, (context needed)");
        }

        return instance;
    }

    public DataHolder getDataHolder() {
        return dataHolder;
    }

    public SocketHandler getSocketHandler() {
        return socketHandler;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public FtpUploader getFtpUploader() {
        return ftpUploader;
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }
}
