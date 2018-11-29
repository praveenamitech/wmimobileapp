package com.example.bhanu.hdevices;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class MeterDetailsActivity extends AppCompatActivity {

    private static final String TAG = MeterDetailsActivity.class.getSimpleName();

    private EnergyMeter energyMeter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_details);

        int meterIndex = getIntent().getIntExtra("meterIndex", -1);
        String meterType = getIntent().getStringExtra("meterType");

        if (meterType.equals("saved")) {
            energyMeter = DataHolder.getInstance().getSavedDevicesList().get(meterIndex);
        } else if (meterType.equals("undefined")) {
            energyMeter = DataHolder.getInstance().getUndefinedDevicesList().get(meterIndex);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Meter - " + energyMeter.getMtrNo());
        toolbar.setNavigationIcon(R.mipmap.logo);
        setSupportActionBar(toolbar);

        FileManager fileManager = FileManager.getInstance();
        String fileNamePrefix = energyMeter.getMakeString() + "_" + energyMeter.getMtrNo();
        energyMeter.setRelatedFiles(fileManager.getFilesStartingWithName(fileNamePrefix));

        ArrayList<File> files = energyMeter.getRelatedFiles();

        for (File file : files) {
            Log.d(TAG, "file name: " + file.getName());
        }

        ListView listView = findViewById(R.id.filesListView);
        MeterFilesListAdapter meterFilesListAdapter = new MeterFilesListAdapter(this, files);
        listView.setAdapter(meterFilesListAdapter);
    }
}
