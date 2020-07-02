package com.tencent.iot.hub.device.java.service;

/**
 * mqtt远程客户端选项
 */

public class TXMqttClientOptions {

    /**
     * 服务器URI
     */
    private String mServerURI;

    /**
     * Iot Hub控制台获取产品ID
     */
    private String mProductId;

    /**
     * 设备名，唯一
     */
    private String mDeviceName;

    private String mSecretKey;

   

    public TXMqttClientOptions() {
    }

    public TXMqttClientOptions(String serverURI, String productId, String deviceName, String secretKey) {
        this.mServerURI = serverURI;
        this.mProductId = productId;
        this.mDeviceName = deviceName;
        this.mSecretKey = secretKey;
    }


    public String getServerURI() {
        return mServerURI;
    }

    public TXMqttClientOptions serverURI(String serverURI) {
        this.mServerURI = serverURI;
        return this;
    }

    public String getProductId() {
        return mProductId;
    }

    public TXMqttClientOptions productId(String productId) {
        this.mProductId = productId;
        return this;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public TXMqttClientOptions deviceName(String deviceName) {
        this.mDeviceName = deviceName;
        return this;
    }

    public String getSecretKey() {
        return mSecretKey;
    }

    public TXMqttClientOptions secretKey(String secretKey) {
        this.mSecretKey = secretKey;
        return this;
    }


    @Override
    public String toString() {
        return "TXMqttClientOptions{" +
                "mServerURI='" + mServerURI + '\'' +
                ", mProductId='" + mProductId + '\'' +
                ", mDeviceName='" + mDeviceName + '\'' +
                '}';
    }
}
