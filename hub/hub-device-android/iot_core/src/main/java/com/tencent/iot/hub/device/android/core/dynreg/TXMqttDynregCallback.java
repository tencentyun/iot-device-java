package com.tencent.iot.hub.device.android.core.dynreg;

/**
 * Created by willssong on 2019/7/15
 * willssong@tencent.com
 */
public abstract class TXMqttDynregCallback {

    /**
     * Callback on getting device PSK
     * @param devicePsk
     */
    public abstract void onGetDevicePSK(String devicePsk);

    /**
     * Callback on getting deivce cert and priv
     * @param deivceCert
     * @param devicePriv
     */
    public abstract void onGetDeviceCert(String deivceCert, String devicePriv);

    /**
     * Callback on dynamic register failed
     * @param cause
     * @param errMsg
     */
    public abstract void onFailedDynreg(Throwable cause, String errMsg);

    /**
     * Callback on dynamic register failed
     * @param cause
     */
    public abstract void onFailedDynreg(Throwable cause);
}
