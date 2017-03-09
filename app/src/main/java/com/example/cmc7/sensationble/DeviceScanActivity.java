package com.example.cmc7.sensationble;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.support.annotation.NonNull;

import android.support.v4.app.ActivityCompat;


public class DeviceScanActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static  DeviceScanActivity mInstance = null;
    public String username = "User1";
    public int weight = 50;
    public int calories = 0;
    public int steps_goal = 12500;
    protected DeviceScanActivity(){}
    public static synchronized DeviceScanActivity getInstance(){
        if(null == mInstance){
            mInstance = new DeviceScanActivity();
        }
        return mInstance;
    }

    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private TextView scan_description;
    private ListView scan_listView;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BLEService mBLEService;
    private ProgressDialog progress;

    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;

    // IBinder Connection Object to manage Service lifecycle:
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
                Log.e(TAG, "SENSATION: Unable to initialize BLE Class");
                finish();
            }else{
                if(mBLEService.getConnectionState()==BLEService.STATE_CONNECTED){
                    startActivity(new Intent(DeviceScanActivity.this, DeviceControlActivity.class));
                    finish();
                    Toast.makeText(DeviceScanActivity.this, "Already connected!", Toast.LENGTH_SHORT).show();

                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService = null;
        }
    };

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

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number can be used
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

        // Open the broadcast receiver:
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Bind the service using IBinder, so that this activity can call BLEService's functions:
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

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
        //Disable BLE Scanning:
        scanLeDevice(false);
        //Clear the Device List:
        mLeDeviceListAdapter.clear();
        //Unbind the service:
        unbindService(mServiceConnection);
        mBLEService = null;
        //Unregister broadcast receiver:
        unregisterReceiver(mGattUpdateReceiver);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    //Start your service here

                    // Start the BLEService Service if it's not started:
                    Intent gattServiceIntent = new Intent(this, BLEService.class);
                    startService(gattServiceIntent);

                }
            }
        }
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

        //Connect the device, if broadcast is received, start the activity:
        mBLEService.connect(device);
        progress = ProgressDialog.show(this, "Please Wait", "Connecting to GATT Server...", true);


    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                progress.dismiss();
                finish();
                startActivity(new Intent(DeviceScanActivity.this, DeviceControlActivity.class));
            }else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)){
                progress.dismiss();
                Toast.makeText(context, "BLE Device Timeout!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        return intentFilter;
    }
}
