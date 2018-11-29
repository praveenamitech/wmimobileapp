package com.example.bhanu.hdevices.protocols;

import android.util.Log;

import com.example.bhanu.hdevices.Constants;
import com.example.bhanu.hdevices.CustomUtils;
import com.example.bhanu.hdevices.EnergyMeter;
import com.example.bhanu.hdevices.FileManager;
import com.example.bhanu.hdevices.MetersHelper;
import com.example.bhanu.hdevices.SavedDevicesListAdapter;
import com.example.bhanu.hdevices.UndefinedDevicesListAdapter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bhanu on 16/2/18.
 *
 * Thread which communicates with L&T meter.
 */

public class LNT2ServiceThread extends Thread {
    private static final String TAG = LNT2ServiceThread.class.getSimpleName();

    private Socket myClientSocket;
    private EnergyMeter device;

    // to send hex
    private DataOutputStream dout = null;

    private InputStream inputStream = null;
    private byte[] responseBytes;

    private ArrayList<byte[]> commandResponses = new ArrayList<>();

    private FileManager fileManager = FileManager.getInstance();

    public LNT2ServiceThread(Socket s, EnergyMeter device) {
        this.device = device;
        myClientSocket = s;
        device.setMakeString(Constants.LNT2_STR);

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
        Log.d(TAG, "Created new therad for client: " + myClientSocket.getInetAddress().getHostAddress());
        AtomicBoolean gotInitData = new AtomicBoolean(false);
        final AtomicBoolean error = new AtomicBoolean(false);

        //writing thread, write first command to socket in separate thread continuously
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (error.get()) {
                    break;
                }
                try {
                    write(Commands.lnt2_cmd_first);
                    Thread.sleep(1000);
                    if (gotInitData.get()) break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        //Reading thread, wait for first command response in separate thread
        new Thread(() -> {
            byte akgBuffer[] = new byte[50];
            int readBytes;
            try {
                readBytes = inputStream.read(akgBuffer);
                responseBytes = Arrays.copyOf(akgBuffer, readBytes);
                Log.d(TAG, "received init resp: " + CustomUtils.bytesToHexString(responseBytes));

                if (responseBytes[0] == Commands.lnt2_resp_akg1[0]
                        || responseBytes[0] == Commands.lnt2_resp_akg2[0]) {
                    gotInitData.set(true);
                    Log.d(TAG, "got initial response: " + CustomUtils.bytesToHexString(responseBytes));
                    commandResponses.add(responseBytes);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException occurred while waiting for the 1st command response");
                e.printStackTrace();
                gotInitData.set(false);
            } catch (NegativeArraySizeException e) {
                Log.e(TAG, "Socket connection might have been disconnected");
                e.printStackTrace();
                gotInitData.set(false);
            } catch (Exception e) {
                Log.e(TAG, "Unknown occurred while waiting for the 1st command response");
                e.printStackTrace();
                gotInitData.set(false);
            }
        }).start();

        try {
            for (int i = 0; i < 10; i++) {
                Log.d(TAG, "Waiting for 1st command response");
                Thread.sleep(Constants.MIN_DELAY);
                if (gotInitData.get()) {
                    break;
                }

                if (i == 9 && !gotInitData.get()) {
                    Log.e("TAG", "Did not get any data after 1st command");
                    error.set(true);
                    break;
                }
            }

            if (error.get()) {
                Log.e(TAG, "Error occurred after first command");
                dout.close();
                inputStream.close();
                myClientSocket.close();
                device.setReadStatus(Constants.FAILED);
                updateListItem(device);
                return;
            }

            // send second command
            Thread.sleep(Constants.MIN_DELAY);
            write(Commands.lnt2_cmd_second);

            // receive response from second command
            Thread.sleep(Constants.MIN_DELAY);
            if (inputStream.available() > 0) {
                bytes = inputStream.read(buffer);
                responseBytes = Arrays.copyOf(buffer, bytes);
                Log.d(TAG, "received meter s_no: " + new String(responseBytes));
                commandResponses.add(responseBytes);
            } else {
                sleep(Constants.NORM_DELAY);
                if(inputStream.available() == 0) {
                    // stop reading
                    inputStream.close();
                    dout.close();
                    myClientSocket.close();
                    error.set(true);
                }
            }

            if (error.get()) {
                Log.e(TAG, "Error occurred after first command");
                dout.close();
                inputStream.close();
                myClientSocket.close();
                device.setReadStatus(Constants.FAILED);
                updateListItem(device);
                return;
            }

            // send third command
            sleep(Constants.MIN_DELAY);
            write(Commands.lnt2_cmd_third);
            sleep(Constants.MIN_DELAY);

            StringBuilder largeDataBuilder = new StringBuilder();
            buffer = new byte[1024];

            while (true) {
                try {
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        responseBytes = Arrays.copyOf(buffer, bytes);
                        String responseString = new String(responseBytes);
                        Log.d(TAG, "received data: " + responseString);
                        largeDataBuilder.append(responseString);
                        if (responseString.contains(("!"))) {
                            break;
                        }
                    } else {
                        sleep(Constants.NORM_DELAY);
                        if(inputStream.available() == 0) {
                            // stop reading
                            inputStream.close();
                            dout.close();
                            myClientSocket.close();
                            error.set(true);
                            break;
                        }
                    }
                } catch (IOException e) {
                    error.set(true);
                    break;
                } catch (Exception e) {
                    error.set(true);
                    break;
                }
            }

            if (error.get()) {
                Log.e(TAG, "Error while reading LNT2 data");
                dout.close();
                inputStream.close();
                myClientSocket.close();
                device.setReadStatus(Constants.FAILED);
                updateListItem(device);
                return;
            }

            String fileName = CustomUtils.getFileNameOfDevice(device);

            updateListItem(device);
            commandResponses.add(largeDataBuilder.toString().getBytes());
            CustomUtils.writeAsciiToFile(commandResponses, fileName);
            device.setTimestamp(CustomUtils.getDateTime(new Date()));
            device.getRelatedFiles().add(fileManager.getFile(fileName));
            commandResponses = new ArrayList<>();

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
            Log.e(TAG, "IOExcepiton while running LNT2 thread.");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                dout.close();
                inputStream.close();
                myClientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void  write(byte[] data) {
        try {
            dout.write(data);
            dout.flush();
            Log.d(TAG, "Sent Data: " + CustomUtils.bytesToHexString(data));
        } catch (SocketException e) {
            Log.e(TAG, "socket exception occurred");
            e.printStackTrace();
            device.setReadStatus(Constants.FAILED);
            updateListItem(device);
        } catch (IOException e) {
            e.printStackTrace();
            device.setReadStatus(Constants.FAILED);
            updateListItem(device);
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
