package com.hakim.rxbluetooth;

import android.bluetooth.BluetoothDevice;

import rx.Observable;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/18 16:41<br/>
 * Desc :
 */
public interface IBluetooth {

    void startScanDevice();

    void stopScanDevice();

    Observable<String> subscribeAction();

    Observable<BluetoothDevice> subscribeFoundDevice();

    Observable<Integer> subscribeBluetoothState();

    Observable<Boolean> asServer();

    Observable<Boolean> connect(String address);

    void disconnect();

    Observable<byte[]> writeForResult(byte[] data);

    Observable<byte[]> writeForResult(byte[] data, int timeout);

    Observable<Boolean> writeQuiet(byte[] data);

    Observable<byte[]> subscribeFeedback();

    void register();

    void unregister();

}
