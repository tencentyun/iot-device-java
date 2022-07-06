package com.tencent.iot.hub.device.java.core.dynreg;

/**
 * 绑定设备结果回调接口
 */
public abstract class TXMqttBindDeviceCallback {

    /**
     * 绑定失败
     * @param cause 失败原因
     */
    public abstract void onBindFailed(Throwable cause);

    /**
     * 绑定成功
     *
     * @param msg 平台返回的绑定成功的消息
     */
    public abstract void onBindSuccess(String msg);
}
