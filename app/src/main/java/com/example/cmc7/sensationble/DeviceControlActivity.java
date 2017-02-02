package com.example.cmc7.sensationble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.audiofx.AudioEffect;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends AppCompatActivity {
    TextView heartrate_text; //debug code
    TextView step_text;
    TextView button;
    //List<BluetoothGattCharacteristic> chars = new ArrayList<BluetoothGattCharacteristic>();

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
                if(deviceName!=null) getSupportActionBar().setTitle(deviceName);
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
        heartrate_text = (TextView) findViewById(R.id.heartrate);
        step_text = (TextView) findViewById(R.id.step);
        button = (TextView) findViewById(R.id.debug);
        heartrate_text.setText("000");
        step_text.setText("0");
        button.setText(" ");

        //TODO: Under Construction



        Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                button.setText("refreshed");

                BluetoothGattCharacteristic heartRate = mBLEService.getSupportedGattServices().get(3).getCharacteristics().get(0);
                mBLEService.setCharacteristicNotification(heartRate);

            }

        });

        Button time_sync = (Button) findViewById(R.id.time_sync);
        time_sync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                BluetoothGattCharacteristic chara = mBLEService.getSupportedGattServices().get(2).getCharacteristics().get(6);
                Long tsLong = System.currentTimeMillis()/1000;
                tsLong = tsLong - 946656000;
                int tsInt = tsLong.intValue();
                String ts = tsLong.toString();
                button.setText(ts);
                chara.setValue(tsInt,BluetoothGattCharacteristic.FORMAT_UINT32,0);
                mBLEService.writeCharacteristic(chara);

            }
        });

        Button disconnect = (Button) findViewById(R.id.disconnect);
        disconnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                button.setText("disconnected");
                mBLEService.close();
                finish();
                startActivity(new Intent(DeviceControlActivity.this, DeviceScanActivity.class));

            }
        });

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

    protected void requestCharacteristics(List<BluetoothGattCharacteristic> chars){
        mBLEService.readCharacteristic(chars.get(chars.size()-1));
        chars.remove(chars.get(chars.size() - 1));
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //Show string if the GATT has already disconnected:
                heartrate_text.append("GATT has been disconnected!\n");


                //finish();
                //Toast.makeText(DeviceControlActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }else if(BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                //Show it to screen:
                //heartrate_text.append("GATT Services Discovered!\n");
                List<BluetoothGattService> serviceList = mBLEService.getSupportedGattServices();
                for(BluetoothGattService service: serviceList){
                    //heartrate_text.append("\tService UUID:" + service.getUuid().toString()+"\n");
                    Log.i(TAG, "-");
                    List<BluetoothGattCharacteristic> charslist = service.getCharacteristics();
                    for(BluetoothGattCharacteristic chara: charslist){
                        Log.i(TAG,chara.getUuid().toString());
                    }
                }

            }else if(BLEService.ACTION_DATA_AVAILABLE.equals(action)){
                //Take the available data:

                String uuid = intent.getStringExtra(BLEService.EXTRA_DATA_UUID);
                byte[] bytes = intent.getByteArrayExtra(BLEService.EXTRA_DATA_BYTES);

                //Show it to screen:
                String step_id = "0000fff6-0000-1000-8000-00805f9b34fb";
                String heartrate_id = "00002a37-0000-1000-8000-00805f9b34fb";
                //if (uuid.equals(heartrate_id)){
                //    heartrate_text.append("\t\tAction_Data_Avalable\n");     //debug

                //    heartrate_text.append("\t\tCharacteristic UUID: " + uuid + "\n");
                //    heartrate_text.append("\t\t\tContent:" + new String(bytes) + "\n");
                //}

                if(uuid.equals(step_id)){
                    //step_text.append("\t\tAction_Data_Avalable\n");     //debug
                        //D2 04 00 00
                    String str = new String(bytes);
                    Integer step = bytes[0] & 0xFF |
                            (bytes[1] & 0xFF) << 8 |
                            (bytes[2] & 0xFF) << 16 |
                            (bytes[3] & 0xFF) << 24;

                    if(step.toString().length() > 4){
                        step_text.setTextSize(step_text.getTextSize() - 10);
                    }
                    step_text.setText(step.toString());


                }
                else{

                    String str2 = new String(bytes);
                    Integer heart = bytes[0] & 0xFF | (bytes[1] & 0xFF) << 8;
                    if(heart.toString().length() == 1){
                        heartrate_text.setText("00" + heart.toString());
                    }
                    else if(heart.toString().length() == 2){
                        heartrate_text.setText("0" + heart.toString());
                    }
                    else{
                        heartrate_text.setText(heart.toString());
                    }

                    BluetoothGattCharacteristic stepCount = mBLEService.getSupportedGattServices().get(2).getCharacteristics().get(5);
                    mBLEService.readCharacteristic(stepCount);

                }


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
