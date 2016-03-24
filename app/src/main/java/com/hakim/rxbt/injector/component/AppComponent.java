package com.hakim.rxbt.injector.component;

import android.content.Context;

import com.hakim.rxbluetooth.IBluetooth;
import com.hakim.rxbt.MainActivity;
import com.hakim.rxbt.injector.module.AppModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/24 11:21<br/>
 * Desc :
 */
@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(MainActivity activity);

    Context provideApplication();

    IBluetooth provideBluetoothService();
}
