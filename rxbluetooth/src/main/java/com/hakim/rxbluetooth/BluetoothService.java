package com.hakim.rxbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static android.bluetooth.BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;
import static android.bluetooth.BluetoothAdapter.EXTRA_CONNECTION_STATE;
import static android.bluetooth.BluetoothAdapter.EXTRA_STATE;
import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;
import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/18 16:54<br/>
 * Desc :
 */

@Singleton
public class BluetoothService implements IBluetooth {
    private static final String TAG = "BluetoothService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothSecure";
    private static final String NAME_INSECURE = "BluetoothInsecure";
    private static final int FEEDBACK_TIME_OUT_IN_MS = 10000; //ms

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    private static final int CONNECTION_MAX_RETRY_TIME = 3;

    private final BluetoothAdapter adapter;
    private final PublishSubject<Object> broadcastReceiverSubject = PublishSubject.create();
    private final PublishSubject<byte[]> socketSubject = PublishSubject.create();
    private ExecutorService threadPool;
    private SocketHandler mSocketHandler;


    private final Context mContext;

    @Inject
    public BluetoothService(Context context) {

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IllegalStateException("Fail to initialize BluetoothAdapter");
        }

        this.mContext = context;
        threadPool = Executors.newCachedThreadPool();
    }

    @Override
    public void startScanDevice() {
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
        stopScanDevice();
        adapter.startDiscovery();
    }

    @Override
    public void stopScanDevice() {
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
    }

    /**
     * 订阅蓝牙广播动作，未指定默认调度器
     *
     * @return
     */
    @Override
    public Observable<String> subscribeAction() {
        return broadcastReceiverSubject.asObservable().ofType(String.class);
    }

    /**
     * 订阅扫描到的蓝牙设备，为指定默认调度器
     *
     * @return
     */
    @Override
    public Observable<BluetoothDevice> subscribeFoundDevice() {
        return broadcastReceiverSubject.asObservable().ofType(BluetoothDevice.class);
    }

    /**
     * 订阅蓝牙的各种状态，为制定默认调度器
     *
     * @return
     */
    @Override
    public Observable<Integer> subscribeBluetoothState() {
        return broadcastReceiverSubject.asObservable().ofType(Integer.class);
    }

    /**
     * 作为客户端，向服务端发起连接请求。默认在io调度器上执行
     *
     * @param address 服务器端蓝牙地址
     */
    @Override
    public Observable<Boolean> connect(final String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return Observable.error(new Throwable("The bluetooth address is not valid"));
        }
        stopScanDevice();
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    BluetoothDevice device = adapter.getRemoteDevice(address);
                    try {
                        subscriber.onNext(connectDevice(device));
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 连接
     *
     * @param device
     * @return
     * @throws IOException
     */
    private boolean connectDevice(BluetoothDevice device) throws IOException {
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
        try {
            bondDevice(device);
        } catch (BondFailureException e) {
//            e.printStackTrace();
            Log.e(TAG, "Fail to bond device[" + device.getName() + "]");
        }

        int time = CONNECTION_MAX_RETRY_TIME;
        boolean connected = false;
        do {
            try {
                Log.d(TAG, "Try to connect device[" + device.getAddress() + "] : " + time);
                socket.connect();
                Log.d(TAG, "Success to connect !!");
                //Success to connect device
                connected = true;
                connected(socket);

            } catch (IOException e) {
                e.printStackTrace();
                time--;
                connected = false;
            }
        } while (!connected && time != 0);

        if (!connected) {
            try {
                socket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }

        return connected;
    }

    private boolean connected(BluetoothSocket socket) throws IOException {
        if (mSocketHandler != null) {
            mSocketHandler.cancel();
            mSocketHandler = null;
        }
        mSocketHandler = new SocketHandler(socket);
        try {
            threadPool.execute(mSocketHandler);
        } catch (RejectedExecutionException e) {
//            e.printStackTrace();
            Log.e(TAG, "############Reject execute task##############");
            return false;
        }
        return true;
    }

    /**
     * 配对
     *
     * @param device
     */
    private void bondDevice(BluetoothDevice device) throws BondFailureException {
        if (BOND_NONE == device.getBondState()) {
            try {
                Method method = BluetoothDevice.class.getMethod("createBond");
                method.invoke(device);
            } catch (Exception e) {
                throw new BondFailureException("Fail to bond device");
            }
        }
    }

    /**
     * 断开蓝牙连接
     */
    @Override
    public void disconnect() {
        if (mSocketHandler != null) {
            mSocketHandler.cancel();
        }
    }

    /**
     * 作为服务器端,接受其他蓝牙设备的连接请求。默认在IO调度器上执行
     *
     */
    @Override
    public Observable<Boolean> asServer() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if(!subscriber.isUnsubscribed()){
                    try {
                        BluetoothServerSocket serverSocket = adapter.listenUsingRfcommWithServiceRecord
                                (NAME_SECURE, MY_UUID_SECURE);
                        BluetoothSocket socket = serverSocket.accept();
                        subscriber.onNext(connected(socket));
                        subscriber.onCompleted();
                    } catch (IOException e) {
//                        e.printStackTrace();
                        subscriber.onError(e);
                    }
                }

            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 发送数据，可检测发送成功与否
     *
     * @param data 发送的数据
     * @return 返回可观察发送结果（成功与否）的可观察对象
     */
    @Override
    public Observable<Boolean> writeQuiet(final byte[] data) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> observer) {
                if (!observer.isUnsubscribed()) {
                    String error = write(data);
                    if (error == null) {
                        observer.onNext(true);
                        observer.onCompleted();
                    } else {
                        observer.onError(new Throwable(error));
                    }
                }
            }
        });

    }

    @Override
    public Observable<byte[]> writeForResult(byte[] data) {
        return writeForResult(data, FEEDBACK_TIME_OUT_IN_MS);
    }

    /**
     * 向蓝牙些数据， 默认使用io调度器<br/>
     * {@code timeout} 时间内没有返回数据，<br/>
     * 则抛出超时的异常
     *
     * @param data    发送的数据
     * @param timeout 超过timeout的时间，则抛出异常，单位毫秒(ms)
     * @return 返回   返回可接受响应数据的可观察对象
     */
    @Override
    public Observable<byte[]> writeForResult(byte[] data, int timeout) {
        String error = write(data);
        if (error != null) {
            socketSubject.onError(new Throwable(error));
        }
        return socketSubject.asObservable()
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io());
    }

    private String write(byte[] data) {
        String error = null;
        if (data != null) {
            if (mSocketHandler != null) {
                try {
                    mSocketHandler.write(data);
                } catch (IOException e) {
//                    e.printStackTrace();
                    error = "Bluetooth has disconnected ";
                }
            } else {
                error = " Bluetooth has not been connected yet! ";
            }
        } else {
            error = "Parameter data is null";
        }
        return error;
    }

    /**
     * 订阅收到的数据。未指定默认调度器
     */
    @Override
    public Observable<byte[]> subscribeFeedback() {
        return socketSubject.asObservable();
    }

    /**
     * 注册广播，
     *
     * @see BluetoothAdapter#ACTION_DISCOVERY_STARTED
     * @see BluetoothAdapter#ACTION_DISCOVERY_FINISHED
     * @see BluetoothAdapter#ACTION_CONNECTION_STATE_CHANGED
     * @see BluetoothDevice#ACTION_FOUND
     * @see BluetoothDevice#ACTION_ACL_CONNECTED
     * @see BluetoothDevice#ACTION_ACL_DISCONNECTED
     * @see BluetoothDevice#ACTION_BOND_STATE_CHANGED
     * @see BluetoothAdapter#ACTION_DISCOVERY_STARTED
     */
    @Override
    public void register() {
        registerBTBroadcast();
    }

    /**
     * 注销广播
     */
    @Override
    public void unregister() {
        unregisterBTBroadcast();
    }

    private void registerBTBroadcast() {
        IntentFilter filter = new IntentFilter(ACTION_DISCOVERY_STARTED);
        filter.addAction(ACTION_DISCOVERY_FINISHED);
        filter.addAction(ACTION_FOUND);
        filter.addAction(ACTION_STATE_CHANGED);
        filter.addAction(ACTION_BOND_STATE_CHANGED);
        //Bluetooth connect
        filter.addAction(ACTION_ACL_CONNECTED);
        //Bluetooth disconnect
        filter.addAction(ACTION_ACL_DISCONNECTED);
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED);//not work, why ?
        mContext.registerReceiver(bluetoothReceiver, filter);
        Log.d(TAG, " Register broadcast! ");

    }

    private void unregisterBTBroadcast() {
        mContext.unregisterReceiver(bluetoothReceiver);
        Log.d(TAG, " Unregister broadcast! ");
    }

    /*package*/ void omitData(PublishSubject subject, Object object) {
        if (object instanceof Integer) {
            Log.w(TAG, "Omit Bluetooth state : " + object);
        }

        if (subject.hasObservers()) {
            subject.onNext(object);
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "Bluetooth current action ---> " + action);
            omitData(broadcastReceiverSubject, action);
            switch (action) {
                case ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                    omitData(broadcastReceiverSubject, device);
                    break;
                case ACTION_ACL_CONNECTED:
                    break;
                case ACTION_CONNECTION_STATE_CHANGED:
                    //蓝牙连接状态的改变
                    Integer connectionState = intent.getIntExtra(EXTRA_CONNECTION_STATE,
                            BluetoothAdapter.STATE_DISCONNECTED);
                    Log.i(TAG, "############### Bluetooth CONNECTION state is changed! " +
                            "new state = " + connectionState);
                    omitData(broadcastReceiverSubject, connectionState);
                    break;
                case ACTION_STATE_CHANGED:
                    //蓝牙打开关闭状态的改变
                    Integer onOffState = intent.getIntExtra(EXTRA_STATE, STATE_OFF);
                    Log.i(TAG, "############### Bluetooth On/Off state is changed! " +
                            "new state = " + onOffState);
                    omitData(broadcastReceiverSubject, onOffState);
                    break;
                case ACTION_BOND_STATE_CHANGED:
                    //蓝牙绑定状态的改变
                    Integer bondState = intent.getIntExtra(EXTRA_BOND_STATE, BOND_NONE);
                    Log.i(TAG, "############### Bluetooth BOND state is changed! " +
                            "new state = " + bondState);
                    omitData(broadcastReceiverSubject, bondState);
                default:
                    break;
            }
        }
    };

    private class SocketHandler implements Runnable {
        private BluetoothSocket mmSocket;
        private InputStream mmInput;
        private OutputStream mmOutput;

        public SocketHandler(BluetoothSocket socket) throws IOException {
            Log.d(TAG, "Start read thread !");
            mmSocket = socket;
            mmInput = mmSocket.getInputStream();
            mmOutput = mmSocket.getOutputStream();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            for (; ; ) {
                try {
                    int size = mmInput.read(buffer);
                    if (size != -1) {
                        byte[] received = Arrays.copyOfRange(buffer, 0, size);
                        omitData(socketSubject, received);
                    }
                } catch (IOException e) {
//                    e.printStackTrace();
                    omitData(socketSubject, new Throwable(e));
                    break;
                }
            }
        }

        public void write(byte[] data) throws IOException {
            mmOutput.write(data);
            Log.d(TAG, "Write to bluetooth : " + new String(data));
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
//                e.printStackTrace();
                Log.e(TAG, "Fail to close socket");
            }
        }

    }

}

