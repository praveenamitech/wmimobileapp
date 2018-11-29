package com.example.bhanu.hdevices;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by bhanu on 14/2/18.
 *
 *
 *
 */

public class FileManager {
    private static final String TAG = FileManager.class.getSimpleName();
    private static FileManager instance = null;

    private File dir;

    private FileManager() {
        Log.d(TAG, "isExternalStorageReadable: " + isExternalStorageReadable());
        Log.d(TAG, "isExternalStorageWritable: " + isExternalStorageWritable());
        getAlbumStorageDir("MetersData");
    }

    public static FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }

        return instance;
    }

    public void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();


        }

        catch (FileNotFoundException fnfe1) {
            Log.e(TAG, fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public void write(String fileName, String fileContents) {
        File file = new File(dir, fileName);

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println(fileContents);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getAbsoluteFileName(String fileName) {
        File file = new File(dir, fileName);
        return file.getAbsolutePath();
    }

    public ArrayList<File> getFilesStartingWithName(String fileName) {
        File[] files = dir.listFiles();

        ArrayList<File> filesList = new ArrayList<>();

        for (File f : files) {
            if (f.getName().startsWith(fileName)) {
                filesList.add(f);
            }
        }

        return filesList;
    }

    public File getFile(String fileName) {
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().equals(fileName)) {
                return f;
            }
        }

        return null;
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private File getAlbumStorageDir(String dirName) {
        // Get the directory for the user's public pictures directory.
        dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), dirName);
        if (!dir.mkdir()) {
            Log.e(TAG, "Directory not created");
        }
        return dir;
    }

    //    public File getAlbumStorageDir(Context context, String albumName) {
//        // Get the directory for the app's private pictures directory.
//        File file = new File(context.getExternalFilesDir(
//                Environment.DIRECTORY_DOWNLOADS), albumName);
//        if (!file.mkdirs()) {
//            Log.e(TAG, "Directory not created");
//        }
//        return file;
//    }
}
