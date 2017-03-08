package com.tinycircuits.tinycircuitsble;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.tinycircuits.tinycircuitsble.fragments.KitKatBLEScannerFragment;
import com.tinycircuits.tinycircuitsble.fragments.LollipopBLEScannerFragment;
import com.tinycircuits.tinycircuitsble.fragments.BLEScannerFragment;
import com.tinycircuits.tinycircuitsble.interfaces.UIStateCallback;
import com.tinycircuits.tinycircuitsble.tools.Debug;


/**
 * Created by Nic on 8/27/2014.
 */
public class ScanActivity extends Activity implements UIStateCallback{

    private boolean isScanning = false;

    public static final int REQUEST_ENABLE_BT = 200;

    private BLEScannerFragment fragment;

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);

        setContentView(R.layout.scan_activity);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            //18-20
            fragment = new KitKatBLEScannerFragment();
        }else{
            //21+
            fragment = new LollipopBLEScannerFragment();
        }


        fragment.setCallback(this);
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();

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
                fragment.startScan();
                break;
            case R.id.menu_stop:
                fragment.stopScan();
                break;
            case R.id.settings:
                //start and stop are implicitly called onPause()
                startActivity(new Intent(BLEScannerFragment.NOTIFICATION_SETTINGS_INTENT));
        }
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void scanStarted() {
        isScanning = true;
        invalidateOptionsMenu();
    }

    @Override
    public void scanStopped() {
        isScanning = false;
        invalidateOptionsMenu();
    }
}
