package com.tencent.iot.hub.device.java.core.gateway;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class GatewaySampleTest {

	private static final Logger LOG = LoggerFactory.getLogger(GatewaySampleTest.class);

	private static final String TAG = GatewaySampleTest.class.getSimpleName();

	private static String path2Store = System.getProperty("user.dir");

	private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546

	private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";

	private static String mProductID = BuildConfig.TESTGATEWAYSAMPLE_PRODUCT_ID;
	private static String mDevName = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_NAME;
	private static String mDevPSK = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_PSK;
	private static String mSubProductID = BuildConfig.TESTGATEWAYSAMPLE_SUB_PRODUCT_ID;
	private static String mSubDevName = BuildConfig.TESTGATEWAYSAMPLE_SUB_DEV_NAME;
	private static String mSubDevPsk = BuildConfig.TESTGATEWAYSAMPLE_SUB_DEV_PSK;
	private static String mTestTopic = BuildConfig.TESTGATEWAYSAMPLE_TEST_TOPIC;
	private static String mCertFilePath = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
	private static String mPrivKeyFilePath = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
	private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
	private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String
	private static JSONObject jsonObject = new JSONObject();

	private static TXGatewayConnection mqttconnection;
	private static MqttConnectOptions options;
	
	static {
		Loggor.setLogger(LOG);
	}

	private static void connect() {

		String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";

		options = new MqttConnectOptions();
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

		mqttconnection = new TXGatewayConnection(mProductID, mDevName, mDevPSK, new callBack());
		mqttconnection.setSubDevName(mSubDevName);
		mqttconnection.setSubDevProductKey(mSubDevPsk);
		mqttconnection.setSubProductID(mSubProductID);
		mqttconnection.connect(options, null);
	}

	private static void gatewaySubdevOnline() {
		mqttconnection.gatewaySubdevOnline(mSubProductID, mSubDevName);
	}

	private static void gatewaySubdevOffline() {
		mqttconnection.gatewaySubdevOffline(mSubProductID, mSubDevName);//切换子设备下线
	}

	private static void disconnect() {
		mqttconnection.disConnect(null);
	}

	private static void subscribeTopic() {
		mqttconnection.subscribe(mTestTopic, 1, null);
	}

	private static void unSubscribeTopic() {
		mqttconnection.unSubscribe(mTestTopic, null);
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
		// 温度信息
		data.put("temperature", "25");
		// MQTT消息
		MqttMessage message = new MqttMessage();

		JSONObject jsonObject = new JSONObject();
		try {
			for (Map.Entry<String, String> entrys : data.entrySet()) {
				jsonObject.put(entrys.getKey(), entrys.getValue());
			}
		} catch (JSONException e) {
			Loggor.error(TAG, e.getMessage() + "pack json data failed!");
		}
		message.setQos(TXMqttConstants.QOS1);
		message.setPayload(jsonObject.toString().getBytes());

		Loggor.debug(TAG, "pub topic " + mTestTopic + message);
		// 发布主题
		mqttconnection.publish(mTestTopic, message, null);
	}

	private static void setSubDevBinded() {
		mqttconnection.gatewayBindSubdev(mSubProductID, mSubDevName, mSubDevPsk);
	}

	private static void setSubDevUnbinded() {
		mqttconnection.gatewayUnbindSubdev(mSubProductID, mSubDevName);
	}

	private static void checkSubdevRelation() {
		mqttconnection.getGatewaySubdevRealtion();
	}

	public static class callBack extends TXMqttActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			String userContextInfo = "";

			String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
					status.name(), reconnect, userContextInfo, msg);
			Loggor.info(TAG, logInfo);
			unlock();
		}

		private TXOTACallBack oTACallBack = new TXOTACallBack() {

			@Override
			public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
				Loggor.error(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
			}

			@Override
			public boolean onLastestFirmwareReady(String url, String md5, String version) {
				Loggor.error(TAG, "onLastestFirmwareReady url=" + url + " version " + version);
				return false; // false 自动触发下载升级文件  true 需要手动触发下载升级文件
			}

			@Override
			public void onDownloadProgress(int percent, String version) {
				Loggor.error(TAG, "onDownloadProgress:" + percent);
			}

			@Override
			public void onDownloadCompleted(String outputFile, String version) {
				Loggor.error(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

				mqttconnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
			}

			@Override
			public void onDownloadFailure(int errCode, String version) {
				Loggor.error(TAG, "onDownloadFailure:" + errCode);

				mqttconnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
			}
		};

		@Override
		public void onConnectionLost(Throwable cause) {
			String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
			Loggor.info(TAG, logInfo);

		}

		@Override
		public void onDisconnectCompleted(Status status, Object userContext, String msg) {
			String userContextInfo = "";

			String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
			Loggor.info(TAG, logInfo);
		}

		@Override
		public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
			Loggor.debug(TAG, logInfo);
		}

		@Override
		public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			if (Status.ERROR == status) {
				Loggor.error(TAG, logInfo);
			} else {
				Loggor.debug(TAG, logInfo);
			}
		}

		@Override
		public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			Loggor.debug(TAG, logInfo);
		}

		@Override
		public void onMessageReceived(final String topic, final MqttMessage message) {
			String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
			Loggor.debug(TAG, logInfo);
			if (message.toString().contains("\"type\":\"online\"") && message.toString().contains(mSubDevName)) {
				subdevOnlineSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"offline\"") && message.toString().contains(mSubDevName)) {
				subdevOfflineSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"unbind\"") && message.toString().contains(mSubDevName)) {
				subDevUnbindedSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"bind\"") && message.toString().contains(mSubDevName)) {
				subDevBindedSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"describe_sub_devices\"")) {
				checkSubdevRelationSuccess = true;
				unlock();
			}
		}
	}

	/** ============================================================================== Unit Test ============================================================================== **/

	private static final int COUNT = 1;
	private static final int TIMEOUT = 3000;
	private static CountDownLatch latch = new CountDownLatch(COUNT);

	private static volatile boolean subDevUnbindedSuccess = false;
	private static volatile boolean subDevBindedSuccess = false;
	private static volatile boolean subdevOnlineSuccess = false;
	private static volatile boolean checkSubdevRelationSuccess = false;
	private static volatile boolean subdevOfflineSuccess = false;

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
	public void testGatewayConnect() {
		// Loggor.saveLogs("hub/hub-device-java.log"); //保存日志到文件
		connect();
		lock();
		assertSame(mqttconnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
		Loggor.debug(TAG, "after connect");

		setSubDevUnbinded();
		lock();
		assertTrue(subDevUnbindedSuccess);
		Loggor.debug(TAG, "after setSubDevUnbinded");

		setSubDevBinded();
		lock();
		assertTrue(subDevBindedSuccess);
		Loggor.debug(TAG, "after setSubDevBinded");

		gatewaySubdevOnline();
		lock();
		assertTrue(subdevOnlineSuccess);
		Loggor.debug(TAG, "after gatewaySubdevOnline");

		checkSubdevRelation();
		lock();
		assertTrue(checkSubdevRelationSuccess);
		Loggor.debug(TAG, "after checkSubdevRelation");

		gatewaySubdevOffline();
		lock();
		assertTrue(subdevOfflineSuccess);
		Loggor.debug(TAG, "after gatewaySubdevOffline");
	}
}
