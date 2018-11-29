package com.example.bhanu.hdevices;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by bhanu on 20/2/18.
 *
 * This class maintains the dynamic state of the app
 */

public class AppState {
    private static AppState instance = null;

    private String ftpHost, ftpUsername, ftpPassword, ftpServerDir;

    private AppState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("com.amitech.MeterScanner", MODE_PRIVATE);
        setFtpHost(prefs.getString("ftp_host", ""));
        setFtpUsername(prefs.getString("ftp_username", ""));
        setFtpPassword(prefs.getString("ftp_password", ""));
        setFtpServerDir(prefs.getString("ftp_server_dir", ""));
    }

    public static AppState getInstance(Context context) {
        if (instance == null) {
            instance = new AppState(context);
        }

        return instance;
    }

    public static AppState getInstance() {
        return instance;
    }

    public String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
    }

    public String getFtpUsername() {
        return ftpUsername;
    }

    public void setFtpUsername(String ftpUsername) {
        this.ftpUsername = ftpUsername;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public String getFtpServerDir() {
        return ftpServerDir;
    }

    public void setFtpServerDir(String ftpServerDir) {
        this.ftpServerDir = ftpServerDir;
    }
}
