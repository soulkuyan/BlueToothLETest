package com.example.ble.bluetoothletest;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by lei.zhang on 2017/5/15.
 */

public class  LeDeviceListAdapter extends BaseAdapter{

    private ArrayList<BluetoothDevice> mBleDevicesList;
    private LayoutInflater mInflater;

    public LeDeviceListAdapter(Context context) {
        super();
        mBleDevicesList = new ArrayList<>();
        mInflater = LayoutInflater.from(context);
    }

    public void addDevice(BluetoothDevice device) {
        if(!mBleDevicesList.contains(device)) {
            mBleDevicesList.add(device);
        }
    }

    public BluetoothDevice getDevice(int position){
        return mBleDevicesList.get(position);
    }

    public void clear(){
        mBleDevicesList.clear();
    }

    @Override
    public int getCount() {
        return mBleDevicesList.size();
    }

    @Override
    public Object getItem(int position) {
        return mBleDevicesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null){
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.listitem_device, null);
            holder.deviceName = (TextView)view.findViewById(R.id.device_name);
            holder.deviceAddress = (TextView)view.findViewById(R.id.device_address);
            view.setTag(holder);
        }else {
            holder = (ViewHolder) view.getTag();
        }

        //为什么存在蓝牙LBE名字为空的情况
        BluetoothDevice device = mBleDevicesList.get(position);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0){
            holder.deviceName.setText(deviceName);
        }else {
            holder.deviceName.setText(R.string.unknown_device);
        }
        holder.deviceAddress.setText(device.getAddress());
        return view;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
