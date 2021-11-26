package com.tencent.iot.explorer.device.video;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Utils {
    public static String VIDEO_OVER = "videoOver";
    // 9 通话结束， 1 通话开始， 2 通话中对端挂断电话

    public static void sendVideoOverBroadcast(Context context) {
        Intent intent = new Intent("android.intent.action.CART_BROADCAST");
        intent.putExtra(VIDEO_OVER, 9);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }

    public static void sendVideoBroadcast(Context context, int value) {
        Intent intent = new Intent("android.intent.action.CART_BROADCAST");
        intent.putExtra(VIDEO_OVER, value);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        context.sendBroadcast(intent);
    }
}
