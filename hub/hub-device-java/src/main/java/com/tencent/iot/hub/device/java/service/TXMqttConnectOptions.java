package com.tencent.iot.hub.device.java.service;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * 该类负责序列化 MqttConnectOptions 相关信息
 */
public class TXMqttConnectOptions {

    private int mConnectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
    private int mKeepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
    private boolean mAutomaticReconnect = false;
    private boolean mCleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;

    /**
     * 是否使用 shadow
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

    /**
     * 构造函数
     */
    public TXMqttConnectOptions() {
    }

    /**
     * 构造函数
     *
     * @param mConnectionTimeout 连接超时时间
     * @param mKeepAliveInterval 保活周期
     * @param mAutomaticReconnect 是否自动重连
     * @param mCleanSession 是否清理 session
     * @param mAsymcEncryption 异步加密
     * @param mDeviceCertName 设备证书
     * @param mDeviceKeyName 设备密钥
     * @param mSecretKey 密钥
     */
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

    /**
     * 获取连接超时时长
     *
     * @return 连接超时时长
     */
    public int getConnectionTimeout() {
        return mConnectionTimeout;
    }

    /**
     * 设置连接超时时长
     *
     * @param connectionTimeout 连接超时时长
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setConnectionTimeout(int connectionTimeout) {
        this.mConnectionTimeout = connectionTimeout;
        return this;
    }

    /**
     * 获取保活周期
     *
     * @return 保活周期
     */
    public int getKeepAliveInterval() {
        return mKeepAliveInterval;
    }

    /**
     * 设置保活周期
     *
     * @param mKeepAliveInterval 保活周期
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setKeepAliveInterval(int mKeepAliveInterval) {
        this.mKeepAliveInterval = mKeepAliveInterval;
        return this;
    }

    /**
     * 是否自动重连
     *
     * @return 是否自动重连
     */
    public boolean isAutomaticReconnect() {
        return mAutomaticReconnect;
    }

    /**
     * 设置自动重连
     *
     * @param mAutomaticReconnect 是否自动重连
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setAutomaticReconnect(boolean mAutomaticReconnect) {
        this.mAutomaticReconnect = mAutomaticReconnect;
        return this;
    }

    /**
     * 是否清理 session
     *
     * @return 是否清理 session
     */
    public boolean isCleanSession() {
        return mCleanSession;
    }

    /**
     * 设置是否清理 session
     *
     * @param mCleanSession 是否清理 session
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setCleanSession(boolean mCleanSession) {
        this.mCleanSession = mCleanSession;
        return this;
    }

    /**
     * 是否异步加密
     *
     * @return 是否异步加密
     */
    public boolean isAsymcEncryption() {
        return mAsymcEncryption;
    }

    /**
     * 设置是否异步加密
     *
     * @param mAsymcEncryption 是否异步加密
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setAsymcEncryption(boolean mAsymcEncryption) {
        this.mAsymcEncryption = mAsymcEncryption;
        return this;
    }

    /**
     * 获取设备证书
     * @return 设备证书
     */
    public String getDeviceCertName() {
        return mDeviceCertName;
    }

    /**
     * 设置设备证书
     *
     * @param mDeviceCertName 设备证书
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setDeviceCertName(String mDeviceCertName) {
        this.mDeviceCertName = mDeviceCertName;
        return this;
    }

    /**
     * 获取设备密钥
     *
     * @return 设备密钥
     */
    public String getDeviceKeyName() {
        return mDeviceKeyName;
    }

    /**
     * 设置设备密钥
     *
     * @param mDeviceKeyName 设备密钥
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setDeviceKeyName(String mDeviceKeyName) {
        this.mDeviceKeyName = mDeviceKeyName;
        return this;
    }

    /**
     * 获取密钥
     *
     * @return 密钥
     */
    public String getSecretKey() {
        return mSecretKey;
    }

    /**
     * 设置密钥
     *
     * @param secretKey 密钥
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setSecretKey(String secretKey) {
        this.mSecretKey = secretKey;
        return this;
    }

    /**
     * 是否使用影子
     *
     * @return 是否使用影子
     */
    public boolean isUseShadow() {
        return mUseShadow;
    }

    /**
     * 设置是否使用影子
     *
     * @param mUseShadow 是否使用影子
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttConnectOptions setUseShadow(boolean mUseShadow) {
        this.mUseShadow = mUseShadow;
        return this;
    }
}
