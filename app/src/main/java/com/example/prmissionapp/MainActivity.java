package com.example.prmissionapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends Activity  {

    private static final int CAMERA_PERMISSION_CODE = 102;
    private CameraManager mCameraManager;
    private String mCameraId;
    Button login_bt;
    TextView login_tv, app_tv;
    float x,y,z;
    boolean flashAvailable = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //button and text views
        login_bt = findViewById(R.id.button);
        login_tv = findViewById(R.id.login_tv);
        app_tv = findViewById(R.id.app_tv);

        //this block gives back the current battery %
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        final float batteryPct = (level * 100) / (float)scale;


        // this part register a listener for magnetic field
        SensorManager sMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor magnetField = sMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        SensorEventListener magnetListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                Log.d("direction","X:" + x + " Y:" + y + " Z:" + z);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sMan.registerListener(magnetListener, magnetField, SensorManager.SENSOR_DELAY_NORMAL);

        // get camera details to enable flashlight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mCameraId = mCameraManager.getCameraIdList()[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mCameraManager.setTorchMode(mCameraId,true);
                flashAvailable = mCameraManager
                        .getCameraCharacteristics("0")
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        login_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] { Manifest.permission.RECORD_AUDIO },
                        CAMERA_PERMISSION_CODE);
                if(ContextCompat.checkSelfPermission(MainActivity.super.getApplicationContext(),
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(getApplicationContext(), "Flash Permission is not Granted!",Toast.LENGTH_SHORT).show();
                }
                else{
                    checkIfAllPermissionsGranted(batteryPct, x, flashAvailable);
                }
            }
        });

    }

    public void checkIfAllPermissionsGranted(float batteryLevel, float x, boolean flashAvailable) {
        //Note: this check isn't really for true south but I guess for this project is good enough
        boolean isPhonePointingSouth = (x > -18) ? true : false;
        boolean isPhoneBatteryOver30Percent = (batteryLevel > 30) ? true : false;

        if (isPhoneBatteryOver30Percent && flashAvailable && isPhonePointingSouth){
            Toast.makeText(getApplicationContext(), "Login Successful!",Toast.LENGTH_SHORT).show();
            login_tv.setVisibility(View.INVISIBLE);
            app_tv.setText("Battery level: " + batteryLevel + "\n"
                    + "phone direction: South" + "\n"
                    + "Flash available : true");
            app_tv.setTextColor(Color.GREEN);
        }
        else {
            app_tv.setText("Make sure your phone is pointed to the South!");
            app_tv.setTextColor(Color.RED);
        }
    }
}