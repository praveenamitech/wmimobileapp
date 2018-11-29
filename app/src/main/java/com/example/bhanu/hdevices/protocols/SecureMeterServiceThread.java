package com.example.bhanu.hdevices.protocols;

import android.util.Log;

import com.example.bhanu.hdevices.DatabaseHandler;
import com.example.bhanu.hdevices.EnergyMeter;
import com.example.bhanu.hdevices.Constants;
import com.example.bhanu.hdevices.CustomUtils;
import com.example.bhanu.hdevices.Singletons;
import com.example.bhanu.hdevices.UndefinedDevicesListAdapter;
import com.example.bhanu.hdevices.FileManager;
import com.example.bhanu.hdevices.MetersHelper;
import com.example.bhanu.hdevices.SavedDevicesListAdapter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by bhanu on 16/2/18.
 *
 */

public class SecureMeterServiceThread extends Thread {
    private static final String TAG = SecureMeterServiceThread.class.getSimpleName();

    private Socket myClientSocket;
    private boolean m_bRunThread = true;
    private EnergyMeter device;

    // to send hex
    private DataOutputStream dout = null;

    private InputStream inputStream = null;
    private byte[] responseBytes;

    private ArrayList<byte[]> commandResponses = new ArrayList<>();

    private FileManager fileManager = FileManager.getInstance();
    private DatabaseHandler db = null;

    public SecureMeterServiceThread(Socket s, EnergyMeter device) {
        this.device = device;
        myClientSocket = s;

        device.setMakeString(Constants.SECURE_STR);

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
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
//        Log.d(TAG, "Accepted Client Address - " + myClientSocket.getInetAddress().getHostName());
        Log.d(TAG, "Created new therad for client: " + myClientSocket.getInetAddress().getHostAddress());

        try {
            while (m_bRunThread) {
                try {
                    write(Constants.HEX, Commands.sec_cmd_first);
                    Log.d(TAG, "sent 1st command");

                    // wait for 11 bytes response
                    Thread.sleep(500);
                    bytes = inputStream.read(buffer);
                    responseBytes = Arrays.copyOf(buffer, bytes);
                    Log.d(TAG, "received 1st cmd resp: " + CustomUtils.bytesToHexString(responseBytes));
                    commandResponses.add(responseBytes);

                    // send 2nd command (0x80)
                    write(Constants.HEX, Commands.sec_cmd_second);

                    // 2nd command response
                    Thread.sleep(500);
                    bytes = inputStream.read(buffer);
                    responseBytes = Arrays.copyOf(buffer, bytes);
                    Log.d(TAG, "received 2nd cmd resp: " + CustomUtils.bytesToHexString(responseBytes));
                    if (!Arrays.equals(responseBytes, Commands.sec_resp_second)) {
                        m_bRunThread = false;
                    }
                    commandResponses.add(responseBytes);

                    /// 3rd command loop
                    while (true) {
                        write(Constants.HEX, Commands.sec_cmd_third);
                        Thread.sleep(500);
                        bytes = inputStream.read(buffer);
                        if (bytes == -1) {
                            responseBytes = new byte[1024];
                            break;
                        }
                        responseBytes = Arrays.copyOf(buffer, bytes);
                        Log.d(TAG, "received 3rd cmd resp: " + CustomUtils.bytesToHexString(responseBytes));

                        byte[] first2Bytes = Arrays.copyOf(responseBytes, 2);
                        if (Arrays.equals(first2Bytes, new byte[] { (byte)0x8a, (byte)0x82 })) {
                            commandResponses.add(responseBytes);
                            break;
                        } else {
                            commandResponses.add(responseBytes);
                        }
                    }

                    // send 4th command (0xfc)
                    write(Constants.HEX, Commands.sec_cmd_fourth);

                    // 4th command response
                    bytes = inputStream.read(buffer);
                    Log.d(TAG, "received 4th cmd resp: " + CustomUtils.bytesToHexString(responseBytes));
                    responseBytes = Arrays.copyOf(buffer, bytes);
                    if (!Arrays.equals(responseBytes, Commands.sec_resp_fourth)) {
                        m_bRunThread = false;
                    }
                    commandResponses.add(responseBytes);

                    String fileName = CustomUtils.getFileNameOfDevice(device);

                    CustomUtils.writeToFile(commandResponses, fileName);
                    device.setTimestamp(CustomUtils.getDateTime(new Date()));
                    device.getRelatedFiles().add(fileManager.getFile(fileName));

                    commandResponses = new ArrayList<>();
                    m_bRunThread = false;

                    inputStream.close();
                    dout.close();
                    myClientSocket.close();
                    Log.d(TAG,"Stopped thread");

                    device.setReadStatus(Constants.DONE);
                    device.setUploadStatus(Constants.PROGRESS);

                    device.setRelatedFiles(fileManager.getFilesStartingWithName(device.getFileNamePrefix()));

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

                } catch (IOException e) {
                    break;
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
            device.setReadStatus(Constants.FAILED);
            updateListItem(device);
        }
        finally {
            try {
                inputStream.close();
                dout.close();
                myClientSocket.close();
                Log.d(TAG,"Stopped thread");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void  write(int mode, byte[] data) {
        try {
            if (mode == Constants.ASCII) {
                Log.d(TAG, "Sent Data: " + data);
            }

            else if (mode == Constants.HEX) {
                dout.write(data);
                dout.flush();
                Log.d(TAG, "Sent Data: " + CustomUtils.bytesToHexString(data));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateListItem(EnergyMeter device) {
        if (device.isSavedMeter()) {
            SavedDevicesListAdapter.getInstance().updateListItem(device);
        } else {
             UndefinedDevicesListAdapter.getInstance().updateListItem(device);
        }
    }
}
