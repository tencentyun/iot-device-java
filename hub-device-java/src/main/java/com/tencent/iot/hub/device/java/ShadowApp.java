package com.tencent.iot.hub.device.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.pqc.math.linearalgebra.GoppaCode.MaMaPe;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.logging.JSR47Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;


public class ShadowApp {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);
	private static TXShadowConnection mShadowConnection;
	private static String testProductIDString = "YOUR_PRODUCT_ID";
	private static String testDeviceNameString = "YOUR_DEVICE_NAME";
	private static String testPSKString = "YOUR_PSK";
	private static String testTopicString = testProductIDString + "/" + testDeviceNameString + "/data"; 
	private static boolean testFinished = false;
	private static int pubCount = 0;
	private static final int testCnt = 1000;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MqttConnectOptions options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(240);
		options.setAutomaticReconnect(false);
		
		options.setSocketFactory(AsymcSslUtils.getSocketFactory());
		
		mShadowConnection = new TXShadowConnection(testProductIDString, testDeviceNameString, testPSKString, new callback());
		mShadowConnection.connect(options, null);
		try {
			while(pubCount < testCnt) {
				pubCount += 1;
				Thread.sleep(20000);

				if (pubCount < 3) {  // 更新设备影子

					List<DeviceProperty>  mDevicePropertyList = new ArrayList<>();

					DeviceProperty deviceProperty1 = new DeviceProperty();
					deviceProperty1.key("updateCount").data(String.valueOf(pubCount)).dataType(TXShadowConstants.JSONDataType.INT);
					mShadowConnection.registerProperty(deviceProperty1);

					DeviceProperty deviceProperty2 = new DeviceProperty();
					deviceProperty2.key("energyConsumption").data(String.valueOf(10+pubCount)).dataType(TXShadowConstants.JSONDataType.INT);
					mShadowConnection.registerProperty(deviceProperty2);

					DeviceProperty deviceProperty3 = new DeviceProperty();
					deviceProperty3.key("temperatureDesire").data(String.valueOf(25)).dataType(TXShadowConstants.JSONDataType.INT);
					mShadowConnection.registerProperty(deviceProperty3);

					mDevicePropertyList.add(deviceProperty1);
					mDevicePropertyList.add(deviceProperty2);
					mDevicePropertyList.add(deviceProperty3);

					mShadowConnection.update(mDevicePropertyList, null);
				}
				if (pubCount == 4) {
					mShadowConnection.get(null);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mShadowConnection.disConnect(null);
	}

	public static class callback extends TXShadowActionCallBack{
		/**
	     * 文档请求响应的回调接口
	     *
	     * @param type 文档操作方式, get/update/delete
	     * @param result 请求响应结果, 0: 成功；非0：失败
	     * @param jsonDocument   云端返回的json文档
	     */
	    public void onRequestCallback(String type, int result, String jsonDocument) {
			System.out.println("onRequestCallback" + jsonDocument);
	    }

	    /**
	     * 设备属性更新回调接口
	     *
	     * @param propertyJSONDocument 从云端收到的原始设备属性json文档
	     * @param propertyList   更新后的设备属性集
	     */
	    public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends DeviceProperty> propertyList) {
			System.out.println("onDevicePropertyCallback " +propertyList);
	    }


	    /**
	     * 收到来自云端的消息
	     *
	     * @param topic   主题名称
	     * @param message 消息内容
	     */
	    public void onMessageReceived(String topic, MqttMessage message) {
	    	System.out.println(topic + " arrived , message is " + message.toString());
	    }


	    /**
	     * 发布消息完成回调
	     *
	     * @param status        Status.OK: 发布消息成功； Status.ERROR: 发布消息失败
	     * @param token         消息token，包含消息内容结构体
	     * @param userContext   用户上下文
	     * @param msg           详细信息
	     */
	    public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String msg) {
	    	System.out.println("Onpublish " + status + msg);
	    }

	    /**
	     * 订阅主题完成回调
	     *
	     * @param status           Status.OK: 订阅成功； Status.ERROR: 订阅失败
	     * @param token            消息token，包含消息内容结构体
	     * @param userContext      用户上下文
	     * @param msg              详细信息
	     */
	    public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
	    	System.out.println("OnSubscribe " + status + msg);
	    	MqttMessage message = new MqttMessage();
			// 这里添加获取到的数据
			message.setPayload(("hello123").getBytes());
			message.setQos(1);
			mShadowConnection.publish(testTopicString, message, null);
	    }

	    /**
	     * 取消订阅主题完成回调
	     *
	     * @param status           Status.OK: 取消订阅成功； Status.ERROR: 取消订阅失败
	     * @param token            消息token，包含消息内容结构体
	     * @param userContext      用户上下文
	     * @param msg              详细信息
	     */
	    public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
	    }
		
	    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
	    	System.out.println("Connect Completed");
	    	mShadowConnection.subcribe(testTopicString, 1, null);
		}

		public void onConnectionLost(Throwable cause) {
		}
	}
}
