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

public class App extends Application {

    private final String TAG = App.class.getSimpleName();

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
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
