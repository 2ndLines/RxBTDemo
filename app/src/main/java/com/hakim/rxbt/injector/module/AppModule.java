package com.hakim.rxbt.injector.module;

import android.app.Application;
import android.content.Context;

import com.hakim.rxbluetooth.BluetoothService;
import com.hakim.rxbluetooth.IBluetooth;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/24 11:18<br/>
 * Desc :
 */
@Module
public class AppModule {
    private final Application application;

    public AppModule(Application application){
        this.application = application;
    }

    @Provides @Singleton
    Context provideApplicationContext(){
        return application;
    }

    @Provides @Singleton
    IBluetooth provideBluetoothService(BluetoothService service){
        return service;
    }

}
