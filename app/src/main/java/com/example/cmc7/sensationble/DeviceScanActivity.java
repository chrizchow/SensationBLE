package com.example.cmc7.sensationble;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceScanActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private TextView scan_description;
    private ListView scan_listView;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;

    //This is the "starting point" of this Activity.
    //It will be called by Android automatically, just like main():
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Create the basic layout thing:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        //Link the Description TextView after the screen is initialized:
        scan_description = (TextView)findViewById(R.id.textView_scan_description);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    //This function will run after onCreate(), or when user switch back from other app:
    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        scan_listView = (ListView) findViewById(R.id.listView);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        scan_listView.setAdapter(mLeDeviceListAdapter);
        scan_listView.setOnItemClickListener(this);


        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

    }

    //This function runs after using responding with the startActivityForResult():
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //This function runs when user suspend the app:
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    //This function runs when Android needs to create menu bar,
    //it should be called automatically during init of the activity:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_devicescan, menu);
        if(!mScanning){
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
            scan_description.setText(R.string.scan_description);
        }else{
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
            scan_description.setText(R.string.scan_scanning);
        }
        return true;
    }

    //This is the on-click listener that will run when user selects something in the action bar:
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (item.getItemId()){
            case R.id.action_scan:
                    mLeDeviceListAdapter.clear();
                    scanLeDevice(true);
                    break;
            case R.id.action_stop:
                    scanLeDevice(false);
                    break;
            case R.id.action_about:
                aboutDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //it scans forever, it's quite power eating.
            //we do this because we want to ensure it scans something:
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    // This function will be called by Bluetooth API when Android successfully find BLE devices:
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    // This function shows the "About" dialog. Nothing really magical:
    private void aboutDialog(){
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.about_message)
                .setTitle(R.string.about_title);
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();

    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //Take the BluetoothDevice object and check it:
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);
        if (device == null) return;

        //Prepare an intent to fire a new activity:
        final Intent intent = new Intent(this, DeviceControlActivity.class);

        //Stop scanning to save power:
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }

        //Start the activity:
        Toast.makeText(this, "Connecting", Toast.LENGTH_SHORT).show();
        startActivity(intent);


    }
}
