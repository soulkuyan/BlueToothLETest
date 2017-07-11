package com.example.ble.bluetoothletest;

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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

//    ArrayList<BluetoothGattCharacteristic> requestList; //by 1825 for 请求排序

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 11){
                Utils.close(mBluetoothAdapter, mBluetoothGatt);
                mBluetoothGatt = null;
            }
        }
    };

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHandler.sendEmptyMessage(11);
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // 以前连过，重新连接
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        //当连接状态发生改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (status == BluetoothGatt.GATT_SUCCESS){
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    intentAction = Utils.ACTION_GATT_CONNECTED;
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(intentAction);
                    Log.i(TAG, "================success");
                    Log.i(TAG, "Connected to GATT server.");
//                Log.i(TAG, "Attempting to start service discovery:" +
//                        mBluetoothGatt.discoverServices());
                    // 连接成功后，先启动服务发现
                    mBluetoothGatt.discoverServices();
                }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    intentAction = Utils.ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    mHandler.sendEmptyMessage(11);
                    broadcastUpdate(intentAction);
                }
            }else {
                intentAction = Utils.ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                mHandler.sendEmptyMessage(11);
                broadcastUpdate(intentAction);

            }
        }

        // 发现新服务端
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.w(TAG, "===========onServicesDiscovered: " + status);
                showLog();
//                readDataFromCharacteristic(); //此处废弃，在activity里进行读取
                broadcastUpdate(Utils.ACTION_GATT_SERVICES_DISCOVERED);
            }else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
//                mBluetoothAdapter.disable();
//                mBluetoothAdapter.enable();
            }
        }

        // 读写特性
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic character, int status) {
            super.onCharacteristicRead(gatt, character, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "===========onCharacteristicRead: " + "status:" + String.valueOf(status == BluetoothGatt.GATT_SUCCESS) + " :" + status);
                byte[] data = character.getValue();
                Log.w(TAG, "read success: " + Arrays.toString(data));
                broadcastUpdate(Utils.ACTION_DATA_AVAILABLE, character);
            }else {
                Log.w(TAG, "read fail: " + "status:" + String.valueOf(status == BluetoothGatt.GATT_SUCCESS) + " :" + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.w(TAG, "===========onCharacteristicWrite: " + "status:" + String.valueOf(status == BluetoothGatt.GATT_SUCCESS) + " :" + status);
                Log.e(TAG, "write success: " + characteristic.getValue());

            }else {
                Log.w(TAG, "write fail: " + "status:" + String.valueOf(status == BluetoothGatt.GATT_SUCCESS) + " :" + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "===========onCharacteristicChanged: " + characteristic.getValue());
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.w(TAG, "===========onDescriptorRead: " + "status:" + String.valueOf(status == BluetoothGatt.GATT_SUCCESS) + " :" + status);
            Log.e(TAG, "read success DescriptorRead: " + descriptor.getValue());
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        byte[] data = characteristic.getValue();
        intent.putExtra(Utils.EXTRA_DATA, Utils.bytesToHexString(data));
        sendBroadcast(intent);
    }

    public void showLog() {
        if (mBluetoothGatt == null) {
            return;
        }

        List<BluetoothGattService> supportedGattServices = mBluetoothGatt.getServices();
        for (int i = 0; i < supportedGattServices.size(); i++){
            Log.e(TAG,"1:BluetoothGattService UUID=:"+supportedGattServices.get(i).getUuid());
            List<BluetoothGattCharacteristic> listGattCharacteristic = supportedGattServices.get(i).getCharacteristics();

            Log.e(TAG,"2:BluetoothGattCharacteristic UUID=:" + listGattCharacteristic.size());

            for(int j=0; j<listGattCharacteristic.size(); j++){
                getDescriptorFromCharacteristic(listGattCharacteristic.get(j));
                int charaProp = listGattCharacteristic.get(j).getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PERMISSION_READ) > 0){
                    Log.e(TAG,"2:BluetoothGattCharacteristic UUID=:"+listGattCharacteristic.get(j).getUuid() + "  can read");
                    Log.e(TAG,"2:BluetoothGattCharacteristic UUID=:"+listGattCharacteristic.get(j).getValue());
                    getDescriptorFromCharacteristic(listGattCharacteristic.get(j));
                }
                if ((charaProp | BluetoothGattCharacteristic.PERMISSION_WRITE) > 0){
                    Log.e(TAG,"2:BluetoothGattCharacteristic UUID=:"+listGattCharacteristic.get(j).getUuid() + "  can write");
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0){
                    Log.e(TAG,"2:BluetoothGattCharacteristic UUID=:"+listGattCharacteristic.get(j).getUuid() + "  can notication");
                    mBluetoothGatt.setCharacteristicNotification(listGattCharacteristic.get(j), true);
                }
            }
        }
    }

    public void getDescriptorFromCharacteristic(BluetoothGattCharacteristic characteristic){
        List<BluetoothGattDescriptor> gattDescriptors = characteristic.getDescriptors();
        for (BluetoothGattDescriptor descriptor : gattDescriptors){
            Log.e(TAG, "-------->desc uuid:" + descriptor.getUuid());
            int descPermission = descriptor.getPermissions();
//            Log.e(TAG,"-------->desc permission:"+ Utils.getDescPermission(descPermission));

            byte[] desData = descriptor.getValue();
            if (desData != null && desData.length > 0) {
                Log.e(TAG, "-------->desc value:"+ new String(desData));
            }
        }
    }

//    public void readDescriotor(BluetoothGattCharacteristic characteristic){
//        Log.i(TAG, "click start read Descriotor");
//        String UUID = "00002901-0000-1000-8000-00805f9b34fb";
//        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(java.util.UUID.fromString(UUID));
//        mBluetoothGatt.readDescriptor(descriptor);
//    }


    //根据characteristic UUID读取
    public void readDataFromCharacteristic(BluetoothGattCharacteristic characteristic){
        if (mBluetoothAdapter == null || mBluetoothGatt  == null){
            Log.w(TAG, "BluetoothAdapter not initialized" + mBluetoothAdapter + mBluetoothGatt);

        }else {
            mBluetoothGatt.readCharacteristic(characteristic);
//            mBluetoothGatt.readDescriptor(characteristic.getDescriptor(characteristic.getUuid()));
            setCharacteristicNotification(characteristic, true);
            Log.i(TAG, "click and start read");
        }
    }

    //根据characteristic UUID斜土
    public void writeDataForCharacteristic(BluetoothGattCharacteristic characteristic){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    //根据characteristic UUID监听特性变化
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }

//    //by 1825 for 请求排序
//    private void addCharacteristicToRead(BluetoothGattCharacteristic characteristic){
//        if (requestList == null){
//            requestList = new ArrayList<>(); //by 1825 for 请求排序
//        }
//        requestList.add(characteristic);
//        if (requestList.size() == 1){
//            readDataFromCharacteristic(requestList[0]);
//        }
//
//    }

}
