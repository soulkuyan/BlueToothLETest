package com.example.ble.bluetoothletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.HashMap;

/**
 * Created by lei.zhang on 2017/5/11.
 * 工具类
 */

public class Utils {

    public final static String ACTION_GATT_CONNECTED =
            "com.visunex.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.visunex.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.visunex.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.visunex.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.visunex.bluetooth.le.EXTRA_DATA";

    private static HashMap<String, String> attributes = new HashMap();

    //检查设备是否支持BLE功能
    public static boolean isSupportBLE(final Context context){
        boolean isSupport = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        return  isSupport;
    }

    //根据UUID判断机器名
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    //退出BLE服务
    public static void close(BluetoothAdapter mBluetoothAdapter, BluetoothGatt mBluetoothGatt) {
        if (mBluetoothGatt == null) {
            return;
        }
        disconnect(mBluetoothAdapter, mBluetoothGatt);
        mBluetoothGatt.close();
    }

    //取消连接BLE设备
    public static void disconnect(BluetoothAdapter mBluetoothAdapter, BluetoothGatt mBluetoothGatt) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    //byte[ ] -->16进制字符串
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    //16进制字符串 -->byte[ ]
    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }
    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }
}
