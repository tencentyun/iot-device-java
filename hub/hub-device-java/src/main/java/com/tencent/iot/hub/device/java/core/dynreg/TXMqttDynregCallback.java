package com.tencent.iot.hub.device.java.core.dynreg;

/**
 * 动态注册结果回调接口
 */
public abstract class TXMqttDynregCallback {

    /**
     * 回调设备密钥
     *
     * @param devicePsk 设备密钥
     */
    public abstract void onGetDevicePSK(String devicePsk);

    /**
     * 回调设备证书和设备私钥
     * @param deivceCert 设备证书
     * @param devicePriv 设备私钥
     */
    public abstract void onGetDeviceCert(String deivceCert, String devicePriv);

    /**
     * 动态注册失败
     * @param cause 失败原因
     * @param errMsg 平台返回的失败信息
     */
    public abstract void onFailedDynreg(Throwable cause, String errMsg);

    /**
     * 动态注册失败
     * @param cause 失败原因
     */
    public abstract void onFailedDynreg(Throwable cause);
}
