package com.example.bhanu.hdevices.protocols;

import android.util.Log;

import com.example.bhanu.hdevices.Constants;
import com.example.bhanu.hdevices.CustomUtils;
import com.example.bhanu.hdevices.EnergyMeter;
import com.example.bhanu.hdevices.FileManager;
import com.example.bhanu.hdevices.MetersHelper;
import com.example.bhanu.hdevices.SavedDevicesListAdapter;
import com.example.bhanu.hdevices.UndefinedDevicesListAdapter;

import org.apache.commons.lang3.ArrayUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.GenericArrayType;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bhanu on 16/2/18.
 *
 */

public class GenusServiceThread extends Thread {
    private static final String TAG = GenusServiceThread.class.getSimpleName();

    private Socket myClientSocket;
    private EnergyMeter device;

    // to send hex
    private DataOutputStream dout = null;

    private InputStream inputStream = null;
    private byte[] responseBytes;

    private ArrayList<byte[]> commandResponses = new ArrayList<>();

    private FileManager fileManager = FileManager.getInstance();

    public GenusServiceThread(Socket s, EnergyMeter device) {
        this.device = device;
        myClientSocket = s;
        device.setMakeString(Constants.GENUS_STR);

        try {
            dout = new DataOutputStream(myClientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, s.toString());

        try {
            inputStream = myClientSocket.getInputStream();
            dout = new DataOutputStream(myClientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        Log.d(TAG, "Created new thread for client: " + myClientSocket.getInetAddress().getHostAddress());

        LinkedHashMap<String, byte[]> commands = new LinkedHashMap<>();

        commands.put("first_cmd", Commands.gen_first);
        commands.put("gen_second", Commands.gen_second);
        commands.put("gen_third", Commands.gen_third);
        commands.put("gen_fourth", Commands.gen_fourth);

        Iterator iterator = commands.entrySet().iterator();
        AtomicBoolean error = new AtomicBoolean(false);


        while (iterator.hasNext()) {
            try {
                Map.Entry pair = (Map.Entry)iterator.next();
                write((byte[])pair.getValue());
                Log.d(TAG, "sent " + pair.getKey());
                responseBytes = read();

                if (responseBytes == null) {
                    error.set(true);
                    break;
                }

                commandResponses.add(responseBytes);
            } catch (SocketException e) {
                Log.d(TAG, "socket exception occurred");
                error.set(true);
                e.printStackTrace();
                break;
            } catch (IOException e) {
                error.set(true);
                e.printStackTrace();
                break;
            }
        }

        if (error.get()) {
            try {
                inputStream.close();
                dout.close();
                myClientSocket.close();
                Log.d(TAG,"Stopped thread");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }

            device.setReadStatus(Constants.FAILED);
            updateListItem(device);
            return;
        }

        String fileName = CustomUtils.getFileNameOfDevice(device);
        CustomUtils.writeToFile(commandResponses, fileName);
        device.setTimestamp(CustomUtils.getDateTime(new Date()));
        device.getRelatedFiles().add(fileManager.getFile(fileName));

        device.setReadStatus(Constants.DONE);
        device.setUploadStatus(Constants.PROGRESS);
        updateListItem(device);

        new Thread(() -> {
            MetersHelper.uploadAndDeleteFiles(device, isUploaded -> {
                if (!isUploaded) {
                    device.setUploadStatus(Constants.FAILED);
                    updateListItem(device);
                } else {
                    FileManager fileManager = FileManager.getInstance();
                    device.setRelatedFiles(fileManager.getFilesStartingWithName(device.getFileNamePrefix()));
                    updateListItem(device);
                }
            });
        }).start();

        try {
            inputStream.close();
            dout.close();
            myClientSocket.close();
            Log.d(TAG,"Stopped thread");
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private byte[] read() throws IOException {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        byte[] chunk;
        int count = 0;

        while (true) {
            while (inputStream.available() > 0) {
                bytes = inputStream.read(buffer);
                chunk = Arrays.copyOf(buffer, bytes);
                count++;

                Log.d(TAG, "Received data: " + CustomUtils.bytesToHexString(chunk));

                if (count > 1) {
                    responseBytes = ArrayUtils.addAll(responseBytes, chunk);
                } else {
                    responseBytes = chunk;
                }
            }

            try {
                Thread.sleep(Constants.MIN_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

            if (inputStream.available() == 0) {
                break;
            }
        }

        if (!isValidResponse(responseBytes)) {
            Log.e(TAG, "Not a valid response (doesn't start with 4706)");
            Log.e(TAG, CustomUtils.bytesToHexString(responseBytes));
            return null;
        }

        return responseBytes;
    }

    private void  write(byte[] data) throws IOException {
        dout.write(data);
        dout.flush();
        Log.d(TAG, "Sent Data: " + CustomUtils.bytesToHexString(data));
    }

    private boolean isValidResponse(byte[] cmdBytes) {
        return cmdBytes[0] == 0x47 && cmdBytes[1] == 0x06;
    }

    private void updateListItem(EnergyMeter device) {
        if (device.isSavedMeter()) {
            SavedDevicesListAdapter.getInstance().updateListItem(device);
        } else {
            UndefinedDevicesListAdapter.getInstance().updateListItem(device);
        }
    }
}
