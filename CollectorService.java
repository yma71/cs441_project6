package com.llu17.youngq.smartpark;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.llu17.youngq.smartpark.data.GpsDbHelper;

import java.lang.reflect.Method;
import java.util.Timer;


/**
 * Created by youngq on 17/2/15.
 */

public class CollectorService extends Service implements SensorEventListener {

    public static SQLiteDatabase mDb;
    private PowerManager.WakeLock wakeLock = null;

    private static int sampling_rate;
//    public static final String ACTION = "com.llu17.youngq.sqlite_gps.CollectorService";
//    private final static String tag = "UploadService";
    /*===GPS===*/
    private LocationManager locationManager;
    private double[] gps_location = new double[2]; //location 1.latitude 2.longitude
    private double[] bearAndSpeed = new double[2];    //1.bearing 2.speed   sqlite just has real type, so we use double here
    public static String id = ""; //phone id
    public static boolean[] mark = new boolean[]{false};    //used to mark bus stop

    public LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.e("===location===","changed!");


            bearAndSpeed[0] = location.getBearing();
            bearAndSpeed[1] = location.getSpeed();

            gps_location[0] = location.getLatitude();
            gps_location[1] = location.getLongitude();

//            BigDecimal bg = new BigDecimal(location.getLatitude());
//            BigDecimal bg1 = new BigDecimal(location.getLongitude());
//            gps_location[0] = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//            gps_location[1] = bg1.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
//            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivity(intent);
        }
    };
    /*===Sensor:accelerometer & gyroscope & magnetometer===*/
//    private static int RATE = 1000;  //100 -> 10 samples/s 50 -> 20 samples/s 20 -> 50 samples/s
    private SensorManager sensorManager;
    private Sensor sensor;
//    private static final float NS2S = 1.0f / 1000000000.0f;
//    private long timestamp;
    private int label = 0;
    private double[] stepcount = new double[2]; //stepcount 1.last one 2.real step
    private double[] acce = new double[3];    //accelerator
//    private double[] angle = new double[3];
    private double[] gyro = new double[3];  //gyroscope
    //07.25 add
    private double[] d_magnetic = new double[3];    //magnetic
    Timer timer, timer1;
    /*===Motion State===*/
    public static int[] state = new int[]{6};
    /*===Battery Consumption===*/
    private int batteryLevel = 0;
    private int batteryScale = 0;
    private int[] batteryPercentage = new int[1];
    private BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryLevel=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            batteryScale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            batteryPercentage[0] = batteryLevel*100/batteryScale;
            //显示电量
            Log.e("battery scale: ", ""+batteryScale);
            Log.e("battery level: ", ""+batteryLevel);
            Log.e("battery percentage: ", ""+batteryPercentage[0]+"%");
        }
    };
    /*===WiFi State===*/
//    NetworkInfo wifiCheck;
    private int[] wifistate = new int[1];
    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            NetworkInfo currentNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            if(currentNetworkInfo.isConnected()){
                wifistate[0] = 1;
                Log.e("WiFi is Connected","!!!!!"+wifistate[0]);
            }else{
                wifistate[0] = 0;
                Log.e("WiFi is not Connected","!!!!!"+wifistate[0]);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        acquireWakeLock();
        GpsDbHelper dbHelper = new GpsDbHelper(this);
        mDb = dbHelper.getWritableDatabase();
        label = 0;
        id = getSerialNumber();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        sampling_rate = Integer.valueOf(preferences.getString(getResources().getString(R.string.sr_key_all),"1000"));
        Log.e("-----ALL SR2-----","CS : "+sampling_rate);

        /*===WiFi State===*/
//        ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        wifiCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//
//        if (wifiCheck.isConnected()) {
//            // Do whatever here
//            Log.e("WiFi is Connected","!!!!!!!!!!!!!!!!");
//        } else {
//            Log.e("WiFi is not Connected","!!!!!!!!!!!!!!!!");
//        }
        registerReceiver(this.mConnReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        /*===Battery Consumption===*/
        IntentFilter intentFilter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(broadcastReceiver, intentFilter);

        /*===GPS===*/
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
        catch(SecurityException e){
            e.getStackTrace();
        }
        Log.e("===GPS===","===begin===");
//        Toast.makeText(this, "Starting the GPS!!!!!", Toast.LENGTH_SHORT).show();

//        LocationProvider info = locationManager.getProvider(LocationManager.GPS_PROVIDER);
//        if(info.supportsBearing())
//            Log.e("=========","support bearing");
//        else
//            Log.e("=========","not support bearing");
//        if(info.supportsSpeed())
//            Log.e("=========","support speed");
//        else
//            Log.e("=========","not support speed");
        /*===Sensor===*/
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //07.25 add
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.e("===Sensor===","===begin===");
        /*---step count---*/
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
//        Toast.makeText(this, "Sensor Service Started", Toast.LENGTH_SHORT).show();
        timer = new Timer();
        timer.schedule(new Upload(acce, gyro, stepcount, gps_location, state, d_magnetic, bearAndSpeed, mark), 0, sampling_rate);
        timer1 = new Timer();
        timer1.schedule(new Upload1(batteryPercentage, wifistate), 0, 60000);   //change to 60000
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(mConnReceiver);
        try {
            locationManager.removeUpdates(locationListener);
        }
        catch(SecurityException e){
            e.getStackTrace();
        }
        Log.e("===GPS===","===stop===");
        timer.cancel();
        timer1.cancel();
        sensorManager.unregisterListener(this);
        Log.e("===Sensor===","===stop===");
        try {       //make sure Upload class finish using mDb
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mDb.close();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*---step count---*/
        if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            if(label == 0) {
                stepcount[0] = event.values[0];
                label++;
            }
            stepcount[1] = event.values[0] - stepcount[0];
            Log.e("stepcount[0]:",""+stepcount[0]);
            Log.e("stepcount[1]:",""+stepcount[1]);
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acce[0] = event.values[0];
            acce[1] = event.values[1];
            acce[2] = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            if (timestamp != 0) {
//                // 得到两次检测到手机旋转的时间差（纳秒），并将其转化为秒
//                final float dT = (event.timestamp - timestamp) * NS2S;
//                // 将手机在各个轴上的旋转角度相加，即可得到当前位置相对于初始位置的旋转弧度
//                angle[0] += event.values[0] * dT;
//                angle[1] += event.values[1] * dT;
//                angle[2] += event.values[2] * dT;
//                // 将弧度转化为角度
//                gyro[0] = (float) Math.toDegrees(angle[0]);
//                gyro[1] = (float) Math.toDegrees(angle[1]);
//                gyro[2] = (float) Math.toDegrees(angle[2]);
//            }
//            timestamp = event.timestamp;
            gyro[0] = event.values[0];
            gyro[1] = event.values[1];
            gyro[2] = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            d_magnetic[0] = event.values[0];
            d_magnetic[1] = event.values[1];
            d_magnetic[2] = event.values[2];
        }
//        Log.d("hi","debug");
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("onAccuracy®Changed",""+accuracy);
    }

    private static String getSerialNumber(){

        String serial = null;

        try {

            Class<?> c =Class.forName("android.os.SystemProperties");

            Method get =c.getMethod("get", String.class);

            serial = (String)get.invoke(c, "ro.serialno");

        } catch (Exception e) {

            e.printStackTrace();

        }

        return serial;

    }

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    private void acquireWakeLock()
    {
        if (null == wakeLock)
        {
            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock)
            {
                wakeLock.acquire();
            }
        }
    }
    //释放设备电源锁
    private void releaseWakeLock()
    {
        if (null != wakeLock)
        {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
