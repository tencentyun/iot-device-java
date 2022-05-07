package com.tencent.iot.explorer.device.android.app;

import android.app.Application;

import com.tencent.iot.explorer.device.rtc.log.LogcatHelper;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LogcatHelper.getInstance(this).start();
    }
}
