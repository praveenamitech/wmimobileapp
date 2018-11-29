package com.example.bhanu.hdevices;

import android.util.Log;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Created by bhanu on 19/2/18.
 *
 */

public class ApacheFtpUploader implements FtpUploader {

    private static ApacheFtpUploader instance = null;
    private static final String TAG = ApacheFtpUploader.class.getSimpleName();
    private FTPClient ftp = null;
    private AppState appState;

    private ApacheFtpUploader() {
        ftp = new FTPClient();
        appState = AppState.getInstance();
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
    }

    public static ApacheFtpUploader getInstance() {
        if (instance == null) {
            instance = new ApacheFtpUploader();
        }

        return instance;
    }

    @Override
    public void connect() throws Exception {
        int reply;
        String host = appState.getFtpHost(),
                user = appState.getFtpUsername(),
                pwd = appState.getFtpPassword();

        ftp.connect(host);
        reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }

        if (ftp.login(user, pwd)) {
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            System.out.println("Logged into FTP server successfully");
        } else {
            ftp.logout();
            ftp.disconnect();
            throw new Exception("Failed log into FTP server");
        }
    }

    @Override
    public boolean fileExists(String fileName) {
        try {
            FTPFile[] allFiles = ftp.listFiles(AppState.getInstance().getFtpServerDir());

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
        }

        return false;
    }

    @Override
    public void uploadFile(String localFileFullName, String fileName, String hostDir) throws Exception {
        try (InputStream input = new FileInputStream(new File(localFileFullName))) {
            ftp.storeFile(hostDir + fileName, input);
        }
    }

    @Override
    public void disconnect(){
        if (this.ftp.isConnected()) {
            try {
                this.ftp.logout();
                this.ftp.disconnect();
            } catch (IOException f) {
                // do nothing as file is already saved to server
            }
        }
    }
}
