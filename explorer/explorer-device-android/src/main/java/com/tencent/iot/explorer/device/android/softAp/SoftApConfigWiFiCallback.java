package com.tencent.iot.explorer.device.android.softAp;

public abstract class SoftApConfigWiFiCallback {

    /**
     * Callback on request connect wifi 请求设备连接wifi
     * @param ssid wifi ssid
     * @param password password password wifi密钥
     */
    public abstract void requestConnectWifi(String ssid, String password);
}
