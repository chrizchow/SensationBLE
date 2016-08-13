package com.example.cmc7.sensationble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by cmc7 on 8/13/2016.
 */
class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    private final Context context;
    private LayoutInflater mInflator;

    // initializer:
    public LeDeviceListAdapter(Context context) {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
        this.context = context;
        mInflator = LayoutInflater.from(this.context);

    }


    public boolean addDevice(BluetoothDevice device){
        if(!mLeDevices.contains(device)){
            return mLeDevices.add(device);
        }else{
            return false;
        }
    }

    public void clear(){
        mLeDevices.clear();
    }

    public BluetoothDevice getDevice(int i){
        return mLeDevices.get(i);
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    // This function will run when it is connected to ListView and need to update screen
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View rowView = mInflator.inflate(R.layout.devicelist_layout, viewGroup, false);

        // Initialize and connect the TextView from View to Controller:
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);

        // Retrieve the device name and address from ArrayList:
        BluetoothDevice device = mLeDevices.get(i);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        // Show the content to the View:
        if (deviceName != null && deviceName.length() > 0)
            firstLine.setText(deviceName);
        else
            firstLine.setText(R.string.unknown_device);
        secondLine.setText(device.getAddress());

        // return the finished drawing row:
        return rowView;
    }
}
