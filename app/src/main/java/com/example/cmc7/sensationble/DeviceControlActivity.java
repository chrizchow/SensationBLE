package com.example.cmc7.sensationble;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
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
import android.location.Location;
import android.content.ServiceConnection;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


public class DeviceControlActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener{
    TextView heartrate_text; //debug code
    TextView step_text;
    TextView button;
    TextView calories_text;
    TextView stepGoal_text;
    static Dialog d;
    ProgressBar step_pro;
    private String urlText;
    //List<BluetoothGattCharacteristic> chars = new ArrayList<BluetoothGattCharacteristic>();

    //Location Services Google API:
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

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
                if(deviceName!=null) {
                    getSupportActionBar().setTitle(deviceName);
                }

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
        calories_text = (TextView) findViewById(R.id.calories);
        step_pro = (ProgressBar) findViewById(R.id.progressRed);
        stepGoal_text = (TextView) findViewById(R.id.stepGoal_text);
        heartrate_text.setText("000");
        step_text.setText("0");
        button.setText(" ");
        calories_text.setText("0 calories");
        step_pro.setMax(12500);

        //TODO: Under Construction

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                button.setText("refreshed");

                BluetoothGattCharacteristic heartRate = mBLEService.getSupportedGattServices().get(3).getCharacteristics().get(0);
                mBLEService.setCharacteristicNotification(heartRate);

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

    protected void onStart() {
        if(mGoogleApiClient!=null) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    protected void onStop() {
        if(mGoogleApiClient!=null){
            mGoogleApiClient.disconnect();
        }
        super.onStop();
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
                Log.i(TAG,"GATT has been disconnected!");


                //finish();
                Toast.makeText(DeviceControlActivity.this, "BLE Device Disconnected", Toast.LENGTH_SHORT).show();
            }else if(BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                updateTime();

            }else if(BLEService.ACTION_DATA_AVAILABLE.equals(action)){
                //Take the available data:

                String uuid = intent.getStringExtra(BLEService.EXTRA_DATA_UUID);
                byte[] bytes = intent.getByteArrayExtra(BLEService.EXTRA_DATA_BYTES);

                //Show it to screen:
                String step_id = "0000fff6-0000-1000-8000-00805f9b34fb";
                String heartrate_id = "00002a37-0000-1000-8000-00805f9b34fb";

                if(uuid.equals(step_id)){
                    //step_text.append("\t\tAction_Data_Avalable\n");     //debug //D2 04 00 00
                    String str = new String(bytes);
                    Integer step = bytes[0] & 0xFF |
                            (bytes[1] & 0xFF) << 8 |
                            (bytes[2] & 0xFF) << 16 |
                            (bytes[3] & 0xFF) << 24;

                    if(step.toString().length() > 4){
                        step_text.setTextSize(step_text.getTextSize() - 10);
                    }
                    step_text.setText(step.toString());

                    step_pro.setProgress(step);

                    if(step >= step_pro.getMax())
                        stepGoal_text.setText("Step Goal Achieved!");
                    else
                        stepGoal_text.setText("");

                    changeCalories(step);

                    updateTime();

                }
                else if(uuid.equals(heartrate_id)){

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
                else{
                    fallDialog();
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

    //This function runs when Android needs to create menu bar,
    //it should be called automatically during init of the activity:
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_devicecontrol, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.action_about:
                aboutDialog();
                getLocation();
                fallDialog();
                break;
            case R.id.action_change:
                changeDialog();
                break;
            case R.id.steps_change:
                changeSteps();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateTime(){
        BluetoothGattCharacteristic chara = mBLEService.getSupportedGattServices().get(2).getCharacteristics().get(6);
        Long tsLong = System.currentTimeMillis() / 1000;
        tsLong = tsLong - 946656000;
        int tsInt = tsLong.intValue();
        String ts = tsLong.toString();
        button.setText(ts);
        chara.setValue(tsInt, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        mBLEService.writeCharacteristic(chara);
    }

    public void changeCalories(int step){
        double weight = DeviceScanActivity.getInstance().weight;
        double temp = 2.20462262 * weight * 0.57;
        temp /= 2200;
        temp *= step;
        DeviceScanActivity.getInstance().calories = (int)temp;

        calories_text.setText(Integer.toString((int)temp) + " Calories");
    }

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

    private void changeDialog(){
        final Dialog d = new Dialog(this);
        d.setTitle("Change Information");
        d.setContentView(R.layout.number_picker);
        final EditText editText = (EditText) d.findViewById(R.id.editText);
        Button b1 = (Button) d.findViewById(R.id.button1);
        Button b2 = (Button) d.findViewById(R.id.button2);
        final NumberPicker np = (NumberPicker) d.findViewById(R.id.numberPicker);

        editText.setText(DeviceScanActivity.getInstance().username);

        np.setMaxValue(100);
        np.setMinValue(0);
        np.setValue(DeviceScanActivity.getInstance().weight);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                np.setTag(i1);
            }
        });

        b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                DeviceScanActivity.getInstance().weight = np.getValue();
                DeviceScanActivity.getInstance().username = editText.getText().toString();
                d.dismiss();
            }
        });
        b2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss(); // dismiss the dialog
            }
        });
        d.show();

    }

    private void fallDialog(){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((urlText)));
        startActivity(browserIntent);
        Vibrator vibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
    }

    private void changeSteps(){
        final Dialog d = new Dialog(this);
        d.setTitle("Change Steps Goal");
        d.setContentView(R.layout.number_picker_steps);
        Button b1 = (Button) d.findViewById(R.id.button1);
        Button b2 = (Button) d.findViewById(R.id.button2);
        Button b3 = (Button) d.findViewById(R.id.button3);
        final NumberPicker np = (NumberPicker) d.findViewById(R.id.numberPicker_steps);

        np.setMaxValue(50000);
        np.setMinValue(0);
        np.setValue(DeviceScanActivity.getInstance().steps_goal);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                numberPicker.setValue((i1 < i)?i-100:i+100);
            }
        });

        b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                DeviceScanActivity.getInstance().steps_goal = np.getValue();
                step_pro.setMax(np.getValue());
                d.dismiss();
            }
        });
        b2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss(); // dismiss the dialog
            }
        });
        b3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                np.setValue(12500);
            }
        });
        d.show();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        //getLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private void getLocation(){
        try{
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }catch(SecurityException e){
            e.printStackTrace();
        }

        if (mLastLocation != null) {
            // fetch and convert the last lat and long
            String latitude = String.valueOf(mLastLocation.getLatitude());
            String longitude = String.valueOf(mLastLocation.getLongitude());
            // show to user the current lat and long
            button.setText(latitude + " ," + longitude);

            urlText = getResources().getString(R.string.json_url);
            urlText = urlText.concat(DeviceScanActivity.getInstance().username);
            urlText = urlText.concat("&lat="+latitude);
            urlText = urlText.concat("&lng="+longitude);

        }else{
            Toast.makeText(this, "No Location Detected", Toast.LENGTH_LONG).show();
        }
    }

}
