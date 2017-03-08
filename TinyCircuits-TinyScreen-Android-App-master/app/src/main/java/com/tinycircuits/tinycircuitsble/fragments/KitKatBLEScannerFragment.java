package com.tinycircuits.tinycircuitsble.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.tinycircuits.tinycircuitsble.ControlActivity;
import com.tinycircuits.tinycircuitsble.R;
import com.tinycircuits.tinycircuitsble.helpers.LeDeviceListAdapter;
import com.tinycircuits.tinycircuitsble.tools.IntentData;

/**
 * Created by Nic on 7/8/2015.
 */
public class KitKatBLEScannerFragment extends BLEScannerFragment {


    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    // Stops scanning after 10 seconds.

    private Handler handler;


    public KitKatBLEScannerFragment(){}

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        rootView = inflater.inflate(R.layout.frag_scan_list, container,false);

        mBluetoothAdapter = ((BluetoothManager) getActivity().
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        checkCreateState(getActivity(), mBluetoothAdapter);


        return rootView;

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
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

                    callback.scanStopped();
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            callback.scanStarted();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            callback.scanStopped();
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        scanLeDevice(false);
        if(mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void startScan(){
        scanLeDevice(true);
    }

    @Override
    public void stopScan(){
        scanLeDevice(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkResumeState(mBluetoothAdapter, getActivity());

        ListView deviceList = (ListView) rootView.findViewById(R.id.deviceList);

        mLeDeviceListAdapter = new LeDeviceListAdapter(getActivity());
        deviceList.setAdapter(mLeDeviceListAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                callback.scanStopped();
                scanLeDevice(false);

                BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);

                Intent intent = new Intent(getActivity(), ControlActivity.class);
                intent.putExtra(IntentData.NAME_DATA, device.getName());
                intent.putExtra(IntentData.ADDRESS_DATA, device.getAddress());
                startActivity(intent);
            }
        });
    }
}
