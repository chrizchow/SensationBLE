package com.example.cmc7.sensationble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class DeviceControlActivity extends AppCompatActivity {
    TextView debug_txtView; //debug code

    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private BLEService mBLEService;

    // IBinder Connection Object to manage Service lifecycle:
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if(mBLEService!=null){

                //Finish this activity if the connection statue is disconnected
                //It should not happen because checking should be done at DeviceScanActivity:
                if(mBLEService.getConnectionState()==BLEService.STATE_DISCONNECTED){
                    Log.e(TAG, "SENSATION: Connection State is disconnected while binding to DeviceControlActivity?!");
                    Toast.makeText(DeviceControlActivity.this, "Please force-stop this app", Toast.LENGTH_SHORT).show();
                    finish();
                }

                //Temporarily title this activity with device name:
                String deviceName = mBLEService.getConnectedGattDeviceName();
                if(deviceName!=null) getActionBar().setTitle(deviceName);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "SENSATION: onServiceDisconnected in DeviceControlActivity");
            mBLEService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        debug_txtView = (TextView) findViewById(R.id.textView);

        //TOOD: Under Construction
        debug_txtView.append("This area shows updates on GATT Things.\n");
    }

    //This function will run after onCreate(), or when user switch back from other app:
    @Override
    protected void onResume() {
        super.onResume();

        // Open the broadcast receiver:
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Bind the service using IBinder, so that this activity can call BLEService's functions:
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    //This function runs when user suspend the app:
    @Override
    protected void onPause() {
        super.onPause();
        //Unbind the service:
        unbindService(mServiceConnection);
        mBLEService = null;
        //Unregister broadcast receiver:
        unregisterReceiver(mGattUpdateReceiver);

    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //Show string if the GATT has already disconnected:
                debug_txtView.append("GATT has been disconnected!\n");

                //finish();
                //Toast.makeText(DeviceControlActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }else if(BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                //Show it to screen:
                debug_txtView.append("GATT Services Discovered!\n");
                List<BluetoothGattService> serviceList = mBLEService.getSupportedGattServices();
                for(BluetoothGattService service: serviceList){
                    debug_txtView.append("\tService UUID:" + service.getUuid().toString()+"\n");
                }


            }else if(BLEService.ACTION_DATA_AVAILABLE.equals(action)){
                //Take the available data:
                String uuid = intent.getStringExtra(BLEService.EXTRA_DATA_UUID);
                byte[] bytes = intent.getByteArrayExtra(BLEService.EXTRA_DATA_BYTES);

                //Show it to screen:
                debug_txtView.append("\t\tCharacteristic UUID: " + uuid +"\n");
                debug_txtView.append("\t\t\tContent:" + new String(bytes) + "\n");

            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BLEService.ACTION_GATT_WRITE_SUCCESSFUL);
        intentFilter.addAction(BLEService.ACTION_GATT_WRITE_FAILED);

        return intentFilter;
    }


}
