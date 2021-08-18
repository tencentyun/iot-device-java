package com.tencent.iot.hub.device.android;

import android.app.AlarmManager;
import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.hub.device.android.core.gateway.TXGatewayConnection;
import com.tencent.iot.hub.device.android.core.util.TXLogImpl;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hub.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class GatewaySampleTest {

	private static final String TAG = GatewaySampleTest.class.getSimpleName();
	private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
	private static String mProductID = BuildConfig.TESTGATEWAYSAMPLE_PRODUCT_ID;
	private static String mDevName = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_NAME;
	private static String mDevPSK  = BuildConfig.TESTGATEWAYSAMPLE_DEVICE_PSK; //若使用证书验证，设为null
	private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
	private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
	private static AtomicInteger requestID = new AtomicInteger(0);

	private static String mSubDev1ProductId = BuildConfig.TESTGATEWAYSAMPLE_SUB_PRODUCT_ID;
	private static String mSubDev1DeviceName = BuildConfig.TESTGATEWAYSAMPLE_SUB_DEV_NAME;
	private static String mSubDev1DevicePSK  = BuildConfig.TESTGATEWAYSAMPLE_SUB_DEV_PSK;

	TXGatewayConnection mConnection = null;

	@Mock
	Context context;
	@Mock
	AlarmManager alarmManager;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);//create all @Mock objetcs

		TXLogImpl.init(InstrumentationRegistry.getInstrumentation().getTargetContext());

		doReturn(alarmManager).when(context).getSystemService(Context.ALARM_SERVICE);

		mConnection = new TXGatewayConnection(context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, false, null,
				new SelfMqttActionCallBack());
	}


	private void gatewayConnect() {
		//初始化连接
		MqttConnectOptions options = new MqttConnectOptions();
		options.setConnectionTimeout(8);
		options.setKeepAliveInterval(240);
		options.setAutomaticReconnect(true);

		if (mDevPSK != null && mDevPSK.length() != 0) {
			Log.i(TAG, "Using PSK");
		} else {
			Log.i(TAG, "Using cert assets file");
			String workDir = System.getProperty("user.dir") + "/hub/hub-device-java/src/test/resources/";
			options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mDevCert, workDir + mDevPriv));
		}

		MQTTRequest mqttRequest = new MQTTRequest("connect", requestID.getAndIncrement());
		mConnection.connect(options, mqttRequest);

		DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
		bufferOptions.setBufferEnabled(true);
		bufferOptions.setBufferSize(1024);
		bufferOptions.setDeleteOldestMessages(true);
		mConnection.setBufferOpts(bufferOptions);
	}

	private void gatewayDisconnect() {
		MQTTRequest mqttRequest = new MQTTRequest("disconnect", requestID.getAndIncrement());
		mConnection.disConnect(mqttRequest);
	}

	private void gatewayBindSubdev() {
		mConnection.gatewayBindSubdev(mSubDev1ProductId, mSubDev1DeviceName, mSubDev1DevicePSK);
	}

	private void gatewayUnbindSubdev() {
		mConnection.gatewayUnbindSubdev(mSubDev1ProductId, mSubDev1DeviceName);
	}

	private void gatewayOnlineSubDev() {
		mConnection.gatewaySubdevOnline(mSubDev1ProductId, mSubDev1DeviceName);
	}

	private void gatewayOfflineSubDev() {
		mConnection.gatewaySubdevOffline(mSubDev1ProductId, mSubDev1DeviceName);
	}
	/**
	 * 实现TXMqttActionCallBack回调接口
	 */
	private class SelfMqttActionCallBack extends TXMqttActionCallBack {

		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
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
			String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
			cause.printStackTrace();
			Log.i(TAG, logInfo);
		}

		@Override
		public void onDisconnectCompleted(Status status, Object userContext, String msg) {
			String userContextInfo = "";
			if (userContext instanceof MQTTRequest) {
				userContextInfo = userContext.toString();
			}
			String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
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
		}

		@Override
		public void onMessageReceived(final String topic, final MqttMessage message) {
			String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
			Log.i(TAG, logInfo);
			if (message.toString().contains("\"type\":\"online\"") && message.toString().contains(mSubDev1DeviceName)) {
				gatewayOnlineSubDevSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"offline\"") && message.toString().contains(mSubDev1DeviceName)) {
				gatewayOfflineSubDevSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"unbind\"") && message.toString().contains(mSubDev1DeviceName)) {
				gatewayUnbindSubdevSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"bind\"") && message.toString().contains(mSubDev1DeviceName)) {
				gatewayBindSubdevSuccess = true;
				unlock();
			} else if (message.toString().contains("\"type\":\"describe_sub_devices\"")) {

			}
		}
	}

	/** ============================================================================== Unit Test ============================================================================== **/

	private static final int COUNT = 1;
	private static final int TIMEOUT = 3000;
	private static CountDownLatch latch = new CountDownLatch(COUNT);

	private static boolean gatewayBindSubdevSuccess = false;
	private static boolean gatewayOnlineSubDevSuccess = false;
	private static boolean gatewayUnbindSubdevSuccess = false;
	private static boolean gatewayOfflineSubDevSuccess = false;

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

		gatewayConnect();
		lock();
		assertSame(mConnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
		Log.d(TAG, "after connect");

		gatewayUnbindSubdev();
		lock();
		assertTrue(gatewayUnbindSubdevSuccess);
		Loggor.debug(TAG, "after setSubDevUnbinded");

		gatewayBindSubdev();
		lock();
		assertTrue(gatewayBindSubdevSuccess);
		Loggor.debug(TAG, "after setSubDevBinded");

		gatewayOnlineSubDev();
		lock();
		assertTrue(gatewayOnlineSubDevSuccess);
		Loggor.debug(TAG, "after gatewaySubdevOnline");

		gatewayOfflineSubDev();
		lock();
		assertTrue(gatewayOfflineSubDevSuccess);
		Loggor.debug(TAG, "after gatewaySubdevOffline");

		gatewayDisconnect();
		lock();
		Log.d(TAG, "after disconnect");
		assertSame(mConnection.getConnectStatus(), TXMqttConstants.ConnectStatus.kDisconnected);
	}
}