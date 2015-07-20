package com.tinycircuits.tinycircuitsble.fragments;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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
import com.tinycircuits.tinycircuitsble.tools.Debug;
import com.tinycircuits.tinycircuitsble.tools.IntentData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nic on 7/10/2015.
 *
 * Specifically targets API 21+
 */
@TargetApi(21)
public class LollipopBLEScannerFragment extends BLEScannerFragment {


    private final List<ScanFilter> filters = new ArrayList<>();

    private final ScanSettings settings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private View rootView;

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(int i = 0; i < results.size(); ++i){
                mLeDeviceListAdapter.addDevice(results.get(i).getDevice());
            }
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            Debug.ShowText(getActivity(), "Scan Failed, Try again.");
        }
    };

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner scanner;

    @Override
    public void startScan(){
        if(scanner != null) {
            callback.scanStarted();

            Handler handler = new Handler();
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, SCAN_PERIOD);

            scanner.startScan(filters, settings, bleScanCallback);


        }
    }

    @Override
    public void stopScan(){
        if(scanner != null) {
            callback.scanStopped();
            scanner.stopScan(bleScanCallback);
        }
    }

    public LollipopBLEScannerFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        rootView = inflater.inflate(R.layout.frag_scan_list, container, false);

        mBluetoothAdapter = ((BluetoothManager) getActivity().
                getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();


        checkCreateState(getActivity(), mBluetoothAdapter);
        scanner = mBluetoothAdapter.getBluetoothLeScanner();

        return rootView;
    }


    @Override
    public void onPause(){
        super.onPause();

        stopScan();
        if(mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

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
                stopScan();

                BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);

                Intent intent = new Intent(getActivity().getApplicationContext(), ControlActivity.class);
                intent.putExtra(IntentData.NAME_DATA, device.getName());
                intent.putExtra(IntentData.ADDRESS_DATA, device.getAddress());
                startActivity(intent);
            }
        });
    }
}
