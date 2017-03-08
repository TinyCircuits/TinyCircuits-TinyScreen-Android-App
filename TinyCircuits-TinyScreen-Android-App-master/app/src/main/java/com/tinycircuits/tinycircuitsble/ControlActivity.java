package com.tinycircuits.tinycircuitsble;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.tinycircuits.tinycircuitsble.bluetoothservice.BluetoothLeService;
import com.tinycircuits.tinycircuitsble.helpers.DataFormat;
import com.tinycircuits.tinycircuitsble.tools.Debug;
import com.tinycircuits.tinycircuitsble.tools.IntentData;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Nic on 8/29/2014.
 */


public class ControlActivity extends Activity {


    private BluetoothLeService mBluetoothLeService;

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;

    public boolean isServiceBound = false;

    public boolean isConnected;
    public boolean isBusy = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        TextView name = (TextView) findViewById(R.id.control_name);
        // TextView address = (TextView)findViewById(R.id.control_address);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        Intent intent = getIntent();

        if (intent.hasExtra(IntentData.NAME_DATA)) {
            mDeviceName = intent.getStringExtra(IntentData.NAME_DATA);
            if (DataFormat.CheckString(mDeviceName)) {
                name.setText(mDeviceName);
            } else {
                //    address.setText(R.string.unknown_name);
            }
        }
        if (intent.hasExtra(IntentData.ADDRESS_DATA)) {
            mDeviceAddress = intent.getStringExtra(IntentData.ADDRESS_DATA);
            if (DataFormat.CheckString(mDeviceAddress)) {
                //    address.setText(mDeviceAddress);
            } else {
                //   address.setText(R.string.unknown_address);
            }
            /*
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                Debug.Log("Connect request result=" + result);
            }
            */
        }
        if (intent.hasExtra(IntentData.IS_CONNECTED)) {
            //gets boolean extra if IS_CONNECTED is passed. default value is FALSE
            isConnected = intent.getBooleanExtra(IntentData.IS_CONNECTED, false);
            updateConnectionState();
        }


        getActionBar().setTitle(R.string.device_control);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        EditText appNameEditor = (EditText)findViewById(R.id.editText);
        appNameEditor.setOnEditorActionListener(new DoneOnEditorActionListener());


        Intent gattServiceIntent = new Intent(ControlActivity.this, BluetoothLeService.class);

        isServiceBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        startService(gattServiceIntent);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Debug.Log("Unable to initialize Bluetooth");
                finish();
            }
            //Debug.ShowText(getApplicationContext(), "Service Connection connected");
            // Automatically connects to the device upon successful start-up initialization.
            if (!isConnected) mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //Debug.ShowText(getApplicationContext(), "Service Connection disconnected");
            mBluetoothLeService = null;
        }
    };


    /**
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
     * or notification operations.
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Debug.Log("gatt connected event");
                isConnected = true;
                isBusy = false;
                updateConnectionState();
                invalidateOptionsMenu();
//                try {
//              /**      Thread.sleep(250);
//                   String currentDate = "D" + new SimpleDateFormat("yyyy MM dd k m s").format(new Date());
//                    mBluetoothLeService.sendData(currentDate);   */
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnected = false;
                isBusy = false;
                updateConnectionState();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_BUSY.equals(action)) {
                isBusy = true;
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (isBusy) {
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.scanning_progress);
        } else {
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        if (isConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent = new Intent(ControlActivity.this, BluetoothLeService.class);
        switch (item.getItemId()) {

            case R.id.menu_connect:
                startService(intent);
                mBluetoothLeService.connect(mDeviceAddress);
                return true;

            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                stopService(intent);
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Wherever isConnected is changed, this should be called afterward
     * <p/>
     * It waits 2.5 seconds before updating. It may take a second or two
     * before it is officially connected/disconnected. It  waits to update
     */
    private void updateConnectionState() {
        mConnectionState.setText(R.string.waiting);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    mConnectionState.setText(R.string.connected);
                } else {
                    mConnectionState.setText(R.string.disconnected);
                }
            }
        }, 1000);

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_BUSY);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService = null;
        if (isServiceBound) {
            unbindService(mServiceConnection);
        }
    }

    public void doSubmit(View view) {
        if (isConnected == true) {
            String currentDate = "D" + new SimpleDateFormat("yyyy MM dd k m s").format(new Date());
            mBluetoothLeService.sendData(currentDate);
        }
    }


    class DoneOnEditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                mBluetoothLeService.appString= ((EditText) findViewById(R.id.editText)).getText().toString();
                //return true;
            }
            return false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Control Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.tinycircuits.tinycircuitsble/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Control Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.tinycircuits.tinycircuitsble/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}

