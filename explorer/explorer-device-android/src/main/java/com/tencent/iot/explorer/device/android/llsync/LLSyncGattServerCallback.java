package com.tencent.iot.explorer.device.android.llsync;

public abstract class LLSyncGattServerCallback {

    /**
     * Callback on start server onFailure 启动gatt服务失败
     * @param errorMessage error message 错误消息
     */
    public abstract void onFailure(String errorMessage);

    /**
     * Callback on request connect wifi 主机请求连接wifi
     * @param ssid wifi ssid
     * @param password password password wifi密钥
     */
    public abstract void requestConnectWifi(String ssid, String password);

    /**
     * Callback on request appbindtoken 主机请求设备绑定发来的token
     * @param token wifi ssid
     */
    public abstract void requestAppBindToken(String token);
}
