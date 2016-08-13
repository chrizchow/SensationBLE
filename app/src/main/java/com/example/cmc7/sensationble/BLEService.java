package com.example.cmc7.sensationble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BLEService extends Service {
    //For diagnostic logcat:
    private final static String TAG = BLEService.class.getSimpleName();

    //For Bluetooth API Connectivity:
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    //IBinder for Service-Activity Communication:
    private final IBinder mBinder = new LocalBinder();

    //For remembering connection state:
    private int mConnectionState = STATE_DISCONNECTED;

    //For notification showing:
    NotificationCompat.Builder mBuilder;

    //For defining different connection state:
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    //For defining different broadcast message:
    public final static String ACTION_GATT_CONNECTED = "SensationBLE.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "SensationBLE.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "SensationBLE.ACTION_GATT_SERVICES_DISCOVERED";


    public BLEService() {
        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("My notification")
                .setContentText("Hello World!");
    }

    //This function will be called if it's not running and someone calls startService():
    @Override
    public void onCreate(){
        Log.e(TAG, "onCreate!");
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy!");
        super.onDestroy();
    }

    //Return the IBinder of BLEService if anyone binds this service:
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind!");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        Log.e(TAG, "onUnbind!");
        return super.onUnbind(intent);
    }

    //Broadcast - broadcast message to all other activities:
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }


    private class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }


}
