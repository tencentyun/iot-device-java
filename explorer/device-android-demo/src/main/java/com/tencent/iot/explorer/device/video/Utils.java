package com.tencent.iot.explorer.device.video;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Utils {
    public static String VIDEO_OVER = "videoOver";

    public static void sendVideoOverBroadcast(Context context) {
        sendVideoBroadcast(context, 9);
    }

    public static void sendVideoBroadcast(Context context, int value) {
        Intent intent = new Intent("android.intent.action.CART_BROADCAST");
        intent.putExtra(VIDEO_OVER, value);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }
}
