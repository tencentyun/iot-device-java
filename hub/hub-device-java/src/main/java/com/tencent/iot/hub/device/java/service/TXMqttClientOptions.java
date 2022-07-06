package com.tencent.iot.hub.device.java.service;

/**
 * mqtt 远程客户端选项
 */
public class TXMqttClientOptions {

    /**
     * 服务器 URI
     */
    private String mServerURI;

    /**
     * Iot Hub 控制台获取产品 ID
     */
    private String mProductId;

    /**
     * 设备名，唯一
     */
    private String mDeviceName;

    private String mSecretKey;

    /**
     * 构造函数
     */
    public TXMqttClientOptions() {
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param productId 产品 ID
     * @param deviceName 设备名
     * @param secretKey 密钥
     */
    public TXMqttClientOptions(String serverURI, String productId, String deviceName, String secretKey) {
        this.mServerURI = serverURI;
        this.mProductId = productId;
        this.mDeviceName = deviceName;
        this.mSecretKey = secretKey;
    }

    /**
     * 获取服务器 URI
     *
     * @return 服务器 URI
     */
    public String getServerURI() {
        return mServerURI;
    }

    /**
     * 设置服务器 URI
     *
     * @param serverURI 服务器 URI
     * @return {@link TXMqttConnectOptions}
     */
    public TXMqttClientOptions serverURI(String serverURI) {
        this.mServerURI = serverURI;
        return this;
    }

    /**
     * 获取产品 ID
     *
     * @return 产品 ID
     */
    public String getProductId() {
        return mProductId;
    }

    /**
     * 设置产品 ID
     *
     * @param productId 产品 ID
     * @return {@link TXMqttClientOptions}
     */
    public TXMqttClientOptions productId(String productId) {
        this.mProductId = productId;
        return this;
    }

    /**
     * 获取设备名
     *
     * @return 设备名
     */
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * 设置设备名
     *
     * @param deviceName 设备名
     * @return {@link TXMqttClientOptions}
     */
    public TXMqttClientOptions deviceName(String deviceName) {
        this.mDeviceName = deviceName;
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
     * @return {@link TXMqttClientOptions}
     */
    public TXMqttClientOptions secretKey(String secretKey) {
        this.mSecretKey = secretKey;
        return this;
    }

    /**
     * 转换成标准格式的字符串内容
     *
     * @return 标准字符串内容
     */
    @Override
    public String toString() {
        return "TXMqttClientOptions{" +
                "mServerURI='" + mServerURI + '\'' +
                ", mProductId='" + mProductId + '\'' +
                ", mDeviceName='" + mDeviceName + '\'' +
                '}';
    }
}
