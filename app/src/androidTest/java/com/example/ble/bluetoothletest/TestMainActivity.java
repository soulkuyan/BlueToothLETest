package com.example.ble.bluetoothletest;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Created by lei.zhang on 2017/5/26.
 */

public class TestMainActivity extends ActivityInstrumentationTestCase2<MainActivity> {
    private Context context;

    public TestMainActivity() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = getActivity().getApplicationContext();
    }

    public void testStart(){
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
