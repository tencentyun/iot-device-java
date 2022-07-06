package com.tencent.iot.explorer.device.android.app.utils;

public abstract class CreateWifiHotspotCallback {
    /**
     * 打开热点成功回调
     * @param ssid 热点名称ssid
     * @param pwd 热点密钥pwd
     */
    public abstract void onCreateWifiHotspotSuccess(String ssid, String pwd);

    /**
     * 打开热点失败回调
     * @param code 失败错误码
     * @param errorMsg 失败消息
     * @param e 失败异常
     */
    public abstract void onCreateWifiHotspotFailure(String code, String errorMsg, Exception e);
}
