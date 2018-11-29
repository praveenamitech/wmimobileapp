package com.example.bhanu.hdevices;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KeyActivity extends AppCompatActivity {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = KeyActivity.class.getSimpleName();

    OkHttpClient client = new OkHttpClient();

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);

        handler = new Handler();
        SharedPreferences prefs = getSharedPreferences("com.amitech.MeterScanner", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Meter Scanner");
        toolbar.setNavigationIcon(R.mipmap.logo);
        setSupportActionBar(toolbar);

        EditText licenseKeyEditText = findViewById(R.id.licenseKeyEditText);
        Button okBtn = findViewById(R.id.okBtn);

        okBtn.setOnClickListener(view -> {
            String licenseKey = licenseKeyEditText.getText().toString();

            if (licenseKey.length() < 10) {
                Toast.makeText(getApplicationContext(), "Not a valid key!", Toast.LENGTH_LONG).show();
                return;
            }

            String deviceId = Settings.Secure.getString(getBaseContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("key", licenseKey);
            jsonObject.addProperty("deviceId", deviceId);

            new Thread(() -> {
                String resp = null;
                try {
                    resp = post(Constants.LICENSE_SERVER_URL + "/register", jsonObject.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (resp == null) {
                    handler.post(() -> {
                        Toast.makeText(getApplicationContext(), "Cannot validate license due to unknown server error!", Toast.LENGTH_LONG).show();
                    });

                    return;
                }

                Log.d(TAG, "resp = " + resp);

                JsonParser parser = new JsonParser();
                JsonObject respJson = parser.parse(resp).getAsJsonObject();

                boolean success = respJson.get("success").getAsBoolean();


                handler.post(() -> {
                    if (success) {
                        Toast.makeText(getApplicationContext(), "Registered successfully", Toast.LENGTH_LONG).show();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isRegistered", true);
                        editor.putString("key", licenseKey);
                        editor.apply();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String error = null;
                        try {
                            error = respJson.get("error").getAsString();
                        } catch (NullPointerException npe) {
                            Log.e(TAG, "in response \'error\' value is null");
                        }

                        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
                    }
                });

            }).start();
        });
    }

    String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
