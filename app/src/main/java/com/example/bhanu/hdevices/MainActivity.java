package com.example.bhanu.hdevices;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.bhanu.hdevices.dialogs.AddNewMeterDialog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.bhanu.hdevices.KeyActivity.JSON;

public class MainActivity extends AppCompatActivity {

    private class FragmentName {
        private static final String UNDEFINED_DEVICES_LIST_FRAGMENT = "UNDEFINED_DEVICES_LIST_FRAGMENT";
        private static final String SAVED_DEVICES_LIST_FRAGMENT = "SAVED_DEVICES_LIST_FRAGMENT";
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    private Handler handler;
    HashMap<String, Fragment> fragments;
    DataHolder dataHolder;
    SocketHandler socketHandler;
    FileManager fileManager;
    TabLayout tabLayout;
    ViewPager viewPager;
    TabPagerAdapter tabPagerAdapter;
    DatabaseHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        handler = new Handler();

        Singletons singletons = Singletons.getInstance(getApplicationContext());

        dataHolder = singletons.getDataHolder();
        socketHandler = singletons.getSocketHandler();
        fileManager = FileManager.getInstance();
        db = singletons.getDatabaseHandler();

        fragments = new HashMap<>();
        fragments.put(FragmentName.SAVED_DEVICES_LIST_FRAGMENT, new SavedDevicesListFragment());
        fragments.put(FragmentName.UNDEFINED_DEVICES_LIST_FRAGMENT, new UndefinedDevicesListFragment());

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Meter Scanner");
        toolbar.setNavigationIcon(R.mipmap.logo);
        setSupportActionBar(toolbar);

        tabPagerAdapter = new TabPagerAdapter(getSupportFragmentManager());
        tabPagerAdapter.addFragment(fragments.get(FragmentName.SAVED_DEVICES_LIST_FRAGMENT), getResources().getString(R.string.saved_meters_tab_name));
        tabPagerAdapter.addFragment(fragments.get(FragmentName.UNDEFINED_DEVICES_LIST_FRAGMENT), getResources().getString(R.string.undefined_meters_tab_name));
        viewPager.setAdapter(tabPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void loadCsvDataIfAny() {
        String line = null;
        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canRead()) {
                String csvFileName = "energy_meters.txt";

                File csvFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), csvFileName);

                if (!csvFile.exists()) {
                    Toast.makeText(getApplicationContext(), "No data found to import", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.truncate("meters");

                FileInputStream fin = new FileInputStream(csvFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(fin));
                br.readLine(); // ignore first line
                while ((line = br.readLine()) != null) {
                    String[] lineArray = line.split(",");
                    if (lineArray.length == 1) {
                        continue;
                    }
                    String make = lineArray[0];
                    String mtrNo = lineArray[1];

                    if (!db.meterExists(mtrNo)) {
                        db.addMeter(mtrNo, Integer.parseInt(make));
                    }
                }

                Toast.makeText(getApplicationContext(), "Data imported successfully, Please restart the app", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error occurred while importing data!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error occurred while importing data.");
            Log.e(TAG, "line = " + line);
            e.printStackTrace();
        }
    }

    private void exportFromDBToCsv() {
        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                String csvFileName = "energy_meters.txt";

                File csvFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), csvFileName);

                if (csvFile.delete()) {
                    Log.d(TAG, "Deleted existing txt file: " + csvFile.getName());
                }

                FileOutputStream fout = new FileOutputStream(csvFile);
                ArrayList<EnergyMeter> meters = db.getMeters();

                String headingStr = "make, mtrNo, makeString, readStatus, uploadStatus, timestamp\n\r";
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(headingStr);
                String tempString;

                for (EnergyMeter m : meters) {
                    tempString = m.getMake() + "," + m.getMtrNo() + "," + m.getMakeString() + "," + m.getReadStatus() + "," + m.getUploadStatus() + "," + m.getTimestamp() + "\n\r";
                    stringBuilder.append(tempString);
                }

                fout.write(stringBuilder.toString().getBytes());

                Log.d(TAG, "Data exported to csv file \'" + csvFile.getCanonicalPath() + "\' successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while exporting data to csv file.");
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.ftp_settings) {
            Log.d(TAG, "ftp settings clicked");
            Intent intent = new Intent(getApplicationContext(), FTPConfigActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.add_new_meter) {
            FragmentManager manager = getSupportFragmentManager();
            AddNewMeterDialog addNewMeterDialog = new AddNewMeterDialog();
            addNewMeterDialog.show(manager, "addNewMeterDialog");
            return true;
        }

        if (id == R.id.upload_all_saved) {
            Log.d(TAG, "savedDevicesList size: " + dataHolder.getSavedDevicesList().size());
            uploadAllMetersFiles(dataHolder.getSavedDevicesList());
            return true;
        }

        if (id == R.id.upload_all_undefined) {
            uploadAllMetersFiles(dataHolder.getUndefinedDevicesList());
            return true;
        }

        if (id == R.id.import_meters_data) {
            Log.d(TAG, "Import meters data clicked");
            loadCsvDataIfAny();
            return true;
        }

        if (id == R.id.export_meters_data) {
            Log.d(TAG, "Import meters data clicked");
            exportFromDBToCsv();
            Toast.makeText(getApplicationContext(), "Data exported successfully", Toast.LENGTH_LONG).show();
            return true;
        }

        if (id == R.id.unregister_license) {
            Log.d(TAG, "Unregister license clicked");

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            SharedPreferences prefs = getSharedPreferences("com.amitech.MeterScanner", MODE_PRIVATE);
                            String licenseKey = prefs.getString("key", null);

                            String deviceId = Settings.Secure.getString(getBaseContext().getContentResolver(),
                                    Settings.Secure.ANDROID_ID);

                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("key", licenseKey);
                            jsonObject.addProperty("deviceId", deviceId);

                            new Thread(() -> {
                                String resp = null;
                                try {
                                    resp = unregisterLicensePostReq(Constants.LICENSE_SERVER_URL + "/unregister", jsonObject.toString());
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
                                        Toast.makeText(getApplicationContext(), "Unregistered successfully", Toast.LENGTH_LONG).show();
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putBoolean("isRegistered", false);
                                        editor.putString("key", null);
                                        editor.apply();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
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
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            // do nothing
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Are you sure that you want to unregister license?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    String unregisterLicensePostReq(String url, String json) throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    void uploadAllMetersFiles(ArrayList<EnergyMeter> energyMeters) {
        final int[] fileCount = {0};
        final int[] totalFiles = { 0 };


        for (EnergyMeter device : energyMeters) {
            totalFiles[0] += device.getRelatedFiles().size();
        }

        Log.d(TAG, "totalFiles: " + totalFiles[0]);


        if (totalFiles[0] == 0) {
            Toast.makeText(getApplicationContext(), "No Files To Upload", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(this); // this = YourActivity
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Uploading...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        new Thread(() -> {
            try {
                FtpUploader ftpUploader = Singletons.getInstance(getApplicationContext()).getFtpUploader();
                ftpUploader.connect();
                ftpUploader.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    Toast.makeText(getApplicationContext(), "FTP Error!", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            for (EnergyMeter device : energyMeters) {
                Log.d(TAG, "upload all pressed");

                if (device.getRelatedFiles().size() > 0) {
                    device.setUploadStatus(Constants.PROGRESS);
                    SavedDevicesListAdapter.getInstance().updateList();
                    UndefinedDevicesListAdapter.getInstance().updateList();
                }

                MetersHelper.uploadAndDeleteFiles(device, isUploaded -> {
                    if (!isUploaded) {
                        device.setUploadStatus(Constants.FAILED);
                        SavedDevicesListAdapter.getInstance().updateList();
                        UndefinedDevicesListAdapter.getInstance().updateList();
                        handler.post(dialog::dismiss);
                    } else {
                        FileManager fileManager = FileManager.getInstance();
                        String fileNamePrefix = device.getMakeString() + "_" + device.getMtrNo();
                        device.setRelatedFiles(fileManager.getFilesStartingWithName(fileNamePrefix));

                        if (device.isSavedMeter()) {
                            SavedDevicesListAdapter.getInstance().updateListItem(device);
                        } else {
                            UndefinedDevicesListAdapter.getInstance().updateListItem(device);
                        }

                        ++fileCount[0];

                        Log.d(TAG, "uploaded: " + fileCount[0] + " / " + totalFiles[0]);

                        handler.post(() -> {
                            dialog.setMessage("uploaded: " + fileCount[0] + " / " + totalFiles[0]);

                            if (totalFiles[0] - fileCount[0] <=1) {
                                dialog.dismiss();
                                SavedDevicesListAdapter.getInstance().updateList();
                                UndefinedDevicesListAdapter.getInstance().updateList();
                            }
                        });
                    }
                });
            }

            SavedDevicesListAdapter.getInstance().updateList();
            UndefinedDevicesListAdapter.getInstance().updateList();
        }).start();
    }
}
