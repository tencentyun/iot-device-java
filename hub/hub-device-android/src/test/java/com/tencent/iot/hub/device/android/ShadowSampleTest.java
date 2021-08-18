package com.tencent.iot.hub.device.android;

import android.app.AlarmManager;
import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.hub.device.android.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.android.core.shadow.TXShadowConnection;
import com.tencent.iot.hub.device.android.core.util.TXLogImpl;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Device shadow sample
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class ShadowSampleTest {

	private static final String TAG = ShadowSampleTest.class.getSimpleName();
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

	@Mock
	Context context;
	@Mock
	AlarmManager alarmManager;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);//create all @Mock objetcs

		TXLogImpl.init(InstrumentationRegistry.getInstrumentation().getTargetContext());

		doReturn(alarmManager).when(context).getSystemService(Context.ALARM_SERVICE);

		mShadowConnection = new TXShadowConnection(context, mProductID, mDevName, mDevPSK, new ShadowActionCallBack());
	}

	private void connect() {
		String workDir = System.getProperty("user.dir") + "/";

		// init connection
		MqttConnectOptions options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(60);
		options.setAutomaticReconnect(true);
		//客户端证书文件名  mDevPSK是设备秘钥

		if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0 && !mDevCert.equals("DEVICE_CERT_CONTENT_STRING") && !mDevPriv.equals("DEVICE_PRIVATE_KEY_CONTENT_STRING")) {
			Loggor.info(TAG, "Using cert stream " + mDevPriv + "  " + mDevCert);
			options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
		} else if (mDevPSK != null && mDevPSK.length() != 0){
			Loggor.info(TAG, "Using PSK");
			// options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 hub-device-java sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
		} else {
			Loggor.info(TAG, "Using cert assets file");
			options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
		}
		mShadowConnection.connect(options, null);
	}

	private void closeConnect() {
		mShadowConnection.disConnect(null);
	}

	private void registerProperty() {
		DeviceProperty deviceProperty1 = new DeviceProperty();
		deviceProperty1.key("updateCount").data(String.valueOf(mUpdateCount.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
		mShadowConnection.registerProperty(deviceProperty1);

		DeviceProperty deviceProperty2 = new DeviceProperty();
		deviceProperty2.key("temperatureDesire").data(String.valueOf(mTemperatureDesire.getAndIncrement())).dataType(TXShadowConstants.JSONDataType.INT);
		mShadowConnection.registerProperty(deviceProperty2);

		mDevicePropertyList.add(deviceProperty1);
		mDevicePropertyList.add(deviceProperty2);
	}

	private static void update() {

		for (DeviceProperty deviceProperty : mDevicePropertyList) {
			if ("updateCount".equals(deviceProperty.mKey)) {
				deviceProperty.data(String.valueOf(mUpdateCount.getAndIncrement()));
			} else if ("temperatureDesire".equals(deviceProperty.mKey)) {
				deviceProperty.data(String.valueOf(mTemperatureDesire.getAndIncrement()));
			}
		}

		Loggor.info(TAG, "update device property");
		mShadowConnection.update(mDevicePropertyList, null);
	}

	private static void getDeviceDocument() {
		mShadowConnection.get(null);
	}

	private static void subscribeTopic() {
		// QOS等级
		int qos = TXMqttConstants.QOS1;
		// 用户上下文（请求实例）
		MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
		Loggor.debug(TAG, "Start to subscribe" + mTestTopic);
		// 调用TXShadowConnection的subscribe方法订阅主题
		mShadowConnection.subscribe(mTestTopic, qos, mqttRequest);
	}

	private static void unSubscribeTopic() {
		// 用户上下文（请求实例）
		MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
		Loggor.debug(TAG, "Start to unSubscribe" + mTestTopic);
		// 取消订阅主题
		mShadowConnection.unSubscribe(mTestTopic, mqttRequest);
	}

	private static void publishTopic() {
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
			Loggor.error(TAG, "pack json data failed!" + e.getMessage());
		}
		message.setQos(TXMqttConstants.QOS1);
		message.setPayload(jsonObject.toString().getBytes());

		// 用户上下文（请求实例）
		MQTTRequest mqttRequest = new MQTTRequest("publishTopic", requestID.getAndIncrement());

		Loggor.debug(TAG, "pub topic " + mTestTopic + message);
		// 发布主题
		mShadowConnection.publish(mTestTopic, message, mqttRequest);
	}

	private class ShadowActionCallBack extends TXShadowActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			super.onConnectCompleted(status, reconnect, userContext, msg);
			String userContextInfo = "";
			if (userContext instanceof MQTTRequest) {
				userContextInfo = userContext.toString();
			}
			String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
					status.name(), reconnect, userContextInfo, msg);
			Log.i(TAG, logInfo);
			if (!reconnect){unlock();}
		}

		@Override
		public void onConnectionLost(Throwable cause) {
			super.onConnectionLost(cause);
			String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
			Log.i(TAG, logInfo);
		}

		@Override
		public void onRequestCallback(String type, int result, String document) {
			super.onRequestCallback(type, result, document);
			String logInfo = String.format("onRequestCallback, type[%s], result[%d], document[%s]", type, result, document);
			Log.i(TAG, logInfo);
			if (type.equals("get")) {
				getDeviceDocumentSuccess = true;
				unlock();
			}
		}

		@Override
		public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends com.tencent.iot.hub.device.java.core.shadow.DeviceProperty> devicePropertyList) {
			super.onDevicePropertyCallback(propertyJSONDocument, devicePropertyList);
			String logInfo = String.format("onDevicePropertyCallback, propertyJSONDocument[%s], deviceProperty[%s]",
					propertyJSONDocument, devicePropertyList.toString());
			Log.i(TAG, logInfo);
		}

		@Override
		public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			String userContextInfo = "";
			if (userContext instanceof MQTTRequest) {
				userContextInfo = userContext.toString();
			}
			String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
			Log.i(TAG, logInfo);
			if (status == Status.OK && Arrays.toString(token.getTopics()).contains(mTestTopic)) {
				publishTopicSuccess = true;
				unlock();
			}
		}

		@Override
		public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";
			if (userContext instanceof MQTTRequest) {
				userContextInfo = userContext.toString();
			}
			String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			if (Status.ERROR == status) {
				Log.e(TAG, logInfo);
			} else {
				Log.i(TAG, logInfo);
				if (Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)){ // 订阅mTestTopic成功
					subscribeTopicSuccess = true;
					unlock();
				}
			}
		}

		@Override
		public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";
			if (userContext instanceof MQTTRequest) {
				userContextInfo = userContext.toString();
			}
			String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			Log.i(TAG, logInfo);
			if (status == Status.OK && Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)) {
				unSubscribeTopicSuccess = true;
				unlock();
			}
		}

		@Override
		public void onMessageReceived(String topic, MqttMessage message) {
			super.onMessageReceived(topic, message);
			String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
			Log.i(TAG, logInfo);
		}
	}

	/** ============================================================================== Unit Test ============================================================================== **/

	private static final int COUNT = 1;
	private static final int TIMEOUT = 3000;
	private static CountDownLatch latch = new CountDownLatch(COUNT);

	private static boolean subscribeTopicSuccess = false;
	private static boolean publishTopicSuccess = false;
	private static boolean unSubscribeTopicSuccess = false;
	private static boolean getDeviceDocumentSuccess = false;

	private static void lock() {
		latch = new CountDownLatch(COUNT);
		try {
			latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void unlock() {
		latch.countDown();// 回调执行完毕，唤醒主线程
	}

	@Test
	public void testShadowConnect() {
		// Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
		connect();
		lock();
		assertSame(mShadowConnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
		Loggor.debug(TAG, "after connect");

		subscribeTopic();
		lock();
		assertTrue(subscribeTopicSuccess);
		Loggor.debug(TAG, "after subscribe");

		publishTopic();
		lock();
		assertTrue(publishTopicSuccess);
		Loggor.debug(TAG, "after publish");

		unSubscribeTopic();
		lock();
		assertTrue(unSubscribeTopicSuccess);
		Loggor.debug(TAG, "after unSubscribe");

		getDeviceDocument();
		lock();
		assertTrue(getDeviceDocumentSuccess);
		Loggor.debug(TAG, "after getDeviceDocument");
	}
}
