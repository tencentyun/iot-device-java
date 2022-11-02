package com.tencent.iot.explorer.device.android.app;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.tencent.iot.explorer.device.android.http.IoTAuth;
import com.tencent.iot.explorer.device.android.utils.SharePreferenceUtil;

public class App extends Application {

    public static final String CONFIG = "config";

    public static final String ACCESS_TOKEN = "token";

    public static final String TOKEN_EXPIRED_TIME = "expiredTime";

    @Override
    public void onCreate() {
        super.onCreate();
        readLocalToken();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void readLocalToken() {
        String token = SharePreferenceUtil.getString(this, App.CONFIG, App.ACCESS_TOKEN);
        long expiredTime = SharePreferenceUtil.getLong(this, App.CONFIG, App.TOKEN_EXPIRED_TIME);
        IoTAuth.INSTANCE.init(token, expiredTime);
    }
}
