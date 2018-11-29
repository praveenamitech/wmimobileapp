package com.example.bhanu.hdevices;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by bhanu on 14/2/18.
 *
 */

public class DataHolder {
    private static final String TAG = DataHolder.class.getSimpleName();
    private static DataHolder instance = null;

    private ArrayList<EnergyMeter> undefinedDevicesList, savedDevicesList;

    private DataHolder() {
        Log.d(TAG, "Initiated data holder");
        undefinedDevicesList = new ArrayList<>();
        savedDevicesList = new ArrayList<>();
    }

    public static DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }

        return instance;
    }

    public ArrayList<EnergyMeter> getUndefinedDevicesList() {
        return undefinedDevicesList;
    }

    public void setUndefinedDevicesList(ArrayList<EnergyMeter> connectedDevicesList) {
        this.undefinedDevicesList = connectedDevicesList;
    }

    public ArrayList<EnergyMeter> clearUndefinedDevicesList() {
        undefinedDevicesList = new ArrayList<>();
        return undefinedDevicesList;
    }

    public ArrayList<EnergyMeter> getSavedDevicesList() {
        return savedDevicesList;
    }

    public void setSavedDevicesList(ArrayList<EnergyMeter> savedDevicesList) {
        this.savedDevicesList = savedDevicesList;
    }
}
