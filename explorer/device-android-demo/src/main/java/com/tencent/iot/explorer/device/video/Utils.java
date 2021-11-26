package com.tencent.iot.explorer.device.video;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Utils {
    public static String VIDEO_OVER = "videoOver";

    public static void sendVideoOverBroadcast(Context context) {
        Intent intent = new Intent("android.intent.action.CART_BROADCAST");
        intent.putExtra(VIDEO_OVER, 0);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }
}
