package com.tinycircuits.tinycircuitsble;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.tinycircuits.tinycircuitsble.helpers.LeDeviceListAdapter;
import com.tinycircuits.tinycircuitsble.notificationservice.NLService;
import com.tinycircuits.tinycircuitsble.tools.Debug;
import com.tinycircuits.tinycircuitsble.tools.IntentData;


import java.util.List;


/**
 * Created by Nic on 8/27/2014.
 */
public class ScanActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean isScanning = false;
    private Handler handler;

    private final String NOTIFICATION_SETTINGS_INTENT =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;

    private static final int REQUEST_ENABLE_BT = 200;
    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);

        setContentView(R.layout.scan_activity);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Debug.ShowText(getApplicationContext(), getString(R.string.bt_not_supported));
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Debug.ShowText(getApplicationContext(), getString(R.string.bt_not_supported));
            finish();
            return;
        }

        List<BluetoothDevice> tempList = bluetoothManager.getConnectedDevices(BluetoothGatt.GATT);
        if(!tempList.isEmpty()){
            Intent intent = new Intent(ScanActivity.this, ControlActivity.class);
            intent.putExtra(IntentData.NAME_DATA, tempList.get(0).getName());
            intent.putExtra(IntentData.ADDRESS_DATA, tempList.get(0).getAddress());
            intent.putExtra(IntentData.IS_CONNECTED, true);
            startActivity(intent);
        }


    }

    /**
     * If true, then the notification service is enabled.
     * If false, we must allow them to enable it.
     * @return true or false if NLService is enabled
     */
    private boolean checkNotificationsEnabled(){
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = NLService.class.getName();

        //Debug.Log(packageName);

        return (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Debug.ShowText(getApplicationContext(), getString(R.string.bt_enabled));
            finish();
            return;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler = new Handler();
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Debug.ShowText(getApplicationContext(),"scan timed out after " + SCAN_PERIOD/1000 + " seconds.");
                    isScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            isScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        if (!isScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.scanning_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.settings:
                startActivity(new Intent(NOTIFICATION_SETTINGS_INTENT));
        }
        invalidateOptionsMenu();
        return true;
    }

    @Override
    protected void onPause(){
        super.onPause();
        scanLeDevice(false);
        if(mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else {

            ListView deviceList = (ListView) findViewById(R.id.deviceList);

            mLeDeviceListAdapter = new LeDeviceListAdapter(getApplicationContext());
            deviceList.setAdapter(mLeDeviceListAdapter);
            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    scanLeDevice(false);

                    BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);

                    Intent intent = new Intent(ScanActivity.this, ControlActivity.class);
                    intent.putExtra(IntentData.NAME_DATA, device.getName());
                    intent.putExtra(IntentData.ADDRESS_DATA, device.getAddress());
                    startActivity(intent);
                }
            });


            if (checkNotificationsEnabled()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert_title);
                builder.setIcon(R.drawable.tiny_circuits_logo);
                builder.setMessage(R.string.alert_message);
                builder.setNegativeButton(R.string.alert_neg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Debug.ShowText(getApplicationContext(),
                                "You must enable notifications in order for the app to work properly.");
                        finish();
                    }
                });
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(NOTIFICATION_SETTINGS_INTENT));
                    }
                });
                builder.create().show();
            }
        }

    }

}
