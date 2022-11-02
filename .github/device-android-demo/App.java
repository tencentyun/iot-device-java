package com.tencent.iot.explorer.device.android.app;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.multidex.MultiDex;

import com.kugou.ultimatetv.UltimateTv;
import com.kugou.ultimatetv.constant.TvIntent;
import com.kugou.ultimatetv.util.BroadcastUtil;
import com.kugou.ultimatetv.util.KGLog;

import com.tencent.iot.explorer.device.android.http.IoTAuth;
import com.tencent.iot.explorer.device.android.utils.SharePreferenceUtil;

public class App extends Application {

    private final String TAG = App.class.getSimpleName();

    public static final String CONFIG = "config";

    public static final String ACCESS_TOKEN = "token";

    public static final String TOKEN_EXPIRED_TIME = "expiredTime";

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (KGLog.DEBUG) KGLog.d(TAG, "onReceive, action:" + action);
            if (TvIntent.ACTION_SONG_SERVICE_CREATE.equals(action)) {
                if (KGLog.DEBUG) KGLog.d(TAG, "ACTION_SONG_SERVICE_CREATE");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //调用sdk的onApplicationCreate
        UltimateTv.onApplicationCreate(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvIntent.ACTION_SONG_SERVICE_CREATE);
        BroadcastUtil.registerReceiver(mBroadcastReceiver, intentFilter);
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
