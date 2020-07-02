package com.tencent.iot.hub.device.java.service;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * 该类负责序列化MqttConnectOptions相关信息
 */

public class TXMqttConnectOptions {

    private int mConnectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
    private int mKeepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
    private boolean mAutomaticReconnect = false;
    private boolean mCleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;

    /**
     * 是否使用shadow
     */
    private boolean mUseShadow = false;

    /**
     * 是否采用非对称加密
     */
    private boolean mAsymcEncryption = true;

    /**
     * 设备证书名
     */
    private String mDeviceCertName;

    /**
     * 设备私钥文件名
     */
    private String mDeviceKeyName;

    /**
     * 设备密码
     */
    private String mSecretKey;

    public TXMqttConnectOptions() {
    }

    public TXMqttConnectOptions(int mConnectionTimeout, int mKeepAliveInterval, boolean mAutomaticReconnect, boolean mCleanSession,
                                boolean mAsymcEncryption, String mDeviceCertName, String mDeviceKeyName, String mSecretKey) {
        this.mConnectionTimeout = mConnectionTimeout;
        this.mKeepAliveInterval = mKeepAliveInterval;
        this.mAutomaticReconnect = mAutomaticReconnect;
        this.mCleanSession = mCleanSession;
        this.mAsymcEncryption = mAsymcEncryption;
        this.mDeviceCertName = mDeviceCertName;
        this.mDeviceKeyName = mDeviceKeyName;
        this.mSecretKey = mSecretKey;
    }

    public int getConnectionTimeout() {
        return mConnectionTimeout;
    }

    public TXMqttConnectOptions setConnectionTimeout(int mConnectionTimeout) {
        this.mConnectionTimeout = mConnectionTimeout;
        return this;
    }

    public int getKeepAliveInterval() {
        return mKeepAliveInterval;
    }

    public TXMqttConnectOptions setKeepAliveInterval(int mKeepAliveInterval) {
        this.mKeepAliveInterval = mKeepAliveInterval;
        return this;
    }

    public boolean isAutomaticReconnect() {
        return mAutomaticReconnect;
    }

    public TXMqttConnectOptions setAutomaticReconnect(boolean mAutomaticReconnect) {
        this.mAutomaticReconnect = mAutomaticReconnect;
        return this;
    }

    public boolean isCleanSession() {
        return mCleanSession;
    }

    public TXMqttConnectOptions setCleanSession(boolean mCleanSession) {
        this.mCleanSession = mCleanSession;
        return this;
    }

    public boolean isAsymcEncryption() {
        return mAsymcEncryption;
    }

    public TXMqttConnectOptions setAsymcEncryption(boolean mAsymcEncryption) {
        this.mAsymcEncryption = mAsymcEncryption;
        return this;
    }

    public String getDeviceCertName() {
        return mDeviceCertName;
    }

    public TXMqttConnectOptions setDeviceCertName(String mDeviceCertName) {
        this.mDeviceCertName = mDeviceCertName;
        return this;
    }

    public String getDeviceKeyName() {
        return mDeviceKeyName;
    }

    public TXMqttConnectOptions setDeviceKeyName(String mDeviceKeyName) {
        this.mDeviceKeyName = mDeviceKeyName;
        return this;
    }

    public String getSecretKey() {
        return mSecretKey;
    }

    public TXMqttConnectOptions setSecretKey(String secretKey) {
        this.mSecretKey = secretKey;
        return this;
    }

    protected boolean isUseShadow() {
        return mUseShadow;
    }

    protected TXMqttConnectOptions setUseShadow(boolean mUseShadow) {
        this.mUseShadow = mUseShadow;
        return this;
    }
}
