package com.tencent.iot.hub.device.java.core.dynreg;

public abstract class TXMqttBindDeviceCallback {

    public abstract void onBindFailed(Throwable cause);
    public abstract void onBindSuccess(String msg);
}
