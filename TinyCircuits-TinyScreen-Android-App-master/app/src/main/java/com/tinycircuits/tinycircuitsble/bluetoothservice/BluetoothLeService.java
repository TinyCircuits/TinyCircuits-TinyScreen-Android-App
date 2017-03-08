package com.tinycircuits.tinycircuitsble.bluetoothservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Binder;
import android.os.IBinder;

import com.tinycircuits.tinycircuitsble.R;
import com.tinycircuits.tinycircuitsble.ScanActivity;
import com.tinycircuits.tinycircuitsble.helpers.DataFormat;
import com.tinycircuits.tinycircuitsble.notificationservice.NLService;
import com.tinycircuits.tinycircuitsble.tools.Debug;
import com.tinycircuits.tinycircuitsble.tools.IntentData;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

/**
 * Created by Nic on 8/29/2014.
 */
public class BluetoothLeService extends Service {

    private String mBluetoothDeviceAddress;
    public String appString="Phone Messaging Message";
    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private Intent notifyIntent;

    private Notification notification;

    private NotificationManager notificationManager;

    private final int NOTIFICATION_ID = 999;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";

    public final static String ACTION_BUSY =
            "com.example.bluetooth.le.BUSY";

    public final static String NOTIFICATION_POSTED =
            "com.example.notification.NOTIFICATION_POSTED";

    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "com.example.bluetooth.le.DEVICE_DOES_NOT_SUPPORT_UART";

    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private IBinder binder = new LocalBinder();

    public boolean checkAppString(String app){
        String[] appNames = appString.split("\\s+");
        for( int i = 0; i < appNames.length; i++){
            if(app.contains(appNames[i]))return true;
        }
        return false;
    }


    /**
     * Receives notifications from NLService as intents
     */

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.hasExtra(IntentData.NOTIFICATION_DATA)
                    && intent.hasExtra(IntentData.NOTIFICATION_SENDER)
                    && intent.hasExtra(IntentData.NOTIFICATION_TITLE)
                    && intent.getAction().equals(NOTIFICATION_POSTED)){
                String text = intent.getStringExtra(IntentData.NOTIFICATION_DATA);
                String sender = intent.getStringExtra(IntentData.NOTIFICATION_SENDER);
                String title = intent.getStringExtra(IntentData.NOTIFICATION_TITLE);

                if(checkAppString(getApplicationName(sender))){
                    if(getApplicationName(sender).equals("Phone") && title.length()<3) {
                        String messageString = "1Incoming Call";
                        sendData(DataFormat.TrimText(messageString));
                    }else {
                        String nameString = "1" + title + ":";
                        sendData(DataFormat.TrimText(nameString));
                        String messageString = "2" + text;

                        try {
                            Thread.sleep(100);
                            sendData(DataFormat.TrimText(messageString));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                String output= getApplicationName(sender) + " : " + title + " : " + text;
                Debug.Log(output);
            }
        }
    };



    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Debug.Log("client is bound");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //Debug.Log("Bluetooth service started");
        //connected, start listening for notifications
        if(notifyIntent == null)
            notifyIntent = new Intent(BluetoothLeService.this, NLService.class);
        startService(notifyIntent);
        registerReceiver(notificationReceiver, addNotificationIntentFilter());
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(notifyIntent);
        notificationManager.cancel(NOTIFICATION_ID);
        unregisterReceiver(notificationReceiver);
        close();
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Debug.Log("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Debug.Log("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {

        //tells the UI that its trying to do something
        broadcastUpdate(ACTION_BUSY);

        if (mBluetoothAdapter == null || address == null) {
            Debug.Log("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }


        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Debug.Log("Device not found.  Unable to connect.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        // This tends to take a longer time for whatever reason.
        // ACTION_BUSY updates the UI until a CONNECTED status is received
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Debug.Log("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                buildStickyNotification(device.getName());
                return true;
            } else {
                return false;
            }
        }


        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Debug.Log("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        buildStickyNotification(device.getName());
        return true;
    }

    /**
     *
     * @param deviceName The name of the connected device
     *
     * Builds a sticky (persistent) notification based on the device info
     * Allows user to click on notification and view device details.
     * Will be removed (canceled) upon disconnecting
     */
    private void buildStickyNotification(String deviceName){
        Intent intent = new Intent(this, ScanActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                intent,
                0);

        notification = new Notification.Builder(this)
            .setContentTitle( getString(R.string.connected) )
            .setContentText(deviceName)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.tiny_circuits_logo)
            .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
        //Debug.Log("notification should be posted now: " + device.getName());
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if ( bluetoothNull() ) {
            Debug.Log("BluetoothAdapter not initialized");
            return;
        }
        //tells the UI that its trying to do something
        broadcastUpdate(ACTION_BUSY);

        mBluetoothGatt.disconnect();
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void close() {
        if (mBluetoothGatt == null) {
            Debug.Log("bluetooth gatt is null already");
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Callback will broadcast right now but nothing will be caught in @file ControlService.java
     *  because it does not have an intent filter or a broadcast receiver object
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Debug.Log("Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Debug.Log("Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Debug.Log("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }
    };

    private void broadcastUpdate(final String action) {

        final Intent intent = new Intent(action);
        sendBroadcast(intent);

    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ( bluetoothNull() ) {
            Debug.Log("BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Checks if the {@code mBluetoothAdapter} or {@code mBluetoothGatt} are null.
     * @return a boolean if one is null
     */
    private boolean bluetoothNull(){
        return (mBluetoothAdapter == null || mBluetoothGatt == null);
    }

    /**
     * Adds notification filter to service so it can receive broadcasts from
     * {@code com.tinycurcuits.tinycircuitsble.notificationservice$NLService}
     * @return an intent filter to grab a specific intent
     */
    private static IntentFilter addNotificationIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.NOTIFICATION_POSTED);
        return intentFilter;
    }

    /**
     *
     * @param message message to send to bluetooth le device
     * @param sender what notification sent the message
     */
    public void sendData(String data) {

        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        //showMessage("mBluetoothGatt null"+ mBluetoothGatt);
        if (RxService == null) {
            //showMessage("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            //showMessage("Rx characteristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        RxChar.setValue(data.getBytes(Charset.forName("UTF-8")));
        boolean status = writeCharacteristic(RxChar);

        Debug.Log("sending " + data);
    }

    /**
     *
     * @param contextName name of the context would be something like {@code com.app.example}.
     *                    We can use this intent string to get the string label
     *                    that is defined for that application.
     *                    If it does not exist, then return {@code ""}
     * @return user friendly app name, or empty string
     */
    private String getApplicationName(String contextName){
        String appName = "";
        try {

            Context context = createPackageContext(contextName, Context.CONTEXT_IGNORE_SECURITY);
            int labelResId = context.getApplicationInfo().labelRes;
            appName = context.getString(labelResId);
        }catch (PackageManager.NameNotFoundException e){
            Debug.Log("Could not get the package context for: " + contextName + "\n" + e.getMessage());
        }catch(Resources.NotFoundException e){
            Debug.Log("App name label for " + contextName + " is not defined as a string resource.\n"  + e.getMessage());
        }
        return appName;
    }

}
