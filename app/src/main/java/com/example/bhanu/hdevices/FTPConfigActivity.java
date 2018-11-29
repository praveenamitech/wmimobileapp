package com.example.bhanu.hdevices;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FTPConfigActivity extends AppCompatActivity {

    String ftpHost, ftpUsername, ftpPassword, ftpServerDir;
    EditText usernameEditText, hostEditText, passwordEditText, ftpServerDirEditText;
    Intent intent;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftpconfig);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Meter Scanner - Server Config");
        toolbar.setNavigationIcon(R.mipmap.logo);
        setSupportActionBar(toolbar);

        Button okBtn = findViewById(R.id.okBtn);

        hostEditText = findViewById(R.id.input_ftp_host);
        usernameEditText = findViewById(R.id.input_ftp_username);
        passwordEditText = findViewById(R.id.input_ftp_password);
        ftpServerDirEditText = findViewById(R.id.input_ftp_server_dir);

        prefs = getSharedPreferences("com.amitech.MeterScanner", MODE_PRIVATE);
        hostEditText.setText(prefs.getString("ftp_host", ""));
        usernameEditText.setText(prefs.getString("ftp_username", ""));
        passwordEditText.setText(prefs.getString("ftp_password", ""));
        ftpServerDirEditText.setText(prefs.getString("ftp_server_dir", ""));

        okBtn.setOnClickListener(view -> {
            updateFtpDetails();
            Toast.makeText(getApplicationContext(), "FTP Settings Changed", Toast.LENGTH_LONG).show();
            intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateFtpDetails();
    }

    private void updateFtpDetails() {
        ftpHost = hostEditText.getText().toString();
        ftpUsername = usernameEditText.getText().toString();
        ftpPassword = passwordEditText.getText().toString();
        ftpServerDir = ftpServerDirEditText.getText().toString();

        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("ftp_host", ftpHost);
        edit.putString("ftp_username", ftpUsername);
        edit.putString("ftp_password", ftpPassword);
        edit.putString("ftp_server_dir", ftpServerDir);
        edit.apply();

        AppState appState = AppState.getInstance();
        appState.setFtpHost(ftpHost);
        appState.setFtpUsername(ftpUsername);
        appState.setFtpPassword(ftpPassword);
        appState.setFtpServerDir(ftpServerDir);
    }
}
