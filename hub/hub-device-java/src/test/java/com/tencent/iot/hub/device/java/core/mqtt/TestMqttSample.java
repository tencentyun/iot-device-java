package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynreg;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynregCallback;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import main.mqtt.MQTTRequest;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Device Mqtt connect sample
 */
public class TestMqttSample {

	private static final Logger LOG = LoggerFactory.getLogger(TestMqttSample.class);

	private static final String TAG = "TXMQTT";

	private static String path2Store = System.getProperty("user.dir");

	private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546

	private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";

	private static String mProductID = BuildConfig.TESTMQTTSAMPLE_PRODUCT_ID;
	private static String mDevName = BuildConfig.TESTMQTTSAMPLE_DEVICE_NAME;
	private static String mDevPSK = BuildConfig.TESTMQTTSAMPLE_DEVICE_PSK;
	private static String mTestTopic = BuildConfig.TESTMQTTSAMPLE_TEST_TOPIC;
	private static String mCertFilePath = "cert_test_1_cert.crt";           // Device Cert File Name
	private static String mPrivKeyFilePath = "cert_test_1_private.key";            // Device Private Key File Name
	private static String mDevCert = "DEVICE_CERT_CONTENT_STRING";           // Cert String
	private static String mDevPriv = "DEVICE_PRIVATE_KEY_CONTENT_STRING";           // Priv String
	private static JSONObject jsonObject = new JSONObject();

	private static TXMqttConnection mqttconnection;
	private static MqttConnectOptions options;

	/**日志保存的路径*/
	private final static String mLogPath = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
	/**
	 * 请求ID
	 */
	private static AtomicInteger requestID = new AtomicInteger(0);

	private static void connect() {
		try {
			Thread.sleep(2000);
			String workDir = System.getProperty("user.dir") + "/";

			// init connection
			options = new MqttConnectOptions();
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
			mqttconnection = new TXMqttConnection(mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,true, new SelfMqttLogCallBack(), new callBack());
			mqttconnection.connect(options, null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void disconnect() {
		try {
			Thread.sleep(2000);
			mqttconnection.disConnect(null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void subscribeTopic() {
		try {
			Thread.sleep(2000);
			mqttconnection.subscribe(mTestTopic, 1, null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void unSubscribeTopic() {
		try {
			Thread.sleep(2000);
			mqttconnection.unSubscribe(mTestTopic, null);
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
				LOG.error(e.getMessage()+"pack json data failed!");
			}
			message.setQos(TXMqttConstants.QOS1);
			message.setPayload(jsonObject.toString().getBytes());

			// 用户上下文（请求实例）

			LOG.debug("pub topic " + mTestTopic + message);
			// 发布主题
			mqttconnection.publish(mTestTopic, message, null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void subscribeRRPCTopic() {
		try {
			Thread.sleep(2000);
			// 用户上下文（请求实例）
			MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
			// 订阅主题
			mqttconnection.subscribeRRPCTopic(TXMqttConstants.QOS0, mqttRequest);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void subscribeBroadCastTopic() {
		try {
			Thread.sleep(2000);
			// 用户上下文（请求实例）
			MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
			// 订阅广播主题 Topic
			mqttconnection.subscribeBroadcastTopic(TXMqttConstants.QOS1, mqttRequest);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void checkFirmware() {
		try {
			Thread.sleep(2000);
			String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
			mqttconnection.initOTA(workDir, new TXOTACallBack() {
				@Override
				public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
					LOG.error("onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
				}

				@Override
				public boolean onLastestFirmwareReady(String url, String md5, String version) {
					LOG.error("MQTTSample onLastestFirmwareReady");
					return false;
				}

				@Override
				public void onDownloadProgress(int percent, String version) {
					LOG.error("onDownloadProgress:" + percent);
				}

				@Override
				public void onDownloadCompleted(String outputFile, String version) {
					LOG.error("onDownloadCompleted:" + outputFile + ", version:" + version);

					mqttconnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
				}

				@Override
				public void onDownloadFailure(int errCode, String version) {
					LOG.error("onDownloadFailure:" + errCode);

					mqttconnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
				}
			});
			mqttconnection.reportCurrentFirmwareVersion("0.0.1");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void deviceLog() {
		try {
			Thread.sleep(2000);
			mqttconnection.mLog(TXMqttLogConstants.LEVEL_ERROR,TAG,"Error level log for test!!!");
			mqttconnection.mLog(TXMqttLogConstants.LEVEL_WARN,TAG,"Warning level log for test!!!");
			mqttconnection.mLog(TXMqttLogConstants.LEVEL_INFO,TAG,"Info level log for test!!!");
			mqttconnection.mLog(TXMqttLogConstants.LEVEL_DEBUG,TAG,"Debug level log for test!!!");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void uploadLog() {
		try {
			Thread.sleep(2000);
			mqttconnection.uploadLog();//上传设备日志
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

//		connect();
//
//		deviceLog();
//
//		uploadLog();
//
//		checkFirmware();
//
//		subscribeBroadCastTopic();
//
//		subscribeRRPCTopic();
//
//        subscribeTopic();
//
//        publishTopic();
//
//        unSubscribeTopic();
//
//        disconnect();
//
        websocketConnect();
//
//        websocketdisconnect();
	}



	private static void websocketdisconnect() {
		try {
			Thread.sleep(5000);
			TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).disconnect();
		} catch (MqttException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void websocketConnect() {

		try {
			// init connection
			MqttConnectOptions conOptions = new MqttConnectOptions();
			conOptions.setCleanSession(true);

			if (mDevPSK != null && mDevPSK.length() != 0) {
				conOptions.setSocketFactory(AsymcSslUtils.getSocketFactory());
			} else {
				String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
				conOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
			}

			conOptions.setConnectionTimeout(8);
			conOptions.setKeepAliveInterval(60);
			conOptions.setAutomaticReconnect(true);

			TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setMqttConnectOptions(conOptions);

			TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).setTXWebSocketActionCallback(new TXWebSocketActionCallback() {

				@Override
				public void onConnected() {
					LOG.debug("onConnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
					unlock();
				}

				@Override
				public void onMessageArrived(String topic, MqttMessage message) {
					LOG.debug("onMessageArrived topic=" + topic);
				}

				@Override
				public void onConnectionLost(Throwable cause) {
					LOG.debug("onConnectionLost" + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
				}

				@Override
				public void onDisconnected() {
					LOG.debug("onDisconnected" + TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState());
				}
			});
			TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).connect();
		} catch (MqttException e) {
			e.printStackTrace();
			LOG.error("MqttException " + e.toString());
		}
	}

	public static class callBack extends TXMqttActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			String userContextInfo = "";

			String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
					status.name(), reconnect, userContextInfo, msg);
			LOG.info(logInfo);
			unlock();
		}

		private TXOTACallBack oTACallBack = new TXOTACallBack() {

			@Override
			public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
				LOG.error("onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
			}

			@Override
			public boolean onLastestFirmwareReady(String url, String md5, String version) {
				LOG.error("onLastestFirmwareReady url=" + url + " version " + version);
				return false; // false 自动触发下载升级文件  true 需要手动触发下载升级文件
			}

			@Override
			public void onDownloadProgress(int percent, String version) {
				LOG.error("onDownloadProgress:" + percent);
			}

			@Override
			public void onDownloadCompleted(String outputFile, String version) {
				LOG.error("onDownloadCompleted:" + outputFile + ", version:" + version);

				mqttconnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
			}

			@Override
			public void onDownloadFailure(int errCode, String version) {
				LOG.error("onDownloadFailure:" + errCode);

				mqttconnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
			}
		};

		@Override
		public void onConnectionLost(Throwable cause) {
			String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
			LOG.info(logInfo);

		}

		@Override
		public void onDisconnectCompleted(Status status, Object userContext, String msg) {
			String userContextInfo = "";

			String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
			LOG.info(logInfo);
			unlock();
		}

		@Override
		public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
			LOG.debug(logInfo);
			if (status == Status.OK && Arrays.toString(token.getTopics()).contains(mTestTopic)) {
				unlock();
			}
		}

		@Override
		public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			if (Status.ERROR == status) {
				LOG.error(logInfo);
			} else {
				LOG.debug(logInfo);
				if (Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)) {
					unlock();
				}
			}
		}

		@Override
		public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			LOG.debug(logInfo);
			if (status == Status.OK && Arrays.toString(asyncActionToken.getTopics()).contains(mTestTopic)) {
				unlock();
			}
		}

		@Override
		public void onMessageReceived(final String topic, final MqttMessage message) {
			String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
			LOG.debug(logInfo);
		}
	}

	/**
	 * 实现TXMqttLogCallBack回调接口
	 */
	private static class SelfMqttLogCallBack extends TXMqttLogCallBack {

		@Override
		public String setSecretKey() {
			String secertKey;
			if (mDevPSK != null && mDevPSK.length() != 0) {  //密钥认证
				secertKey = mDevPSK;
				secertKey = secertKey.length() > 24 ? secertKey.substring(0,24) : secertKey;
				return secertKey;
			} else {
				BufferedReader cert;

				if (mDevCert != null && mDevCert.length() != 0) { //动态注册,从DevCert中读取
					cert = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mDevCert.getBytes(Charset.forName("utf8"))), Charset.forName("utf8")));

				} else { //证书认证，从证书文件中读取

					try {
						String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
						cert=new BufferedReader(new InputStreamReader(new FileInputStream(new File(workDir + mCertFilePath))));
					} catch (IOException e) {
						LOG.error("getSecertKey failed, cannot open CRT Files.");
						return null;
					}
				}
				//获取密钥
				try {
					if (cert.readLine().contains("-----BEGIN")) {
						secertKey = cert.readLine();
						secertKey = secertKey.length() > 24 ? secertKey.substring(0,24) : secertKey;
					} else {
						secertKey = null;
						LOG.error("Invaild CRT Files.");
					}
					cert.close();
				} catch (IOException e) {
					LOG.error("getSecertKey failed.");
					return null;
				}
			}

			return secertKey;
		}

		@Override
		public void printDebug(String message){
			LOG.debug(message);
		}

		@Override
		public boolean saveLogOffline(String log){

			String logFilePath = mLogPath + mProductID + mDevName + ".log";

			LOG.debug("Save log to %s", logFilePath);

			try {
				BufferedWriter wLog = new BufferedWriter(new FileWriter(new File(logFilePath), true));
				wLog.write(log);
				wLog.flush();
				wLog.close();
				return true;
			} catch (IOException e) {
				String logInfo = String.format("Save log to [%s] failed, check the Storage permission!", logFilePath);
				LOG.error(logInfo);
				e.printStackTrace();
				return false;
			}
		}

		@Override
		public String readOfflineLog(){

			String logFilePath = mLogPath + mProductID + mDevName + ".log";

			LOG.debug("Read log from %s", logFilePath);

			try {
				BufferedReader logReader = new BufferedReader(new FileReader(logFilePath));
				StringBuilder offlineLog = new StringBuilder();
				int data;
				while (( data = logReader.read()) != -1 ) {
					offlineLog.append((char)data);
				}
				logReader.close();
				return offlineLog.toString();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public boolean delOfflineLog(){

			String logFilePath = mLogPath + mProductID + mDevName + ".log";

			File file = new File(logFilePath);
			if (file.exists() && file.isFile()) {
				if (file.delete()) {
					return true;
				}
			}
			return false;
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
	public void testMqttConnect() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		connect();
		lock();
		LOG.debug("after connect");
		assertSame(mqttconnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
	}

	@Test
	public void testMqttDisconnect() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		connect();
		lock();
		LOG.debug("after connect");

		disconnect();
		lock();
		LOG.debug("after disconnect");

		assertSame(mqttconnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kDisconnected);
	}

	@Test
	public void testSubscribeTopic() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		connect();
		lock();
		LOG.debug("after connect");

		subscribeTopic();
		lock();
		LOG.debug("after subscribe");

		assertTrue(true);
	}

	@Test
	public void testUnSubscribeTopic() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		connect();
		lock();
		LOG.debug("after connect");

		subscribeTopic();
		lock();
		LOG.debug("after subscribe");

		unSubscribeTopic();
		lock();
		LOG.debug("after unSubscribe");

		assertTrue(true);
	}

	@Test
	public void testPublishTopic() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		connect();
		lock();
		LOG.debug("after connect");

		publishTopic();
		lock();
		LOG.debug("after publish");

		assertTrue(true);
	}

	@Test
	public void testWebsocketConnect() {
		mUnitTest = true;
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

		websocketConnect();
		lock();
		LOG.debug("after connect");

		assertSame(TXWebSocketManager.getInstance().getClient(mProductID, mDevName, mDevPSK).getConnectionState(), ConnectionState.CONNECTED);
	}
}
