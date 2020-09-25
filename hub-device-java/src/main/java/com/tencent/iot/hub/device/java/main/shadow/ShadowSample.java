package com.tencent.iot.hub.device.java.main.shadow;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;


public class ShadowSample {

    private static final String TAG = ShadowSample.class.getSimpleName();
    private static final Logger LOG = LoggerFactory.getLogger(ShadowSample.class);
    
    /**
     * 产品名称
     */
    private static final String PRODUCT_ID = "YOUR_PRODUCT_ID";

    /**
     * 设备名称
     */
    private static final String DEVICE_NAME = "YOUR_DEVICE_NAME";


    /**
     * 密钥
     */
    private static final String SECRET_KEY = null;
	
    /**
     * 设备证书名
     */
    private static final String DEVICE_CERT_NAME = "YOUR_DEVICE_NAME_cert.crt";

    /**
     * 设备私钥文件名
     */
    private static final String DEVICE_KEY_NAME = "YOUR_DEVICE_NAME_private.key";


    /**
     * shadow连接实例
     */
    private TXShadowConnection mShadowConnection;

    /**
     * shadow action 回调接口
     */
    private TXShadowActionCallBack mShadowActionCallBack;

    private AtomicInteger mUpdateCount = new AtomicInteger(0);

    private AtomicInteger mTemperatureDesire = new AtomicInteger(20);

    /**
     * 设备属性集（该变量必须为全局变量）
     */
    private List<DeviceProperty> mDevicePropertyList = null;

    private boolean isConnected = false;

    public ShadowSample(TXShadowActionCallBack shadowActionCallBack) {
        this.mShadowActionCallBack = shadowActionCallBack;
        this.mDevicePropertyList = new ArrayList<>();
    }

    public void getDeviceDocument() {
        if (!isConnected) {
            return;
        }
        mShadowConnection.get(null);
    }

    public void connect() {
        LOG.info("{}", "connect");

        mShadowConnection = new TXShadowConnection(PRODUCT_ID, DEVICE_NAME, SECRET_KEY, mShadowActionCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);
        options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(DEVICE_CERT_NAME, DEVICE_KEY_NAME));
        Status status = mShadowConnection.connect(options, null);
        LOG.info("connect IoT completed, status[{}]", status);
        isConnected = true;
    }

    public void registerProperty() {
        if (!isConnected) {
            return;
        }

        DeviceProperty deviceProperty1 = new DeviceProperty();
        deviceProperty1.key("updateCount").data(String.valueOf(mUpdateCount.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
        mShadowConnection.registerProperty(deviceProperty1);

        DeviceProperty deviceProperty2 = new DeviceProperty();
        deviceProperty2.key("temperatureDesire").data(String.valueOf(mTemperatureDesire.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
        mShadowConnection.registerProperty(deviceProperty2);

        mDevicePropertyList.add(deviceProperty1);
        mDevicePropertyList.add(deviceProperty2);
    }

    public void closeConnect() {
        if (!isConnected) {
            return;
        }
        mShadowConnection.disConnect(null);
        isConnected = false;
    }

    public void updateDeviceProperty(String propertyJSONDocument, List<DeviceProperty> devicePropertyList) {
        if (!isConnected) {
            return;
        }
        LOG.info("{}", "update device property success and report null desired info");
        // 在确认delta更新后，调用reportNullDesiredInfo()接口进行上报
        mShadowConnection.reportNullDesiredInfo();
    }
   
}