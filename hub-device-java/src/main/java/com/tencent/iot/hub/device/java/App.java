package com.qcloud.iot;

import java.util.Arrays;
import java.util.HashMap;
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

import com.qcloud.iot.core.common.Status;
import com.qcloud.iot.core.mqtt.TXMqttActionCallBack;
import com.qcloud.iot.core.mqtt.TXMqttConnection;
import com.qcloud.iot.core.util.AsymcSslUtils;
import com.qcloud.iot.main.mqtt.MQTTSample;
import com.qcloud.iot.main.shadow.SelfMqttActionCallBack;

/**
 * Hello world!
 *
 */
public class App {

	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	private static String mBrokerURL = "tcp://iotcloud-mqtt.gz.tencentdevices.com:1883";

	private static String mProductID = "YOUR_PRODUCT_ID";
	private static String mDevName = "YOUR_DEVICE_NAME";
	private static String mDevPSK = "YOUR_DEV_PSK";
	private static String mSubProductID = "YOUR_SUB_PRODUCT_ID";
	private static String mSubDevName = "YOUR_SUB_DEV_NAME";
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
		System.out.println("qqqqqqqqqqqqqqqqqqq");
//		mMQTTSample.publishTopic("", jsonObject);
		System.out.println("qqqqqqeeeeeeqqqqqq");
		options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(60);
		options.setAutomaticReconnect(true);
		//客户端证书文件名  mDevPSK是设备秘钥
		
		if (mDevPSK != null) {
		//	options.setSocketFactory(AsymcSslUtils.getSocketFactory());
		} else {
			options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mCertFilePath, workDir + mPrivKeyFilePath));
		}
		mqttconnection = new TXMqttConnection(mProductID, mDevName, mDevPSK, new callBack());
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

	public static class callBack extends TXMqttActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			// TODO Auto-generated method stub
			String topic = String.format("%s/%s/%s", mProductID, mDevName,"data");
			System.out.println("ffffffffffff" + status.toString());
			mqttconnection.subscribe(topic, 1, null);
		}

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

			System.out.println("topic = " + topic);
			if(mqttconnection != null) {
				mqttconnection.publish(topic, message, null);
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

			System.out.println("topic = " + topic);
			if(mqttconnection != null) {
				mqttconnection.publish(topic, message, null);
			}
        }
	}
}
