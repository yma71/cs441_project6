package com.llu17.youngq.smartpark;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.llu17.youngq.smartpark.data.GpsContract;
import com.llu17.youngq.smartpark.data.GpsDbHelper;
import com.llu17.youngq.smartpark.table.ACCELEROMETER;
import com.llu17.youngq.smartpark.table.BATTERY;
import com.llu17.youngq.smartpark.table.GPS;
import com.llu17.youngq.smartpark.table.GYROSCOPE;
import com.llu17.youngq.smartpark.table.MAGNETOMETER;
import com.llu17.youngq.smartpark.table.MOTIONSTATE;
import com.llu17.youngq.smartpark.table.PARKINGSTATE;
import com.llu17.youngq.smartpark.table.STEP;
import com.llu17.youngq.smartpark.table.WIFI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static com.llu17.youngq.smartpark.MainActivity.upload_state;

public class UploadServiceM extends Service{

    private PowerManager.WakeLock wakeLock = null;

    private GpsDbHelper dbHelper;
    private SQLiteDatabase db,db1;


    private String gps_url = "http://cs.binghamton.edu/~smartpark/user/gps.php";
    private String acce_url = "http://cs.binghamton.edu/~smartpark/user/accelerometer.php";
    private String gyro_url = "http://cs.binghamton.edu/~smartpark/user/gyroscope.php";
    private String step_url = "http://cs.binghamton.edu/~smartpark/user/step.php";
    private String motion_url = "http://cs.binghamton.edu/~smartpark/user/motionstate.php";
    private String wifi_url = "http://cs.binghamton.edu/~smartpark/user/wifi.php";
    private String battery_url = "http://cs.binghamton.edu/~smartpark/user/battery.php";
    private String magne_url = "http://cs.binghamton.edu/~smartpark/user/magnetometer.php";
    private String park_url = "http://cs.binghamton.edu/~smartpark/user/parkingstate.php";

    private ArrayList<GPS> gpses;
    private ArrayList<ACCELEROMETER> acces;
    private ArrayList<GYROSCOPE> gyros;
    private ArrayList<MOTIONSTATE> motions;
    private ArrayList<STEP> steps;
    private ArrayList<BATTERY> batteries;
    private ArrayList<WIFI> wifis;
    private ArrayList<MAGNETOMETER> magnetometers;
    private ArrayList<PARKINGSTATE> parkinglots;

    private JSONObject acce_object,gyro_object,gps_object,motion_object,step_object,battery_object,wifi_object,magne_object;
    private JSONArray AcceJsonArray,GyroJsonArray,GpsJsonArray,MotionJsonArray,StepJsonArray,BatteryJsonArray,WiFiJsonArray,MagneJsonArray;
    private JSONObject park_object;
    private JSONArray ParkJsonArray;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        acquireWakeLock();
        registerReceiver(this.mConnReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        if(wifistate[0] == 1)
            upload_state.setVisibility(android.view.View.GONE);
        if(wifistate[0] == 0) {
            unregisterReceiver(mConnReceiver);
            Log.e("on destroy","unregister");
        }
        Log.e("service","destroy");
    }

    private ArrayList<GPS> find_all_gps(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, latitude, longitude, bearing, speed , flag from gps_location where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            Log.e("cursor count gps: ", "" + c.getCount());
            if (c != null && c.getCount() > 0) {
                ArrayList<GPS> gpslist = new ArrayList<>();
                GPS gps;
                while (c.moveToNext()) {
                    gps = new GPS();
                    gps.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_ID)));
                    gps.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_TIMESTAMP)));
                    gps.setLatitude(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_LATITUDE)));
                    gps.setLongitude(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_LONGITUDE)));
                    gps.setBearing(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_BEARING)));
                    gps.setSpeed(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_SPEED)));
                    gps.setFlag(c.getInt(c.getColumnIndexOrThrow(GpsContract.GpsEntry.COLUMN_FLAG)));
                    gpslist.add(gps);
                }
                return gpslist;
            } else {
                Log.e("i am here", "hello11111111");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
            Log.e("i am here2", "hello11111111");
        }
        Log.e("i am here3", "hello11111111");
        return null;
    }
    private ArrayList<ACCELEROMETER> find_all_acce(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, X, Y, Z from accelerometer where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            Log.e("cursor count acce: ", "" + c.getCount());
            if (c != null && c.getCount() > 0) {
                ArrayList<ACCELEROMETER> accelist = new ArrayList<>();
                ACCELEROMETER acce;
                while (c.moveToNext()) {
                    acce = new ACCELEROMETER();
                    acce.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.AccelerometerEntry.COLUMN_ID)));
                    acce.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.AccelerometerEntry.COLUMN_TIMESTAMP)));
                    acce.setX(c.getDouble(c.getColumnIndexOrThrow(GpsContract.AccelerometerEntry.COLUMN_X)));
                    acce.setY(c.getDouble(c.getColumnIndexOrThrow(GpsContract.AccelerometerEntry.COLUMN_Y)));
                    acce.setZ(c.getDouble(c.getColumnIndexOrThrow(GpsContract.AccelerometerEntry.COLUMN_Z)));
                    accelist.add(acce);
                }
                return accelist;
            } else {
                Log.e("i am here", "hello2222222222");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally{
            c.close();
            db.close();
            Log.e("i am here2", "hello2222222222");
        }
        Log.e("i am here3", "hello2222222222");
        return null;
    }
    private ArrayList<GYROSCOPE> find_all_gyro(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, X, Y, Z from gyroscope where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            if (c != null && c.getCount() > 0) {
                ArrayList<GYROSCOPE> gyrolist = new ArrayList<>();
                GYROSCOPE gyro;
                while (c.moveToNext()) {
                    gyro = new GYROSCOPE();
                    gyro.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.GyroscopeEntry.COLUMN_ID)));
                    gyro.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.GyroscopeEntry.COLUMN_TIMESTAMP)));
                    gyro.setX(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GyroscopeEntry.COLUMN_X)));
                    gyro.setY(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GyroscopeEntry.COLUMN_Y)));
                    gyro.setZ(c.getDouble(c.getColumnIndexOrThrow(GpsContract.GyroscopeEntry.COLUMN_Z)));
                    gyrolist.add(gyro);
                }
                return gyrolist;
            } else {
                Log.e("i am here", "hello333333333");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
        }
        return null;
    }
    private ArrayList<MOTIONSTATE> find_all_motion(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, state from motionstate where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            if (c != null && c.getCount() > 0) {
                ArrayList<MOTIONSTATE> motionlist = new ArrayList<>();
                MOTIONSTATE motion;
                while (c.moveToNext()) {
                    motion = new MOTIONSTATE();
                    motion.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.MotionStateEntry.COLUMN_ID)));
                    motion.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.MotionStateEntry.COLUMN_TIMESTAMP)));
                    motion.setState(c.getInt(c.getColumnIndexOrThrow(GpsContract.MotionStateEntry.COLUMN_STATE)));
                    motionlist.add(motion);
                }
                return motionlist;
            } else {
                Log.e("i am here", "hello44444444");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
        }
        return null;
    }
    private ArrayList<STEP> find_all_step(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, Count from step where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            if (c != null && c.getCount() > 0) {
                ArrayList<STEP> steplist = new ArrayList<>();
                STEP step;
                while (c.moveToNext()) {
                    step = new STEP();
                    step.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.StepEntry.COLUMN_ID)));
                    step.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.StepEntry.COLUMN_TIMESTAMP)));
                    step.setCount(c.getInt(c.getColumnIndexOrThrow(GpsContract.StepEntry.COLUMN_COUNT)));
                    steplist.add(step);
                }
                return steplist;
            } else {
                Log.e("i am here", "hello55555555");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally{
            c.close();
            db.close();
        }
        return null;
    }
    private ArrayList<BATTERY> find_all_battery(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, Percentage from battery where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            if (c != null && c.getCount() > 0) {
                ArrayList<BATTERY> batterylist = new ArrayList<>();
                BATTERY battery;
                while (c.moveToNext()) {
                    battery = new BATTERY();
                    battery.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.BatteryEntry.COLUMN_ID)));
                    battery.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.BatteryEntry.COLUMN_TIMESTAMP)));
                    battery.setPercentage(c.getInt(c.getColumnIndexOrThrow(GpsContract.BatteryEntry.COLUMN_Percentage)));
                    batterylist.add(battery);
                }
                return batterylist;
            } else {
                Log.e("i am here", "hello66666666");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
        }
        return null;
    }
    private ArrayList<WIFI> find_all_wifi(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, State from wifi where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            if (c != null && c.getCount() > 0) {
                ArrayList<WIFI> wifilist = new ArrayList<>();
                WIFI wifi;
                while (c.moveToNext()) {
                    wifi = new WIFI();
                    wifi.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.WiFiEntry.COLUMN_ID)));
                    wifi.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.WiFiEntry.COLUMN_TIMESTAMP)));
                    wifi.setState(c.getInt(c.getColumnIndexOrThrow(GpsContract.WiFiEntry.COLUMN_State)));
                    wifilist.add(wifi);
                }
                return wifilist;
            } else {
                Log.e("i am here", "hello77777777");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
        }
        return null;
    }
    private ArrayList<MAGNETOMETER> find_all_magne(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, X, Y, Z from magnetometer where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            Log.e("cursor count magne: ", "" + c.getCount());
            if (c != null && c.getCount() > 0) {
                ArrayList<MAGNETOMETER> magnelist = new ArrayList<>();
                MAGNETOMETER magne;
                while (c.moveToNext()) {
                    magne = new MAGNETOMETER();
                    magne.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.MagnetometerEntry.COLUMN_ID)));
                    magne.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.MagnetometerEntry.COLUMN_TIMESTAMP)));
                    magne.setX(c.getDouble(c.getColumnIndexOrThrow(GpsContract.MagnetometerEntry.COLUMN_X)));
                    magne.setY(c.getDouble(c.getColumnIndexOrThrow(GpsContract.MagnetometerEntry.COLUMN_Y)));
                    magne.setZ(c.getDouble(c.getColumnIndexOrThrow(GpsContract.MagnetometerEntry.COLUMN_Z)));
                    magnelist.add(magne);
                }
                return magnelist;
            } else {
                Log.e("i am here", "hello77777777");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally{
            c.close();
            db.close();
            Log.e("i am here2", "hello7777777");
        }
        Log.e("i am here3", "hello77777777");
        return null;
    }
    private ArrayList<PARKINGSTATE> find_all_park(){
        dbHelper = new GpsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = null;
        String s = "select Id, timestamp, state from parkingstate where Tag = 0 limit 200;";
        try {
            c = db.rawQuery(s, null);
            if (c != null && c.getCount() > 0) {
                ArrayList<PARKINGSTATE> parklist = new ArrayList<>();
                PARKINGSTATE park;
                while (c.moveToNext()) {
                    park = new PARKINGSTATE();
                    park.setId(c.getString(c.getColumnIndexOrThrow(GpsContract.ParkingStateEntry.COLUMN_ID)));
                    park.setTimestamp(c.getLong(c.getColumnIndexOrThrow(GpsContract.ParkingStateEntry.COLUMN_TIMESTAMP)));
                    park.setState(c.getInt(c.getColumnIndexOrThrow(GpsContract.ParkingStateEntry.COLUMN_STATE)));
                    parklist.add(park);
                }
                return parklist;
            } else {
                Log.e("i am here", "hello44444444");
            }
        }
        catch(Exception e){
            Log.e("exception: ", e.getMessage());
        }
        finally {
            c.close();
            db.close();
        }
        return null;
    }

    private JSONArray changeAcceDateToJson() {  //把一个集合转换成json格式的字符串
        AcceJsonArray=null;
        AcceJsonArray = new JSONArray();
        for (int i = 0; i < acces.size(); i++) {  //遍历上面初始化的集合数据，把数据加入JSONObject里面
            acce_object = new JSONObject();//一个user对象，使用一个JSONObject对象来装
            try {
                acce_object.put("UserID", acces.get(i).getId());  //从集合取出数据，放入JSONObject里面 JSONObject对象和map差不多用法,以键和值形式存储数据
                acce_object.put("Timestamp", acces.get(i).getTimestamp());
                acce_object.put("X", acces.get(i).getX());
                acce_object.put("Y", acces.get(i).getY());
                acce_object.put("Z", acces.get(i).getZ());
                AcceJsonArray.put(acce_object); //把JSONObject对象装入jsonArray数组里面
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return AcceJsonArray;
    }
    private JSONArray changeGyroDateToJson() {
        GyroJsonArray=null;
        GyroJsonArray = new JSONArray();
        for (int i = 0; i < gyros.size(); i++) {
            gyro_object = new JSONObject();
            try {
                gyro_object.put("UserID", gyros.get(i).getId());
                gyro_object.put("Timestamp", gyros.get(i).getTimestamp());
                gyro_object.put("X", gyros.get(i).getX());
                gyro_object.put("Y", gyros.get(i).getY());
                gyro_object.put("Z", gyros.get(i).getZ());
                GyroJsonArray.put(gyro_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return GyroJsonArray;
    }
    private JSONArray changeGpsDateToJson() {
        GpsJsonArray=null;
        GpsJsonArray = new JSONArray();
        for (int i = 0; i < gpses.size(); i++) {
            gps_object = new JSONObject();
            try {
                gps_object.put("UserID", gpses.get(i).getId());
                gps_object.put("Timestamp", gpses.get(i).getTimestamp());
                gps_object.put("Latitude", gpses.get(i).getLatitude());
                gps_object.put("Longitude", gpses.get(i).getLongitude());
                gps_object.put("Bearing", gpses.get(i).getBearing());
                gps_object.put("Speed", gpses.get(i).getSpeed());
                gps_object.put("Flag", gpses.get(i).getFlag());
                GpsJsonArray.put(gps_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return GpsJsonArray;
    }
    private JSONArray changeMotionDateToJson() {
        MotionJsonArray=null;
        MotionJsonArray = new JSONArray();
        for (int i = 0; i < motions.size(); i++) {
            motion_object = new JSONObject();
            try {
                motion_object.put("UserID", motions.get(i).getId());
                motion_object.put("Timestamp", motions.get(i).getTimestamp());
                motion_object.put("State", motions.get(i).getState());
                MotionJsonArray.put(motion_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return MotionJsonArray;
    }
    private JSONArray changeStepDateToJson() {
        StepJsonArray=null;
        StepJsonArray = new JSONArray();
        for (int i = 0; i < steps.size(); i++) {
            step_object = new JSONObject();
            try {
                step_object.put("UserID", steps.get(i).getId());
                step_object.put("Timestamp", steps.get(i).getTimestamp());
                step_object.put("Count", steps.get(i).getCount());
                StepJsonArray.put(step_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return StepJsonArray;
    }
    private JSONArray changeBatteryDateToJson() {
        BatteryJsonArray=null;
        BatteryJsonArray = new JSONArray();
        for (int i = 0; i < batteries.size(); i++) {
            battery_object = new JSONObject();
            try {
                battery_object.put("UserID", batteries.get(i).getId());
                battery_object.put("Timestamp", batteries.get(i).getTimestamp());
                battery_object.put("Percentage", batteries.get(i).getPercentage());
                BatteryJsonArray.put(battery_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return BatteryJsonArray;
    }
    private JSONArray changeWiFiDateToJson() {
        WiFiJsonArray=null;
        WiFiJsonArray = new JSONArray();
        for (int i = 0; i < wifis.size(); i++) {
            wifi_object = new JSONObject();
            try {
                wifi_object.put("UserID", wifis.get(i).getId());
                wifi_object.put("Timestamp", wifis.get(i).getTimestamp());
                wifi_object.put("State", wifis.get(i).getState());
                WiFiJsonArray.put(wifi_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return WiFiJsonArray;
    }
    private JSONArray changeMagneDateToJson() {  //把一个集合转换成json格式的字符串
        MagneJsonArray=null;
        MagneJsonArray = new JSONArray();
        for (int i = 0; i < magnetometers.size(); i++) {  //遍历上面初始化的集合数据，把数据加入JSONObject里面
            magne_object = new JSONObject();//一个user对象，使用一个JSONObject对象来装
            try {
                magne_object.put("UserID", magnetometers.get(i).getId());  //从集合取出数据，放入JSONObject里面 JSONObject对象和map差不多用法,以键和值形式存储数据
                magne_object.put("Timestamp", magnetometers.get(i).getTimestamp());
                magne_object.put("X", magnetometers.get(i).getX());
                magne_object.put("Y", magnetometers.get(i).getY());
                magne_object.put("Z", magnetometers.get(i).getZ());
                MagneJsonArray.put(magne_object); //把JSONObject对象装入jsonArray数组里面
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.e("here!!!!!!!!", " magne");
        return MagneJsonArray;
    }
    private JSONArray changeParkingDateToJson() {
        ParkJsonArray=null;
        ParkJsonArray = new JSONArray();
        for (int i = 0; i < parkinglots.size(); i++) {
            park_object = new JSONObject();
            try {
                park_object.put("UserID", parkinglots.get(i).getId());
                park_object.put("Timestamp", parkinglots.get(i).getTimestamp());
                park_object.put("State", parkinglots.get(i).getState());
                ParkJsonArray.put(park_object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return ParkJsonArray;
    }

    private int post_data(String url, JSONArray json){
        int StatusCode = 0;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext httpContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(url);

        try {

            StringEntity se = new StringEntity(json.toString());

            httpPost.setEntity(se);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");


            HttpResponse response = httpClient.execute(httpPost, httpContext); //execute your request and parse response
            HttpEntity entity = response.getEntity();

            String jsonString = EntityUtils.toString(entity); //if response in JSON format
            Log.e("response: ",jsonString);

            StatusCode = response.getStatusLine().getStatusCode();
            Log.e("status code: ", "" + StatusCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return StatusCode;
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

    private static CountDownLatch latch = null;

    /*===WiFi State===*/
//    NetworkInfo wifiCheck;
    private int[] wifistate = new int[1];
    private int[] result = new int[9];
    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo wifiNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if(wifiNetworkInfo != null) {
                if (wifiNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.e("wifi", "on");
                    wifistate[0] = 1;
                    boolean label = true;

                    while(label){

                        gpses = find_all_gps();
                        acces = find_all_acce();
                        gyros = find_all_gyro();
                        motions = find_all_motion();
                        steps = find_all_step();
                        batteries = find_all_battery();
                        wifis = find_all_wifi();
                        magnetometers = find_all_magne();
                        parkinglots = find_all_park();

                        if(gpses != null) {
                            latch = new CountDownLatch(6);
                            Thread t1 = new Thread() {
                                public void run() {
                                    result[0] = post_data(gps_url, changeGpsDateToJson());
                                    latch.countDown();
                                }
                            };
                            t1.start();
                            Thread t2 = new Thread() {
                                public void run() {
                                    result[1] = post_data(acce_url, changeAcceDateToJson());
                                    latch.countDown();
                                }
                            };
                            t2.start();
                            Thread t3 = new Thread() {
                                public void run() {
                                    result[2] = post_data(gyro_url, changeGyroDateToJson());
                                    latch.countDown();
                                }
                            };
                            t3.start();
                            Thread t4 = new Thread() {
                                public void run() {
                                    result[3] = post_data(step_url, changeStepDateToJson());
                                    latch.countDown();
                                }
                            };
                            t4.start();
                            Thread t5 = new Thread() {
                                public void run() {
                                    result[4] = post_data(motion_url, changeMotionDateToJson());
                                    latch.countDown();
                                }
                            };
                            t5.start();
                            Thread t8 = new Thread() {
                                public void run() {
                                    result[7] = post_data(magne_url, changeMagneDateToJson());
                                    latch.countDown();
                                }
                            };
                            t8.start();


                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.e("lalala", "-------");
                            Log.e("result[0]","!!!!!"+result[0]);
                            Log.e("result[1]","!!!!!"+result[1]);
                            Log.e("result[2]","!!!!!"+result[2]);
                            Log.e("result[3]","!!!!!"+result[3]);
                            Log.e("result[4]","!!!!!"+result[4]);
                            Log.e("result[7]","!!!!!"+result[7]);
                        }
                        if(batteries != null){
                            latch = new CountDownLatch(2);
                            Thread t6 = new Thread() {
                                public void run() {
                                    result[5] = post_data(wifi_url, changeWiFiDateToJson());
                                    latch.countDown();
                                }
                            };
                            t6.start();
                            Thread t7 = new Thread() {
                                public void run() {
                                    result[6] = post_data(battery_url, changeBatteryDateToJson());
                                    latch.countDown();
                                }
                            };
                            t7.start();
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.e("hehehe", "-------");
                            Log.e("result[5]","!!!!!"+result[5]);
                            Log.e("result[6]","!!!!!"+result[6]);
                        }

                        if(parkinglots != null){
                            latch = new CountDownLatch(1);
                            Thread t9 = new Thread() {
                                public void run() {
                                    result[8] = post_data(park_url, changeParkingDateToJson());
                                    latch.countDown();
                                }
                            };
                            t9.start();
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.e("hehehe", "-------");
                            Log.e("result[8]","!!!!!"+result[8]);
                        }
                        int sum = 0;
                        for(int i = 0; i < 5; i++)
                            sum += result[i];
                        sum += result[7];

                        if (sum == 1200 ) {         //5*200+200= 1200
                            latch = new CountDownLatch(1);
                            Thread t1 = new Thread() {
                                public void run() {

                                    db1 = dbHelper.getWritableDatabase();
                                    try {
                                        if (db1 != null) {
//                                            db1.execSQL("update gps_location set Tag = 1 where timestamp between ? and ?", new Object[]{gpses.get(0).getTimestamp(), gpses.get(gpses.size() - 1).getTimestamp()});
//                                            db1.execSQL("update accelerometer set Tag = 1 where timestamp between ? and ?", new Object[]{acces.get(0).getTimestamp(), acces.get(acces.size() - 1).getTimestamp()});
//                                            db1.execSQL("update gyroscope set Tag = 1 where timestamp between ? and ?", new Object[]{gyros.get(0).getTimestamp(), gyros.get(gyros.size() - 1).getTimestamp()});
//                                            db1.execSQL("update step set Tag = 1 where timestamp between ? and ?", new Object[]{steps.get(0).getTimestamp(), steps.get(steps.size() - 1).getTimestamp()});
//                                            db1.execSQL("update motionstate set Tag = 1 where timestamp between ? and ?", new Object[]{motions.get(0).getTimestamp(), motions.get(motions.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from gps_location where timestamp between ? and ?", new Object[]{gpses.get(0).getTimestamp(), gpses.get(gpses.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from accelerometer where timestamp between ? and ?", new Object[]{acces.get(0).getTimestamp(), acces.get(acces.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from gyroscope where timestamp between ? and ?", new Object[]{gyros.get(0).getTimestamp(), gyros.get(gyros.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from step where timestamp between ? and ?", new Object[]{steps.get(0).getTimestamp(), steps.get(steps.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from motionstate where timestamp between ? and ?", new Object[]{motions.get(0).getTimestamp(), motions.get(motions.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from magnetometer where timestamp between ? and ?", new Object[]{magnetometers.get(0).getTimestamp(), magnetometers.get(magnetometers.size() - 1).getTimestamp()});

                                        } else {
                                            Log.e("db1~~~~~~", "null");
                                        }
                                    } catch (Exception e) {
                                        Log.e("here~~~~~~~~~~~~~~", "stop upload");
                                        Log.e("exception: ", e.getMessage());
                                    } finally {
                                        db1.close();
                                    }
                                    latch.countDown();
                                }
                            };
                            t1.start();
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.e("lalala", "********");
                            result[0] = 0;
                            result[1] = 0;
                            result[2] = 0;
                            result[3] = 0;
                            result[4] = 0;
                            result[7] = 0;
                        }

                        int sum1 = 0;
                        for(int i = 0; i < 2; i++)
                            sum1 += result[i + 5];
                        if (sum1 == 400 ) {         //2*200 = 400
                            latch = new CountDownLatch(1);
                            Thread t2 = new Thread() {
                                public void run() {

                                    db1 = dbHelper.getWritableDatabase();
                                    try {
                                        if (db1 != null) {

//                                            db1.execSQL("update wifi set Tag = 1 where timestamp between ? and ?", new Object[]{wifis.get(0).getTimestamp(), wifis.get(wifis.size() - 1).getTimestamp()});
//                                            db1.execSQL("update battery set Tag = 1 where timestamp between ? and ?", new Object[]{batteries.get(0).getTimestamp(), batteries.get(batteries.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from wifi where timestamp between ? and ?", new Object[]{wifis.get(0).getTimestamp(), wifis.get(wifis.size() - 1).getTimestamp()});
                                            db1.execSQL("delete from battery where timestamp between ? and ?", new Object[]{batteries.get(0).getTimestamp(), batteries.get(batteries.size() - 1).getTimestamp()});

                                        } else {
                                            Log.e("db1~~~~~~", "null");
                                        }
                                    }
                                    catch(Exception e){
                                        Log.e("here~~~~~~~~~~~~~~", "stop upload");
                                        Log.e("exception: ", e.getMessage());
                                    }
                                    finally {
                                        db1.close();
                                    }
                                    latch.countDown();
                                }
                            };
                            t2.start();
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        result[5] = 0;
                        result[6] = 0;

                        if (result[8] == 200 ) {
                            latch = new CountDownLatch(1);
                            Thread t3 = new Thread() {
                                public void run() {

                                    db1 = dbHelper.getWritableDatabase();
                                    try {
                                        if (db1 != null) {

                                            db1.execSQL("delete from parkingstate where timestamp between ? and ?", new Object[]{parkinglots.get(0).getTimestamp(), parkinglots.get(parkinglots.size() - 1).getTimestamp()});

                                        } else {
                                            Log.e("db1~~~~~~", "null");
                                        }
                                    }
                                    catch(Exception e){
                                        Log.e("here~~~~~~~~~~~~~~", "stop upload");
                                        Log.e("exception: ", e.getMessage());
                                    }
                                    finally {
                                        db1.close();
                                    }
                                    latch.countDown();
                                }
                            };
                            t3.start();
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        result[8] = 0;
                        if(gpses == null && batteries == null && parkinglots == null){
                            label = false;
                        }
                    }
                    upload_state.setVisibility(android.view.View.VISIBLE);
                    unregisterReceiver(mConnReceiver);
                } else {
                    wifistate[0] = 0;
                    Log.e("wifi", "off");
                }
            }
            else{
                wifistate[0] = 0;
                Log.e("wifi", "off111");
            }
        }
    };

}

