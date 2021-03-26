package com.tencent.iot.hub.device.java.core.dynreg;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketActionCallback;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketManager;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;

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
import main.mqtt.MQTTRequest;

import static org.junit.Assert.assertTrue;

/**
 * Device Mqtt connect sample
 */
public class TestDynregDevSample {

	private static final Logger LOG = LoggerFactory.getLogger(TestDynregDevSample.class);

	private static final String TAG = "TXMQTT";

	private static String path2Store = System.getProperty("user.dir");

	private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546

	private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";

	private static String mProductID = BuildConfig.TESTMQTTSAMPLE_PRODUCT_ID;
	private static String mDevName = BuildConfig.TESTMQTTSAMPLE_DEVICE_NAME;
	private static String mDevPSK = BuildConfig.TESTMQTTSAMPLE_DEVICE_PSK;
	private static String mProductKey = "";//BuildConfig.TESTMQTTSAMPLE_PRODUCT_KEY;             // Used for dynamic register
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

	private static void dynReg() {
		try {
			Thread.sleep(2000);
			LOG.debug("Test Dynamic");
			TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductKey, mDevName, new SelfMqttDynregCallback());//初始化TXMqttDynreg
			if (dynreg.doDynamicRegister()) {//调起动态注册
				LOG.debug("Dynamic Register OK!");
			} else {
				LOG.error("Dynamic Register failed!");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(TestDynregDevSample.class.getResource("/log4j.properties"));

//		dynReg();

		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String workDir = System.getProperty("user.dir") + "/";

		// init connection
		options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(60);
		options.setAutomaticReconnect(true);
		//客户端证书文件名  mDevPSK是设备秘钥

		if (mDevPSK != null && mDevPSK.length() != 0){
			LOG.info("Using PSK");

		} else {
			LOG.info("Using cert stream " + mDevPriv + "  " + mDevCert);
			options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
		}
		mqttconnection = new TXMqttConnection(mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,true, new SelfMqttLogCallBack(), new callBack());
		mqttconnection.connect(options, null);

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
//        websocketConnect();
//
//        websocketdisconnect();
	}

	public static class callBack extends TXMqttActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			String userContextInfo = "";

			String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
					status.name(), reconnect, userContextInfo, msg);
			LOG.info(logInfo);
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
		}

		@Override
		public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
			LOG.debug(logInfo);
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
			}
		}

		@Override
		public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
			String userContextInfo = "";

			String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
					status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
			LOG.debug(logInfo);
		}

		@Override
		public void onMessageReceived(final String topic, final MqttMessage message) {
			String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
			LOG.debug(logInfo);
		}
	}

	/**
	 * Callback for dynamic register
	 */
	private static class SelfMqttDynregCallback extends TXMqttDynregCallback {

		@Override
		public void onGetDevicePSK(String devicePsk) {
			mDevPSK = devicePsk;
			String logInfo = String.format("Dynamic register OK! onGetDevicePSK, devicePSK[%s]", devicePsk);
			LOG.info(logInfo);
		}

		@Override
		public void onGetDeviceCert(String deviceCert, String devicePriv) {
            mDevCert = deviceCert;   //这里获取的是证书内容字符串 创建对应ssl认证时可使用options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));方式，示例中使用的是读取本地文件路径的方式。
            mDevPriv = devicePriv;   //这里获取的是秘钥内容字符串
			String logInfo = String.format("Dynamic register OK!onGetDeviceCert, deviceCert[%s] devicePriv[%s]", deviceCert, devicePriv);
			LOG.info(logInfo);
		}

		@Override
		public void onFailedDynreg(Throwable cause, String errMsg) {
			String logInfo = String.format("Dynamic register failed! onFailedDynreg, ErrMsg[%s]", cause.toString() + errMsg);
			LOG.error(logInfo);
		}

		@Override
		public void onFailedDynreg(Throwable cause) {
			String logInfo = String.format("Dynamic register failed! onFailedDynreg, ErrMsg[%s]", cause.toString());
			LOG.error(logInfo);
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

//					try {
//						String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
//						cert=new BufferedReader(new InputStreamReader(new FileInputStream(new File(workDir + mCertFilePath))));
//					} catch (IOException e) {
//						LOG.error("getSecertKey failed, cannot open CRT Files.");
						return null;
//					}
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
}
