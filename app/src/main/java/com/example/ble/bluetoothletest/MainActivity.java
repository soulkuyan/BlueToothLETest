package com.example.ble.bluetoothletest;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by lei.zhang on 2017/5/10.
 * 扫描附近的BLE设备，并展示出设备名以及对应的MAC
 */
public class MainActivity extends ListActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private LeDeviceListAdapter mLeListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private TextView scanTv;
    Handler mHandler;
    private boolean mScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHandler = new Handler();
        scanTv = (TextView)findViewById(R.id.scan_textView);
        TextView emptyTv = new TextView(this);
        ((ViewGroup)getListView().getParent()).addView(emptyTv);

        if (!Utils.isSupportBLE(this)){
            Toast.makeText(getApplicationContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            emptyTv.setText(this.getResources().getString(R.string.ble_not_supported));
        }

        //初始化蓝牙适配器
        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        if (mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            emptyTv.setText(this.getResources().getString(R.string.error_bluetooth_not_supported));
            return;
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothAdapter.enable();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter != null){
            mLeListAdapter = new LeDeviceListAdapter(this);
            setListAdapter(mLeListAdapter);
            if (mBluetoothAdapter.isEnabled()){
                scanLeDevice(true);
//                scanTv.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Log.i(TAG, "onresume()2222");
//                        mLeListAdapter.clear();
//                        scanLeDevice(true);
//                    }
//                });
            }else {
                mHandler.postDelayed(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                    @Override
                    public void run() {
                        if (mBluetoothAdapter.isEnabled()){
                            mHandler.removeCallbacks(this);
                            scanLeDevice(true);

                        }else {
                            mHandler.postDelayed(this, 1000);
                        }
                    }
                }, 0);
            }
        }else {
            return;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeListAdapter.clear();
    }


    //搜索BLE设备信息
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable){
        if (enable){
           mHandler.postDelayed(new Runnable() {
               @Override
               public void run() {
                   mScanning = false;
                   mBluetoothAdapter.stopLeScan(mLeScanCallback);

               }
           }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeListAdapter.addDevice(device);
                    mLeListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeListAdapter.getDevice(position);
        if (device == null){
            return;
        }
        final Intent intent = new Intent(this, BluetoothInfoActivity.class);
        intent.putExtra(BluetoothInfoActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(BluetoothInfoActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

}
