package com.example.bhanu.hdevices;

import android.util.Log;

import java.io.File;

/**
 * Created by bhanu on 22/2/18.
 *
 */

public class MetersHelper {
    private static final String TAG = MetersHelper.class.getSimpleName();

    public static void uploadAndDeleteFiles(EnergyMeter device, MyConsumer<Boolean> c) {
        if (device.getRelatedFiles().size() == 0) {
            Log.d(TAG, "No files to upload");
            return;
        }

        for (int i = 0; i < device.getRelatedFiles().size(); i++) {
            File file = device.getRelatedFiles().get(i);
            Log.d(TAG, "uploading file: " + file.getName() + " ...");
            CustomUtils.uploadFile(file.getName(), (isUploaded) -> {
                if (isUploaded) {
                    Log.d(TAG, "uploaded file: " + file.getName());
                    device.setUploadStatus(Constants.DONE);
                    FileManager.getInstance().moveFile(file.getParent() + "/", file.getName(), file.getParent() + "/uploaded/");
                    Log.d(TAG, "moved file to \'uploaded\' folder: " + file.getName());
                    c.accept(true);
                } else {
                    Log.e(TAG, "upload failed: " + file.getName());
                    device.setUploadStatus(Constants.FAILED);
                    c.accept(false);
                }
            });
        }

    }
}
