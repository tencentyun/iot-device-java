package com.tencent.iot.explorer.device.android.app.utils;

public abstract class CloseWifiHotspotCallback {
    /**
     * 关闭热点成功回调
     */
    public abstract void onCloseWifiHotspotSuccess();

    /**
     * 关闭热点失败回调
     * @param errorMsg 失败消息
     * @param e 失败异常
     */
    public abstract void onCloseWifiHotspotFailure(String errorMsg, Exception e);
}
