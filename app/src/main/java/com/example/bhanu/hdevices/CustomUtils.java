package com.example.bhanu.hdevices;

import android.util.Log;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by bhanu on 15/2/18.
 *
 */

public class CustomUtils {
    private static final String TAG = CustomUtils.class.getSimpleName();
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHexString(byte[] bytes, String prefix, String separator) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        String hexString = new String(hexChars);

        StringBuilder stringBuilder = new StringBuilder();
        char[] chars = hexString.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (i % 2 == 0) {
                stringBuilder.append(prefix);
            }

            stringBuilder.append(chars[i]);

            if (i % 2 != 0) {
                stringBuilder.append(separator);
            }
        }

        hexString = stringBuilder.toString().trim();

        return hexString;
    }

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String dataStr) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] tempStrArr = dataStr.split(" ");
        for(String tempStr: tempStrArr) {
            stringBuilder.append(tempStr);
        }

        String s = stringBuilder.toString();

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String getDateTime(Date timestamp) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(Constants.DATETIME_FORMAT);
        String strDate = sdfDate.format(timestamp);
        return strDate;
    }

    public static String getTime(Date timestamp) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(Constants.TIME_FORMAT);
        String strDate = sdfDate.format(timestamp);
        return strDate;
    }

    public static String getCurrentTimeStamp(Date timestamp) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddMMyyHHmmss");//dd/MM/yyyy
        String strDate = sdfDate.format(timestamp);
        return strDate;
    }

    public static void writeToFile(ArrayList<byte[]> commandResponses, String fileName) {
        Log.d(TAG, "creating file");
        // write to file
        FileManager fileManager = FileManager.getInstance();
        StringBuffer stringBuffer = new StringBuffer();
        for(byte[] bytes2 : commandResponses) {
            stringBuffer.append(CustomUtils.bytesToHexString(bytes2, "", "") + "\n");
        }

        fileManager.write(fileName, stringBuffer.toString());

        Log.d(TAG, "file written successfully: " + fileName);
    }

    public static void writeAsciiToFile(ArrayList<byte[]> commandResponses, String fileName) {
        Log.d(TAG, "creating file");
        // write to file
        FileManager fileManager = FileManager.getInstance();
        StringBuilder stringBuilder = new StringBuilder();
        for(byte[] bytes2 : commandResponses) {
            stringBuilder.append(new String(bytes2));
        }

        fileManager.write(fileName, stringBuilder.toString());
        Log.d(TAG, "file written successfully: " + fileName);
    }

    public static String getFileNameOfDevice(EnergyMeter device) {
        String fileName = device.getMakeString() + "_"
                + device.getMtrNo().trim() + "_"
                + CustomUtils.getCurrentTimeStamp(new Date()) + ".txt";
        return fileName;
    }

    private static AppState appState = AppState.getInstance();
    private static final FtpUploader FTP_UPLOADER = Singletons.getInstance().getFtpUploader();

    public static void uploadFile(String fileName, MyConsumer<Boolean> consumer) {
        new Thread(() -> {
            try {
                synchronized (FTP_UPLOADER) {
                    FTP_UPLOADER.connect();
                    FileManager fileManager = FileManager.getInstance();
                    Log.d(TAG, "Uploading file: " + fileName);
                    FTP_UPLOADER.uploadFile(fileManager.getAbsoluteFileName(fileName), fileName, appState.getFtpServerDir());

                    if (FTP_UPLOADER.fileExists(fileName)) {
                        Log.d(TAG, "Upload success: " + fileName);
                        FTP_UPLOADER.disconnect();
                        consumer.accept(true);
                    } else {
                        FTP_UPLOADER.disconnect();
                        consumer.accept(false);
                        uploadFile(fileName, consumer);
                    }
                }
            } catch (Exception e) {

                Log.e(TAG, "Error occurred while uploading file \'" + fileName + "\' to server");
                e.printStackTrace();
                consumer.accept(false);
            }
        }).start();
    }

    public static ArrayList<EnergyMeter> reorderIndexNo(ArrayList<EnergyMeter> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setIndex(i+1);
        }

        return list;
    }

    // size is given in bytes
    public static String friendlyFileSize(long size){
        String hrSize = "";

        double k = size/1024;
        double m = k/1024;
        double g = m/1024;
        double t = g/1024;

        DecimalFormat dec = new DecimalFormat("0.00");

        if (size < 1024) {
            hrSize = size + "B";
        }

        else if (k > 0) {
            hrSize = dec.format(k).concat("KB");
        }

        else if (m > 0) {
            hrSize = dec.format(m).concat("MB");
        }

        else if (g > 0) {
            hrSize = dec.format(g).concat("GB");
        }

        else if (t > 0) {
            hrSize = dec.format(t).concat("TB");
        }

        return hrSize;
    }

    public static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    public static byte[] int2byte(int[]src) {
        int srcLength = src.length;
        byte[]dst = new byte[ src.length];

        for (int i=0; i<srcLength; i++) {
            dst[i] = (byte) src[i];
        }
        return dst;
    }
}
