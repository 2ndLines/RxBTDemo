package com.hakim.rxbt;

import android.app.Application;

import com.hakim.rxbt.injector.component.AppComponent;
import com.hakim.rxbt.injector.component.DaggerAppComponent;
import com.hakim.rxbt.injector.module.AppModule;

/**
 * OKLine(ShenZhen) co.,Ltd.<br/>
 * Author : Shi Haijun <br/>
 * Email : haijun@okline.cn<br/>
 * Date : 2016/3/24 11:22<br/>
 * Desc :
 */
public class MyApplication extends Application {
    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        initInjector();
    }

    private void initInjector(){
        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
    }

    public AppComponent getAppComponent(){
        return appComponent;
    }

}
