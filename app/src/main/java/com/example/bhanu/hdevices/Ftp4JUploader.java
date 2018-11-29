package com.example.bhanu.hdevices;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;

public class Ftp4JUploader implements FtpUploader {
    private static final String TAG = Ftp4JUploader.class.getSimpleName();
    private static Ftp4JUploader instance = null;

    private AppState appState;
    private final FTPClient client;


    private Ftp4JUploader() {
        appState = AppState.getInstance();
        client = new FTPClient();
    }

    @Override
    public boolean fileExists(String fileName) {
        synchronized (client) {
            try {
                FTPFile[] allFiles = client.list();

                if (allFiles.length == 0) {
                    Log.e(TAG, "No file found with name " + fileName);
                    Log.e(TAG, "in directory " + AppState.getInstance().getFtpServerDir());
                    return false;
                }

                for (FTPFile file: allFiles) {
                    if (file.getName().equals(fileName)) {
                        Log.d(TAG, "File found on server: " + fileName);
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (FTPAbortedException e) {
                e.printStackTrace();
            } catch (FTPListParseException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            } catch (FTPDataTransferException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            }

            return false;
        }
    }

    @Override
    public void connect() throws Exception {
        synchronized (client) {
            String host = appState.getFtpHost(),
                    user = appState.getFtpUsername(),
                    pwd = appState.getFtpPassword(),
                    serverDir = appState.getFtpServerDir();

            client.connect(host);
            client.login(user, pwd);
            client.changeDirectory(serverDir);
        }
    }

    @Override
    public void disconnect() {
        synchronized (client) {
            try {
                client.disconnect(true);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FTPIllegalReplyException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void uploadFile(String localFileFullName, String fileName, String hostDir) throws Exception {
        synchronized (client) {
            try (InputStream input = new FileInputStream(new File(localFileFullName))) {
                client.upload(new java.io.File(localFileFullName));
            }
        }
    }

    public static Ftp4JUploader getInstance() {
        if (instance == null) {
            instance = new Ftp4JUploader();
        }

        return instance;
    }
}
