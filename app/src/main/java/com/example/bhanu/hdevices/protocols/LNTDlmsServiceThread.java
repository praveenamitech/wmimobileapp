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
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bhanu on 16/2/18.
 *
 */

public class LNTDlmsServiceThread extends Thread {
    private static final String TAG = LNTDlmsServiceThread.class.getSimpleName();

    private Socket myClientSocket;
    private EnergyMeter device;

    // to send hex
    private DataOutputStream dout = null;

    private InputStream inputStream = null;
    private byte[] responseBytes;

    private ArrayList<byte[]> commandResponses = new ArrayList<>();

    private FileManager fileManager = FileManager.getInstance();

    public LNTDlmsServiceThread(Socket s, EnergyMeter device) {
        this.device = device;
        myClientSocket = s;
        device.setMakeString(Constants.LNT_DLMS_STR);

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

        CommandsGenerator commandsGenerator = new CommandsGenerator();



        AtomicBoolean error = new AtomicBoolean(false);


        try {
            byte[] cmd;

            // 1st
            cmd = commandsGenerator.send_7DLMS(0x53);
            write(cmd);
            Log.d(TAG, "sent " + "1st command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 2nd
            cmd = commandsGenerator.send_7DLMS(0x93);
            write(cmd);
            Log.d(TAG, "sent " + "2st command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 3rd
            cmd = commandsGenerator.send_AthenticationLT();
            write(cmd);
            Log.d(TAG, "sent " + "3rd command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 4th
            commandsGenerator.DLMS_COM = 0x10;
            commandsGenerator.DLMS_COML = 0x0F & commandsGenerator.DLMS_COM;
            commandsGenerator.DLMS_COMH = 0xF0 & commandsGenerator.DLMS_COM;
            cmd = commandsGenerator.getDLMS_MtrDataCmd(1);
            write(cmd);
            Log.d(TAG, "sent " + "4th command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 5th
            cmd = commandsGenerator.getDLMS_MtrDataCmd(2);
            write(cmd);
            Log.d(TAG, "sent " + "5th command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 6th
            cmd = commandsGenerator.send_7DLMS(0x53);
            write(cmd);
            Log.d(TAG, "sent " + "6th command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 7th
            cmd = commandsGenerator.Init_LNT();
            write(cmd);
            Log.d(TAG, "sent " + "7th command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);

            // 8th
            cmd = commandsGenerator.send_AthenticationLT();
            write(cmd);
            Log.d(TAG, "sent " + "8th command");
            Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
            responseBytes = read();

            if (responseBytes == null) {
                handleError();
            }
            commandResponses.add(responseBytes);


            // 9th
            commandResponses.add("ENERGYSTRT".getBytes());
            for (int i = 0;;) {
                if (commandsGenerator.bilack == 0 && commandsGenerator.block_ack == 0) {
                    i++;
                }

                if (i > 4) {
                    break;
                }

                cmd = commandsGenerator.SendCmdforBilling(i, commandsGenerator.bilack, commandsGenerator.block_ack);

                write(cmd);
                Log.d(TAG, "sent " + "9th command");
                Log.d(TAG, "sent " + CustomUtils.bytesToHexString(cmd, "", " "));
                Log.d(TAG, "bill values billack = " + commandsGenerator.bilack);
                Log.d(TAG, "bill values block_ack = " + commandsGenerator.block_ack);
                Log.d(TAG, "bill values cmd = " + i);
                responseBytes = read();

                if (responseBytes == null) {
                    handleError();
                }

                commandsGenerator.checkResponse(responseBytes);

                commandResponses.add(responseBytes);
            }
            commandResponses.add("ENERGYEND".getBytes());


        } catch (SocketException e) {
            Log.d(TAG, "socket exception occurred");
            error.set(true);
            e.printStackTrace();
        } catch (IOException e) {
            error.set(true);
            e.printStackTrace();
        }

        if (error.get()) {
            handleError();
            return;
        }

//        try {
//            send9thCmd();
//        } catch (SocketException e) {
//            Log.d(TAG, "socket exception occurred");
//            error.set(true);
//            e.printStackTrace();
//        } catch (IOException e) {
//            Log.d(TAG, "IOException occurred while sending 9th command");
//            e.printStackTrace();
//        }

        String fileName = CustomUtils.getFileNameOfDevice(device);
        CustomUtils.writeToFile(commandResponses, fileName);
        device.setTimestamp(CustomUtils.getDateTime(new Date()));
        device.getRelatedFiles().add(fileManager.getFile(fileName));

        device.setReadStatus(Constants.DONE);
        device.setUploadStatus(Constants.PROGRESS);
        updateListItem(device);

        new Thread(() -> MetersHelper.uploadAndDeleteFiles(device, isUploaded -> {
            if (!isUploaded) {
                device.setUploadStatus(Constants.FAILED);
                updateListItem(device);
            } else {
                FileManager fileManager = FileManager.getInstance();
                device.setRelatedFiles(fileManager.getFilesStartingWithName(device.getFileNamePrefix()));
                updateListItem(device);
            }
        })).start();

        try {
            inputStream.close();
            dout.close();
            myClientSocket.close();
            Log.d(TAG,"Stopped thread");
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

//    private void send9thCmd() throws IOException {
//        // send 9th command
//        write(commands.get("dlms_ninth"));
//        Log.d(TAG, "sent 9th command");
//        responseBytes = read();
//
//        if (responseBytes == null) {
//            handleError();
//            return;
//        }
//
//        if (responseBytes[1] == (byte) 0xA8) {
//            commandResponses.add(responseBytes);
//            send9thCmd();
//            return;
//        }
//
//        write(commands.get("dlms_ninth_ack"));
//        Log.d(TAG, "sent 9th ack command");
//
//        responseBytes = read();
//
//        if (responseBytes == null) {
//            handleError();
//            return;
//        }
//
//        commandResponses.add(responseBytes);
//    }

    private void handleError() {
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
            Log.e(TAG, "Not a valid response (doesn't start and end with 0X7E)");
            Log.e(TAG, CustomUtils.bytesToHexString(responseBytes));
            return null;
        }

        return responseBytes;
    }

    private void  write(byte[] data) throws IOException {
        dout.write(data);
        dout.flush();
//        Log.d(TAG, "Sent Data: " + CustomUtils.bytesToHexString(data));
        commandResponses.add(data);
    }

    private boolean isValidResponse(byte[] cmdBytes) {
        return cmdBytes[0] == 0x7E && cmdBytes[cmdBytes.length - 1] == 0x7E;
    }

    private void updateListItem(EnergyMeter device) {
        if (device.isSavedMeter()) {
            SavedDevicesListAdapter.getInstance().updateListItem(device);
        } else {
            UndefinedDevicesListAdapter.getInstance().updateListItem(device);
        }
    }
}
