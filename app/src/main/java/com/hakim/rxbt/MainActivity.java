package com.hakim.rxbt;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.hakim.rxbluetooth.IBluetooth;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import rx.functions.Action1;

import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Inject
    IBluetooth bluetooth;

    List<Subscription> subscriptions;

    private void initInjector(){
        ((MyApplication)getApplication()).getAppComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initInjector();
        setContentView(R.layout.activity_main);

    }

    private void subscribeBluetooth(){
        if(subscriptions == null){
            subscriptions = new ArrayList<>();
        }

        //作为服务端监听蓝牙连接
        subscriptions.add(bluetooth.asServer().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean success) {
                Log.d(TAG, "Success to listen & connect : " + success);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        })) ;

        //监听蓝牙广播发送的Action
        subscriptions.add(bluetooth.subscribeAction().subscribe(new Action1<String>() {
            @Override
            public void call(String action) {
                switch (action){
                    case ACTION_ACL_CONNECTED:
                        Log.i(TAG,"Connect");
                        break;
                    case ACTION_ACL_DISCONNECTED:
                        Log.i(TAG,"Disconnect");
                        break;
                    default:
                        break;
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        }));

        //监听蓝牙扫描到的设备
        subscriptions.add(bluetooth.subscribeFoundDevice().subscribe(new Action1<BluetoothDevice>() {
            @Override
            public void call(BluetoothDevice device) {

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        }));

        //监听蓝牙收到的数据
        subscriptions.add(bluetooth.subscribeFeedback().subscribe(new Action1<byte[]>() {
            @Override
            public void call(byte[] bytes) {
                Log.i(TAG,"Received data : " + bytes);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetooth.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetooth.unregister();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(subscriptions != null){
            for (Subscription subscription : subscriptions){
                subscription.unsubscribe();
            }
        }
    }
}

