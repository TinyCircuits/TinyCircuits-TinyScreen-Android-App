package com.tinycircuits.tinycircuitsble.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.tinycircuits.tinycircuitsble.ControlActivity;
import com.tinycircuits.tinycircuitsble.R;
import com.tinycircuits.tinycircuitsble.ScanActivity;
import com.tinycircuits.tinycircuitsble.interfaces.UIStateCallback;
import com.tinycircuits.tinycircuitsble.notificationservice.NLService;
import com.tinycircuits.tinycircuitsble.tools.Debug;
import com.tinycircuits.tinycircuitsble.tools.IntentData;

import java.util.List;

/**
 * Created by Nic on 7/8/2015.
 *
 * Abstract adapter between Android 4.3 - 4.4  and 5.0
 *
 */
public abstract class BLEScannerFragment extends Fragment {

    /**
     * Generic start for children
     */
    public abstract void startScan();

    /**
     * Generic stop for children
     */
    public abstract void stopScan();

    /**
     * callback used to update Main acivity's UI
     */
    protected UIStateCallback callback;

    /**
     * sets the callback for UI updates while scanning
     * @param cb
     */
    public void setCallback(UIStateCallback cb){
        callback = cb;
    }

    protected final long SCAN_PERIOD = 5000;

    public static final String NOTIFICATION_SETTINGS_INTENT =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";


    /**
     * Uses the context and adapter to see if bluetooth is still connected, enabled etc. and
     * checks if the notification listener is enabled
     * @param adapter
     * @param context
     */
    public void checkResumeState(BluetoothAdapter adapter, final Context context){

        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)context).startActivityForResult(enableBtIntent, ScanActivity.REQUEST_ENABLE_BT);
        }else{

            if (checkNotificationsEnabled(context)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.alert_title);
                builder.setIcon(R.drawable.tiny_circuits_logo);
                builder.setMessage(R.string.alert_message);
                builder.setNegativeButton(R.string.alert_neg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Debug.ShowText(context,
                                "You must enable notifications in order for the app to work properly.");
                        ((Activity)context).finish();
                    }
                });
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        context.startActivity(new Intent(NOTIFICATION_SETTINGS_INTENT));
                    }
                });
                builder.create().show();
            }
        }
    }

    /**
     * checks the created state if any devices are connected, if ble is supported.
     * Launches ControlActivity if there is a device connected
     * @param context
     * @param adapter
     */
    public void checkCreateState(Context context, BluetoothAdapter adapter){

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Debug.ShowText(context, context.getString(R.string.bt_not_supported));
            ((Activity)context).finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        // Checks if Bluetooth is supported on the device.
        if (adapter == null) {
            Debug.ShowText(context, context.getString(R.string.bt_not_supported));
            ((Activity)context).finish();
            return;
        }

        List<BluetoothDevice> tempList = bluetoothManager.getConnectedDevices(BluetoothGatt.GATT);
        if(!tempList.isEmpty()){
            Intent intent = new Intent(context, ControlActivity.class);
            intent.putExtra(IntentData.NAME_DATA, tempList.get(0).getName());
            intent.putExtra(IntentData.ADDRESS_DATA, tempList.get(0).getAddress());
            intent.putExtra(IntentData.IS_CONNECTED, true);
            context.startActivity(intent);
        }
    }

    /**
     * If true, then the notification service is enabled.
     * If false, we must allow them to enable it.
     * @return true or false if NLService is enabled
     */
    private boolean checkNotificationsEnabled(Context c){
        ContentResolver contentResolver = c.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = NLService.class.getName();

        //Debug.Log(packageName);

        return (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));

    }

}
