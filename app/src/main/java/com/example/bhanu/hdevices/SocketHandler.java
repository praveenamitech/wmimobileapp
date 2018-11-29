package com.example.bhanu.hdevices;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import com.example.bhanu.hdevices.protocols.GenusServiceThread;
import com.example.bhanu.hdevices.protocols.LNT2ServiceThread;
import com.example.bhanu.hdevices.protocols.LNTDlmsServiceThread;
import com.example.bhanu.hdevices.protocols.LNTLegacyServiceThread;
import com.example.bhanu.hdevices.protocols.LNTServiceThread;
import com.example.bhanu.hdevices.protocols.LPlusGServiceThread;
import com.example.bhanu.hdevices.protocols.SecureDlmsServiceThread;
import com.example.bhanu.hdevices.protocols.SecureMeterServiceThread;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by bhanu on 14/2/18.
 *
 *
 */

public class SocketHandler {
    private static final String TAG = SocketHandler.class.getSimpleName();
    private static SocketHandler instance = null;
    private int SERVER_PORT = 8888;

    private MyConsumer<Boolean> onDevicesFoundHandler = null;
    private DatabaseHandler db;

    ServerSocket myServerSocket;
    boolean ServerOn = true;

    private SocketHandler(Context context) {
        db = DatabaseHandler.getInstance();
        new Thread(() -> {
            try {
                myServerSocket = new ServerSocket(SERVER_PORT);
            } catch(IOException ioe) {
                Log.d(TAG,"Could not create server socket on port 8888. Quitting.");
                System.exit(-1);
            }

            Calendar now = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
            Log.d(TAG,"It is now : " + formatter.format(now.getTime()));

            while(ServerOn) {
                try {
                    Socket clientSocket = myServerSocket.accept();
                    ClientServiceThread cliThread = new ClientServiceThread(clientSocket);
                    cliThread.start();
                } catch(IOException ioe) {
                    Log.d(TAG,"Exception found on accept. Ignoring. Stack Trace :");
                    ioe.printStackTrace();
                }
            }
            try {
                myServerSocket.close();
                Log.d(TAG,"Server Stopped");
            } catch(Exception ioe) {
                Log.d(TAG,"Error Found stopping server socket");
                System.exit(-1);
            }
        }).start();
    }

    public static SocketHandler getInstance (Context context) {
        if (instance == null) {
            instance = new SocketHandler(context);
        }

        return instance;
    }

    class ClientServiceThread extends Thread {
        Socket myClientSocket;
        ArrayList<EnergyMeter> undefinedEnergyMeters = DataHolder.getInstance().getUndefinedDevicesList();
        ArrayList<EnergyMeter> savedEnergyMeters = DataHolder.getInstance().getSavedDevicesList();

        InputStream inputStream = null;
        byte[] responseBytes;

        ClientServiceThread(Socket s) {
            myClientSocket = s;

            try {
                inputStream = myClientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            Log.d(TAG, "Accepted Client Address - " + myClientSocket.getInetAddress().getHostName());

            try {
                // Read from the InputStream
                Thread.sleep(Constants.NORM_DELAY);
                bytes = inputStream.read(buffer);
                responseBytes = Arrays.copyOf(buffer, bytes);

                Log.d(TAG, "Meter connected: " + new String(responseBytes));

                UndefinedDevicesListAdapter undefinedDevicesListAdapter = UndefinedDevicesListAdapter.getInstance();
                SavedDevicesListAdapter savedDevicesListAdapter = SavedDevicesListAdapter.getInstance();

                if (!isInitialCommand(responseBytes)) {
                    Log.e(TAG, "Not an initial command: " + new String(responseBytes));
                    inputStream.close();
                    myClientSocket.close();
                    return;
                }

                String mtrNo = getSerialNo(responseBytes);
                int make = getMake(responseBytes);

                EnergyMeter device = getSavedDevice(mtrNo);
                if (device != null) {
                    // logic to update existing meters
                    Log.d(TAG, "existing meter found");
                    if (device.isReadAgain() || device.getReadStatus().equals(Constants.FAILED)) {
                        device.setReadAgain(false);
                        device.setReadStatus(Constants.PROGRESS);
                        savedDevicesListAdapter.updateList();
                        startMeterThread(device);
                    }

                    else {
                        Log.d(TAG, "Rejected meter: " + new String(responseBytes));
                        Log.d(TAG, "Connected devices: " + undefinedEnergyMeters.toString());
                        inputStream.close();
                        myClientSocket.close();
                    }
                } else {
                    // logic to update undefined meters
                    Log.d(TAG, "undefined meter found");
                    device = getUndefinedDevice(mtrNo);
                    if (device == null) {
                        Log.d(TAG, "Adding new device to the list");
                        EnergyMeter newDevice = new EnergyMeter();
                        newDevice.setMtrNo(mtrNo);
                        newDevice.setMake(make);
                        newDevice.setReadAgain(false);
                        newDevice.setReadStatus(Constants.PROGRESS);
                        newDevice.setIpAddr(myClientSocket.getInetAddress().getHostAddress());
                        undefinedEnergyMeters.add(newDevice);
                        newDevice.setIndex(undefinedEnergyMeters.size());

                        DatabaseHandler.getInstance().addUndefinedMeter(newDevice.getMtrNo(), newDevice.getMake());

                        undefinedDevicesListAdapter.updateList();
                        startMeterThread(newDevice);
                        return;
                    }

                    Log.d(TAG, "isReadAgain: " + device.isReadAgain());

                    if (device.isReadAgain() || device.getReadStatus().equals(Constants.FAILED)) {
                        device.setReadAgain(false);
                        device.setReadStatus(Constants.PROGRESS);
                        undefinedDevicesListAdapter.updateList();
                        startMeterThread(device);
                    }

                    else {
                        Log.d(TAG, "Rejected meter: " + new String(responseBytes));
                        Log.d(TAG, "Connected devices: " + undefinedEnergyMeters.toString());
                        inputStream.close();
                        myClientSocket.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.d(TAG, "...Stopped");
            }
        } // end of run

        boolean isInitialCommand(byte[] cmdBytes) {
            String cmd = new String(cmdBytes);
            try {
                String[] strArr = cmd.split(",");
                int meterType = Integer.parseInt(strArr[0]);

                if (strArr[1].length() > 0) {
                    return true;
                }
            } catch (StringIndexOutOfBoundsException e) {
                Log.e(TAG, "Given String: " + cmd);
                e.printStackTrace();
            } catch (NumberFormatException e) {
                Log.e(TAG, "Not a valid meter type in received string: " + cmd);
            }

            return false;
        }

        int getMake(byte[] cmdBytes) {
            String cmd = new String(cmdBytes);
            String[] strArr = cmd.split(",");
            return Integer.parseInt(strArr[0]);
        }

        String getSerialNo(byte[] cmdBytes) {
            String cmd = new String(cmdBytes);
            String[] strArr = cmd.split(",");
            return strArr[1].trim();
        }

        void startMeterThread(EnergyMeter device) {
            if (device == null) {
                Log.d(TAG, "Passed null device to startMeterThread()");
                return;
            }

            switch (device.getMake()) {
                case Constants.GENUS:
                    GenusServiceThread gst = new GenusServiceThread(myClientSocket, device);
                    gst.start();
                    Log.d(TAG, "GENUS meter connected");
                    break;
                case Constants.LNT:
                    LNTServiceThread lst = new LNTServiceThread(myClientSocket, device);
                    lst.start();
                    Log.d(TAG, "LNT meter connected");
                    break;
                case Constants.SECURE:
                    SecureMeterServiceThread secureMeterServiceThread = new SecureMeterServiceThread(myClientSocket, device);
                    secureMeterServiceThread.start();
                    Log.d(TAG, "SECURE meter connected");
                    break;
                case Constants.SECURE_DLMS:
                    SecureDlmsServiceThread secureDlmsServiceThread = new SecureDlmsServiceThread(myClientSocket, device);
                    secureDlmsServiceThread.start();
                    Log.d(TAG, "SECURE_DLMS meter connected");
                    break;
                case Constants.LPG:
                    LPlusGServiceThread lPlusGServiceThread = new LPlusGServiceThread(myClientSocket, device);
                    lPlusGServiceThread.start();
                    Log.d(TAG, "LPG meter connected");
                    break;
                case Constants.LNT_DLMS:
                    LNTDlmsServiceThread lntDlmsServiceThread = new LNTDlmsServiceThread(myClientSocket, device);
                    lntDlmsServiceThread.start();
                    Log.d(TAG, "LNT_DLMS meter connected");
                    break;
                case Constants.LNT2:
                    LNT2ServiceThread lnt2ServiceThread = new LNT2ServiceThread(myClientSocket, device);
                    lnt2ServiceThread.start();
                    Log.d(TAG, "LNT2 meter connected");
                    break;
                case Constants.LNT_LEGACY:
                    LNTLegacyServiceThread lntLegacyServiceThread = new LNTLegacyServiceThread(myClientSocket, device);
                    lntLegacyServiceThread.start();
                    Log.d(TAG, "LNT_LEGACY meter connected");
                    break;
                default:
                    Log.e(TAG, "Cannot find meter: " + device.getMake());
            }
        }

        EnergyMeter getUndefinedDevice(String mtrNo) {
            EnergyMeter device;
            Log.e(TAG, "undefinedEnergyMeters: " + undefinedEnergyMeters.toString());
            for (int i = 0; i < undefinedEnergyMeters.size(); i++) {
                device = undefinedEnergyMeters.get(i);
                if (mtrNo.equals(device.getMtrNo())) {
                    return device;
                }
            }

            return null;
        }

        EnergyMeter getSavedDevice(String mtrNo) {
            EnergyMeter device;
            for (int i = 0; i < savedEnergyMeters.size(); i++) {
                device = savedEnergyMeters.get(i);
                Log.d(TAG, "meter no: " + device.getMtrNo());
                if (mtrNo.equals(device.getMtrNo())) {
                    return device;
                }
            }

            return null;
        }
    }

    public void onDevicesFound(MyConsumer<Boolean> handler) {
        onDevicesFoundHandler = handler;
    }
}
