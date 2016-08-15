package com.example.cmc7.sensationble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BLEService extends Service {
    //For diagnostic logcat:
    private final static String TAG = BLEService.class.getSimpleName();

    //For Bluetooth API Connectivity:
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    //private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    //IBinder for Service-Activity Communication:
    private final IBinder mBinder = new LocalBinder();

    //For remembering connection state:
    private int mConnectionState = STATE_DISCONNECTED;

    //For notification showing:
    NotificationCompat.Builder mBuilder;

    //For defining different connection state:
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    //For defining different broadcast message:
    public final static String ACTION_GATT_CONNECTED = "SensationBLE.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "SensationBLE.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_WRITE_SUCCESSFUL = "SensationBLE.ACTION_GATT_WRITE_SUCCESSFUL";
    public final static String ACTION_GATT_WRITE_FAILED = "SensationBLE.ACTION_GATT_WRITE_FAILED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "SensationBLE.ACTION_GATT_SERVICES_DISCOVERED";

    public final static String ACTION_DATA_AVAILABLE = "SensationBLE.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA_UUID = "SensationBLE.EXTRA_DATA_UUID";
    public final static String EXTRA_DATA_BYTES = "SensationBLE.EXTRA_DATA_BYTES";

    //Bluetooth LE API Callback function
    //As BLE is asynchronous, Android will call back this object after finishing execution:
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        //this will be called if there's any connection changes (e.g. connected, disconnected...):
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch(newState){
                case BluetoothProfile.STATE_CONNECTED:
                    mConnectionState = STATE_CONNECTED;     //oh, it's connected now
                    broadcastUpdate(ACTION_GATT_CONNECTED); //broadcast to let others know
                    Log.i(TAG, "SENSATION:  Connected to GATT server.");

                    // Attempts to discover services after successful connection.
                    if(mBluetoothGatt.discoverServices())
                        Log.i(TAG, "SENSATION:  Attempting to start service discovery...");
                    else
                        Log.e(TAG, "SENSATION:  Failed to start service discovery!");

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mConnectionState = STATE_DISCONNECTED;      //oh it's disconnected
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);  //broadcast to let others know
                    Log.i(TAG, "SENSATION:  Disconnected from GATT server.");
                    break;
                default:
                    mConnectionState = STATE_DISCONNECTED;      //treat this as disconnected
                    Log.e(TAG, "SENSATION:  onConnectionStateChange: Strange thing - "+status); //i don't know
                    break;

            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            switch(status){
                case BluetoothGatt.GATT_SUCCESS:
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);   //oh! successful found services
                    Log.i(TAG, "SENSATION:  onServicesDiscovered successfully");
                    break;
                default:
                    Log.e(TAG, "SENSATION:  onServicesDiscovered: Strange Thing - "+status);
                    break;

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);  //send a broadcast to read content inside
            }else{
                Log.e(TAG, "SENSATION:  onCharacteristicRead: Strange Thing - "+status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_WRITE_SUCCESSFUL, characteristic); //send a broadcast to indicate changes
            }else{
                broadcastUpdate(ACTION_GATT_WRITE_FAILED);
                Log.e(TAG, "SENSATION:  onCharacteristicWrite: other state - "+status);
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic); //send a broadcast to update changes
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            //We don't care this in this project. Nothing needs to be done.
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            //We don't care this in this project. Nothing needs to be done.
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            //We don't care this in this project. Nothing needs to be done.
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            //We don't care this in this project. Nothing needs to be done.
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            //We don't care this in this project. Nothing needs to be done.
        }
    };


    //This function will be called if it's not running and someone calls startService():
    @Override
    public void onCreate(){
        Log.i(TAG, "SENSATION:  onCreate!");
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "SENSATION:  onDestroy!");
        super.onDestroy();
    }

    //Return the IBinder of BLEService if anyone binds this service:
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "SENSATION:  onBind!");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        Log.i(TAG, "SENSATION:  onUnbind!");
        return super.onUnbind(intent);
    }

    //Broadcast - broadcast message to all other activities:
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //Broadcast - broadcast message with characteristic content to all other activities:
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA_BYTES, characteristic.getValue());
        sendBroadcast(intent);

    }

    //Read characteristic for given characteristic:
    //The result will asynchronously reported through onCharacteristicRead:
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "SENSATION:  BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    //Retrieves a list of supported GATT services on the connected device.
    //This should be called after mBluetoothGatt.discoverServices() completes:
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    //Retrieve a specific GATT service on the connected device.
    //It is more efficient than getting all services.
    //This should be called after mBluetoothGatt.discoverServices() completes:
    public BluetoothGattService getSpecificGattService(UUID uuid){
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getService(uuid);
    }



    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    //Initialize Bluetooth API
    //if the returned value is false, this service will fail.
    //You should unbind and disconnect this service to destroy it:
    public boolean initialize(){
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "SENSATION:  Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "SENSATION:  Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    //Connect to a BluetoothDevice.
    //You can call this function in DeviceScanActivity using IBinder:
    public boolean connect(BluetoothDevice device){
        //Check existence of bluetooth adapter:
        if(mBluetoothAdapter==null){
            Log.e(TAG, "SENSATION:  Cannot connect: bluetooth adapter object is null.");
            mConnectionState = STATE_DISCONNECTED;
            return false;
        }

        //Check legitimacy of BluetoothDevice:
        if(device == null || device.getAddress()==null){
            mConnectionState = STATE_DISCONNECTED;
            return false;
        }

        //Check if this service has been used to connect devices before:
        if(mBluetoothDevice != null || mBluetoothGatt!=null){
            //Properly close and turn off the device
            //close();

            //Disconnect the device if mBluetoothGatt is not null, just to play safe:
            //Remark: Android API 18 can only connect to 5 GATT devices simultaneously
            //mBluetoothGatt.disconnect();
            //mConnectionState = STATE_DISCONNECTED;

            //If the same device is connected previously, simply reconnect:
            if(mBluetoothDevice.getAddress().equals(device.getAddress())){
                Log.d(TAG, "SENSATION:  Trying to use an existing mBluetoothGatt for connection.");
                if(mBluetoothGatt.connect()){
                    mConnectionState = STATE_CONNECTING;
                    return true;
                }else{
                    mConnectionState = STATE_DISCONNECTED;
                    return false;
                }
            }

        }

        //Connect to the BluetoothDevice:
        mBluetoothDevice = device;
        mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "SENSATION:  Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        return true;

    }

    //Close connection to release the resources and free the BLE device properly.
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();  //free BLE device
        mBluetoothGatt = null;   //release resources
        mBluetoothDevice = null; //clear the device record in this service
    }

    //Get current connection state:
    public int getConnectionState(){
        return mConnectionState;
    }

    //Get connected device name:
    public String getConnectedGattDeviceName(){
        if(mBluetoothDevice!=null){
            return mBluetoothDevice.getName();
        }else{
            return null;
        }
    }

    /*
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //if (BLEService.ACTION_GATT_CONNECT_DEVICE.equals(action)) {
            //}
        }
    };

    //The intent (broadcast) filter:
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(BLEService.ACTION_GATT_CONNECT_DEVICE);
        return intentFilter;
    }
    */


}
