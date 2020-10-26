package com.tencent.iot.hub.device.java;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketActionCallback;
import com.tencent.iot.hub.device.java.core.mqtt.TXWebSocketManager;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;

import javax.net.SocketFactory;

/**
 * Hello world!
 *
 */
public class App {

	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	private static String path2Store = System.getProperty("user.dir");

	private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546

	private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";

	private static String mProductID = "YOUR_PRODUCT_ID";
	private static String mDevName = "YOUR_DEVICE_NAME";
	private static String mDevPSK = "YOUR_DEV_PSK";
	private static String mSubProductID = "YOUR_SUB_PRODUCT_ID";
	private static String mSubDevName = "YOUR_SUB_DEV_NAME";
	private static String mSubDevProductKey = "YOUR_SUB_DEV_PSK";
	private static String mTestTopic = "YOUR_TEST_TOPIC";
	private static String mCertFilePath = null;
	private static String mPrivKeyFilePath = null;
	private static JSONObject jsonObject = new JSONObject();
	private static int pubCount = 0;
	private static final int testCnt = 1000;

	private static TXMqttConnection mqttconnection;
	private static MqttConnectOptions options;
	//	private static final TEST = "{\"action\":\"come_home\",\"targetDevice\":\""+mDevName +"  \"}";
	
	private static void dbgPrint(String s) {
		System.out.println(s);
	}
	
	
	public static void main(String[] args) {
//		websocketConnect();
//		websocketdisconnect();
		LogManager.resetConfiguration();
		LOG.isDebugEnabled();
		PropertyConfigurator.configure(App.class.getResource("/log4j.properties"));

		//MQTTSample mMQTTSample = new MQTTSample(new SelfMqttActionCallBack(), mBrokerURL, mProductID, mDevName, mDevPSK,
			//	mSubProductID, mSubDevName, mTestTopic);
		dbgPrint("mqttSample created\n");

		String workDir = System.getProperty("user.dir") + "/";


		//mMQTTSample.connect();

//		dbgPrint("mqttSample connected!");
//
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//		mMQTTSample.subscribeTopic(mTestTopic);
//
//		mMQTTSample.setSubdevOnline();
		try {
			jsonObject.put("tenant_id", "test");
			jsonObject.put("hubId", "test");
			JSONObject jsonObject2 = new JSONObject();
			jsonObject2.put("serial", "test");
			jsonObject2.put("hubId", "test");
			jsonObject2.put("deviceType", "test");
			jsonObject2.put("deviceId", "test");
			jsonObject2.put("data", "data");
			jsonObject.put("data", jsonObject2);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


//		mMQTTSample.publishTopic("", jsonObject);
//		mMQTTSample.publishTopic("", jsonObject);
		System.out.println("qqqqqqeeeeeeqqqqqq");
		options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(60);
		options.setAutomaticReconnect(true);
		//客户端证书文件名  mDevPSK是设备秘钥

		if (mDevPSK != null) {
			options.setSocketFactory(AsymcSslUtils.getSocketFactory());
		} else {
			options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
		}
		mqttconnection = new TXMqttConnection(mProductID, mDevName, mDevPSK, new callBack());
		mqttconnection.setSubDevName(mSubDevName);
		mqttconnection.setSubDevProductKey(mSubDevProductKey);
		mqttconnection.setSubProductID(mSubProductID);
		mqttconnection.connect(options, null);
		try {
			while(pubCount < testCnt) {
				Thread.sleep(20000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mqttconnection.disConnect(null);
	}

	private static void websocketdisconnect() {
		try {
			TXWebSocketManager.getInstance().getClient(mProductID, mDevName).disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	private static void websocketConnect() {

		SocketFactory socketFactory = null;
		if (mDevPSK != null) {
			socketFactory = AsymcSslUtils.getSocketFactory();
		} else {
			String workDir = System.getProperty("user.dir") + "/";
			socketFactory = AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath);
		}

		TXWebSocketManager.getInstance().getClient(mProductID, mDevName).setSecretKey(mDevPSK, socketFactory);
		try {
			TXWebSocketManager.getInstance().getClient(mProductID, mDevName).setTXWebSocketActionCallback(new TXWebSocketActionCallback() {

				@Override
				public void onConnected() {
					System.out.println("onConnected " + TXWebSocketManager.getInstance().getClient(mProductID, mDevName).getConnectionState());
				}

				@Override
				public void onMessageArrived(String topic, MqttMessage message) {
					System.out.println("onMessageArrived topic=" + topic);
				}

				@Override
				public void onConnectionLost(Throwable cause) {
					System.out.println("onConnectionLost" + TXWebSocketManager.getInstance().getClient(mProductID, mDevName).getConnectionState());
				}

				@Override
				public void onDisconnected() {
					System.out.println("onDisconnected" + TXWebSocketManager.getInstance().getClient(mProductID, mDevName).getConnectionState());
				}
			});
			TXWebSocketManager.getInstance().getClient(mProductID, mDevName).connect();
		} catch (MqttException e) {
			e.printStackTrace();
			System.out.println("MqttException " + e.toString());
		}
	}

	public static class callBack extends TXMqttActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			// TODO Auto-generated method stub
			String topic = String.format("%s/%s/%s", mProductID, mDevName,"data");
			System.out.println("ffffffffffff status " + status.toString());
			System.out.println("ffffffffffff msg " + msg);
			mqttconnection.subscribe(topic, 1, null);

			// 订阅网关操作的结果
			String gwTopic = GW_OPERATION_RES_PREFIX + mProductID + "/" + mDevName;
			mqttconnection.subscribe(gwTopic, 1, "Subscribe GATEWAY result topic");

			// 关注远程配置的回调
			mqttconnection.concernConfig();

			// 查询网关的拓扑关系
			mqttconnection.gatewayGetSubdevRelation();
			mqttconnection.getRemoteConfig();
			System.out.println("path2Store=" + path2Store);
			mqttconnection.initOTA(path2Store, oTACallBack);
		}

		private TXOTACallBack oTACallBack = new TXOTACallBack() {

			@Override
			public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {

			}

			@Override
			public boolean onLastestFirmwareReady(String url, String md5, String version) {
				System.out.println("onLastestFirmwareReady url=" + url + " version " + version);
				mqttconnection.gatewayDownSubdevApp(url, path2Store + "/" + md5, md5, version);
				return true; // false 自动触发下载升级文件  true 需要手动触发下载升级文件
			}

			@Override
			public void onDownloadProgress(int percent, String version) {
				mqttconnection.gatewaySubdevReportProgress(percent, version);
			}

			@Override
			public void onDownloadCompleted(String outputFile, String version) {
				mqttconnection.gatewaySubdevReportStart(version);
				mqttconnection.gatewaySubdevReportSuccess(version);
			}

			@Override
			public void onDownloadFailure(int errCode, String version) {
				mqttconnection.gatewaySubdevReportFail(errCode, "", version);
			}
		};

		@Override
		public void onConnectionLost(Throwable cause) {
			// TODO Auto-generated method stub
			LOG.info("onConnectionLost1111111111111111111111");

		}

		@Override
		public void onDisconnectCompleted(Status status, Object userContext, String msg) {
			// TODO Auto-generated method stub
			LOG.info("onDisconnectCompleted111111111111111");
		}
		
		@Override
		public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],   errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()),  errMsg);
            dbgPrint(logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";

            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            dbgPrint(logInfo);
            
			MqttMessage message = new MqttMessage();
			// 这里添加获取到的数据
			message.setPayload(jsonObject.toString().getBytes());
			message.setQos(1);
			String topic = String.format("%s/%s/%s", mProductID, mDevName,"data");

			if(mqttconnection != null) {
//				mqttconnection.publish(topic, message, null);
				System.out.println("000000000000000000000000000");
//				mqttconnection.gatewayBindSubdev(mSubProductID, mSubDevName, mSubDevProductKey);
//				mqttconnection.gatewayUnbindSubdev(mSubProductID, mSubDevName);


				for (String topicEls : asyncActionToken.getTopics()) {
					if (topicEls.startsWith("$ota/update/")) {
						mqttconnection.gatewaySubdevReportVer("0.0");
					}
				}

			}
        }
        
        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
            dbgPrint(logInfo);
			MqttMessage msg = new MqttMessage();
			pubCount += 1;
			
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (pubCount > testCnt) {
				return;
			}

			// 这里添加获取到的数据
			msg.setPayload(jsonObject.toString().getBytes());
			msg.setQos(1);

			if(mqttconnection != null) {
				mqttconnection.publish(topic, message, null);
			}
        }
	}
}
