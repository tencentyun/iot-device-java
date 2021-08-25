package com.tencent.iot.explorer.device.java.core.samples.scenarized;

public class IoTLight {

    private static final String TAG = "IoTLightFragment";

    private static LightSample mLightSample;
    // Default testing parameters
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = "YOUR_PRODUCT_ID";
    private static String mDevName = "YOUR_DEV_NAME";
    private static String mDevPSK  = "null=="; //若使用证书验证，设为null

    private static String mDevCert = "";           // Cert String
    private static String mDevPriv = "";           // Priv String

    private final static String mJsonFileName = "gateway.json";
    private final static String mJsonFilePath = System.getProperty("user.dir") + "/src/test/resources/";
    public static void main(String[] args) {

        mLightSample = new LightSample(mBrokerURL, mProductID, mDevName, mDevPSK, mJsonFileName, mJsonFilePath);
        mLightSample.online();

    }
}
