package com.tencent.iot.hub.device.java.core.shadow;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TestMqttSample;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import main.mqtt.MQTTRequest;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;

/**
 * Device shadow sample
 */
public class TestShadowSample {
	private static final Logger LOG = LoggerFactory.getLogger(TestShadowSample.class);
	private static TXShadowConnection mShadowConnection;

	private static String mProductID = BuildConfig.TESTSHADOWSAMPLE_PRODUCT_ID;
	private static String mDevName = BuildConfig.TESTSHADOWSAMPLE_DEVICE_NAME;
	private static String mDevPSK = BuildConfig.TESTSHADOWSAMPLE_DEVICE_PSK;
	private static String mTestTopic = mProductID + "/" + mDevName + "/data";
	private static String mCertFilePath = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
	private static String mPrivKeyFilePath = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
	private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
	private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String

	private static AtomicInteger mUpdateCount = new AtomicInteger(0);

	private static AtomicInteger mTemperatureDesire = new AtomicInteger(20);

	//请求ID
	private static AtomicInteger requestID = new AtomicInteger(0);

	//设备属性集（该变量必须为全局变量）
	private static List<DeviceProperty> mDevicePropertyList = new ArrayList<>();

	private static void connect() {
		try {
			Thread.sleep(2000);
			String workDir = System.getProperty("user.dir") + "/";

			// init connection
			MqttConnectOptions options = new MqttConnectOptions();
			options.setConnectionTimeout(8);
			options.setKeepAliveInterval(60);
			options.setAutomaticReconnect(true);
			//客户端证书文件名  mDevPSK是设备秘钥

			if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
				LOG.info("Using cert stream " + mDevPriv + "  " + mDevCert);
				options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
			} else if (mDevPSK != null && mDevPSK.length() != 0){
				LOG.info("Using PSK");

			} else {
				LOG.info("Using cert assets file");
				options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
			}
			mShadowConnection = new TXShadowConnection(mProductID, mDevName, mDevPSK, new callback());
			mShadowConnection.connect(options, null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void closeConnect() {
		try {
			Thread.sleep(2000);
			mShadowConnection.disConnect(null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void registerProperty() {
		try {
			Thread.sleep(2000);
			DeviceProperty deviceProperty1 = new DeviceProperty();
			deviceProperty1.key("updateCount").data(String.valueOf(mUpdateCount.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
			mShadowConnection.registerProperty(deviceProperty1);

			DeviceProperty deviceProperty2 = new DeviceProperty();
			deviceProperty2.key("temperatureDesire").data(String.valueOf(mTemperatureDesire.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
			mShadowConnection.registerProperty(deviceProperty2);

			mDevicePropertyList.add(deviceProperty1);
			mDevicePropertyList.add(deviceProperty2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void update() {
		try {
			while(true) {
				Thread.sleep(10000);

				for (DeviceProperty deviceProperty : mDevicePropertyList) {
					if ("updateCount".equals(deviceProperty.mKey)) {
						deviceProperty.data(String.valueOf(mUpdateCount.getAndIncrement()));
					} else if ("temperatureDesire".equals(deviceProperty.mKey)) {
						deviceProperty.data(String.valueOf(mTemperatureDesire.getAndIncrement()));
					}
				}

				LOG.info("update device property");
				mShadowConnection.update(mDevicePropertyList, null);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void getDeviceDocument() {
		try {
			Thread.sleep(2000);
			mShadowConnection.get(null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void subscribeTopic() {
		try {
			Thread.sleep(2000);
			// QOS等级
			int qos = TXMqttConstants.QOS1;
			// 用户上下文（请求实例）
			MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
			LOG.debug("Start to subscribe" + mTestTopic);
			// 调用TXShadowConnection的subscribe方法订阅主题
			mShadowConnection.subcribe(mTestTopic, qos, mqttRequest);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void unSubscribeTopic() {
		try {
			Thread.sleep(2000);
			// 用户上下文（请求实例）
			MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
			LOG.debug("Start to unSubscribe" + mTestTopic);
			// 取消订阅主题
			mShadowConnection.unSubscribe(mTestTopic, mqttRequest);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void publishTopic() {
		try {
			Thread.sleep(2000);
			// 要发布的数据
			Map<String, String> data = new HashMap<String, String>();
			// 车辆类型
			data.put("car_type", "suv");
			// 车辆油耗
			data.put("oil_consumption", "6.6");
			// 车辆最高速度
			data.put("maximum_speed", "205");

			// MQTT消息
			MqttMessage message = new MqttMessage();

			JSONObject jsonObject = new JSONObject();
			try {
				for (Map.Entry<String, String> entrys : data.entrySet()) {
					jsonObject.put(entrys.getKey(), entrys.getValue());
				}
			} catch (JSONException e) {
				LOG.error("pack json data failed!" + e.getMessage());
			}
			message.setQos(TXMqttConstants.QOS1);
			message.setPayload(jsonObject.toString().getBytes());

			// 用户上下文（请求实例）
			MQTTRequest mqttRequest = new MQTTRequest("publishTopic", requestID.getAndIncrement());

			LOG.debug("pub topic " + mTestTopic + message);
			// 发布主题
			mShadowConnection.publish(mTestTopic, message, mqttRequest);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// init connection
		connect();

		subscribeTopic();

		publishTopic();

		unSubscribeTopic();

		getDeviceDocument();

		registerProperty();

		update();

//		closeConnect();

	}

	public static class callback extends TXShadowActionCallBack{

		/**
		 * MQTT Connect完成回调
		 *
		 * @param status        Status.OK: 连接成功； Status.ERROR: 连接失败
		 * @param reconnect     true: 重新连接      false: 首次连接
		 * @param userContext   用户上下文
		 * @param msg           连接信息
		 */
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			String userContextInfo = "";

			String logInfo = String.format("onConnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
			LOG.info(logInfo);
			unlock();
		}

		/**
		 * MQTT连接断开回调
		 *
		 * @param cause       连接断开原因
		 */
		public void onConnectionLost(Throwable cause) {
			String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
			LOG.info(logInfo);
		}
		
		/**
	     * 文档请求响应的回调接口
	     *
	     * @param type 文档操作方式, get/update/delete
	     * @param result 请求响应结果, 0: 成功；非0：失败
	     * @param document   云端返回的json文档
	     */
	    public void onRequestCallback(String type, int result, String document) {
			String logInfo = String.format("onRequestCallback, type[%s], result[%d], document[%s]", type, result, document);
			LOG.info(logInfo);
			if (type.equals("get")) {
				unlock();
			}
	    }

	    /**
	     * 设备属性更新回调接口
	     *
	     * @param propertyJSONDocument 从云端收到的原始设备属性json文档
	     * @param devicePropertyList   更新后的设备属性集
	     */
	    public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends DeviceProperty> devicePropertyList) {
			String logInfo = String.format("onDevicePropertyCallback, propertyJSONDocument[%s], deviceProperty[%s]",
					propertyJSONDocument, devicePropertyList.toString());
			LOG.info(logInfo);
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
	     * @param errMsg        详细信息
	     */
	    public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
			LOG.debug(logInfo);

			if (status == Status.OK && Arrays.toString(token.getTopics()).contains(mTestTopic)) {
				unlock();
			}
	    }

	    /**
	     * 订阅主题完成回调
	     *
	     * @param status           Status.OK: 订阅成功； Status.ERROR: 订阅失败
	     * @param asyncActionToken 消息token，包含消息内容结构体
	     * @param userContext      用户上下文
	     * @param errMsg           详细信息
	     */
	    public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			if (Status.ERROR == status) {
				LOG.error(logInfo);
			} else {
				LOG.debug(logInfo);
			}

			if (Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)) {
				unlock();
			}
	    }

	    /**
	     * 取消订阅主题完成回调
	     *
	     * @param status           Status.OK: 取消订阅成功； Status.ERROR: 取消订阅失败
	     * @param asyncActionToken 消息token，包含消息内容结构体
	     * @param userContext      用户上下文
	     * @param errMsg           详细信息
	     */
	    public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			LOG.debug(logInfo);

			if (status == Status.OK && Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)) {
				unlock();
			}
	    }
	}

	/** ============================================================================== Unit Test ============================================================================== **/

	private static Object mLock = new Object(); // 同步锁
	private static int mCount = 0; // 加解锁条件
	private static boolean mUnitTest = false;

	private static void lock() {
		synchronized (mLock) {
			mCount = 1;  // 设置锁条件
			while (mCount > 0) {
				try {
					mLock.wait(); // 等待唤醒
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void unlock() {
		if (mUnitTest) {
			synchronized (mLock) {
				mCount = 0;
				mLock.notifyAll(); // 回调执行完毕，唤醒主线程
			}
		}
	}

	@Test
	public void testShadowConnect() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		connect();
		LOG.debug("after connect");

		subscribeTopic();
		lock();
		LOG.debug("after subscribe");

		publishTopic();
		lock();
		LOG.debug("after publish");

		unSubscribeTopic();
		lock();
		LOG.debug("after unSubscribe");

		getDeviceDocument();
		lock();
		LOG.debug("after getDeviceDocument");

		registerProperty();
		LOG.debug("after registerProperty");
		assertSame(mShadowConnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
	}
}
