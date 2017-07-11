package com.example.ble.bluetoothletest;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lei.zhang on 2017/5/10.
 * 展示要连接BLE设备信息
 */
public class BluetoothInfoActivity extends Activity {
    private final static String TAG = BluetoothInfoActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private TextView addressTv;
    private TextView stateTv;
    private TextView dataTv;
    private ExpandableListView mGattListView;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    BluetoothGattCharacteristic characteristic;

//    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100){
                mBluetoothLeService.readDataFromCharacteristic(characteristic);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        addressTv = (TextView)findViewById(R.id.device_address);
        stateTv = (TextView)findViewById(R.id.connection_state);
        dataTv = (TextView)findViewById(R.id.data_value);
        mGattListView = (ExpandableListView)findViewById(R.id.gatt_services_list);
        mGattListView.setOnChildClickListener(servicesListClickListner);

        addressTv.setText(mDeviceAddress);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    //显示BLE设备的Service UUID与characterist UUID
    private void displayGattServices(List<BluetoothGattService> gattServices){
        if (gattServices == null){
            return;
        }

        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices){
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, Utils.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, Utils.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattListView.setAdapter(gattServiceAdapter);
    }

    //点击UUID，进行连接
    private ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    dataTv.setText(R.string.no_data);
                    if (mGattCharacteristics != null) {
                        characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        Log.w(TAG, "click success: characteristic" + characteristic.getUuid());

                        //修改放到主线程去调用
//                        mBluetoothLeService.readDataFromCharacteristic(characteristic);
                        mHandler.sendEmptyMessage(100);

//                        mBluetoothLeService.readDescriotor(characteristic);
                        return true;
                    }
                    return false;
                }
            };

    //绑定service并在service中进行操作
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()){
                Log.e(TAG, "Unable to initialize Bluetooth");
                Toast.makeText(getApplicationContext(), R.string.Unable_initialize_Bluetooth, Toast.LENGTH_SHORT).show();
            }else {
                mBluetoothLeService.connect(mDeviceAddress);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    //监听service发过来的广播，并根据广播进行下一步的具体操作
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Utils.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                stateTv.setText(R.string.connected);
            } else if (Utils.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                stateTv.setText(R.string.disconnected);
                dataTv.setText(R.string.no_data);
                mBluetoothLeService.connect(mDeviceAddress);
            } else if (Utils.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (Utils.ACTION_DATA_AVAILABLE.equals(action)) {
                dataTv.setText(intent.getStringExtra(Utils.EXTRA_DATA).replaceAll(" ", ""));
//                mBluetoothLeService.readDataFromCharacteristic(characteristic);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Utils.ACTION_GATT_CONNECTED);
        intentFilter.addAction(Utils.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(Utils.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(Utils.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
