package com.tencent.iot.hub.device.java.core.gateway;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXAlarmPingSender;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

/**
 * Created by willssong on 2018/12/25.
 */

public class TXGatewayConnection extends TXMqttConnection {
	public static final String TAG = "TXMQTT" + MQTT_SDK_VER;
	private static final Logger LOG = LoggerFactory.getLogger(TXGatewayConnection.class);
	
	private HashMap<String, TXGatewaySubdev> mSubdevs = new HashMap<String, TXGatewaySubdev>();
	private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";
	private static final String GW_OPERATION_PREFIX = "$gateway/operation/";

	/**
	 * @param context
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @param serverURI
	 *            服务器URI
	 * @param productID
	 *            产品名
	 * @param deviceName
	 *            设备名，唯一
	 * @param secretKey
	 *            密钥
	 * @param bufferOpts
	 *            发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
	 * @param clientPersistence
	 *            消息永久存储
	 * @param callBack
	 *            连接、消息发布、消息订阅回调接口
	 */
	public TXGatewayConnection(String serverURI, String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXMqttActionCallBack callBack) {
		super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
	}

	/**
	 * 使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
	 * @param context
	 * @param productID
	 * @param deviceName
	 * @param secretKey
	 * @param bufferOpts
	 * @param clientPersistence
	 * @param callBack
	 */
	public TXGatewayConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXMqttActionCallBack callBack) {
		this(null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
	}

	/**
	 *
	 * @param context
	 * @param productID
	 * @param deviceName
	 * @param secretKey
	 * @param bufferOpts
	 * @param callBack
	 */
	public TXGatewayConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
		this(productID, deviceName, secretKey, bufferOpts, null, callBack);
	}

	/**
	 *
	 * @param context
	 * @param productID
	 * @param deviceName
	 * @param secretKey
	 * @param callBack
	 */
	public TXGatewayConnection(String productID, String deviceName, String secretKey, TXMqttActionCallBack callBack) {
		this(productID, deviceName, secretKey, null, null, callBack);
	}

	public TXGatewayConnection(String srvURL, String productID, String deviceName, String secretKey,
			TXMqttActionCallBack callBack) {
		this(srvURL, productID, deviceName, secretKey, null, null, callBack);
	}

	/**
	 *
	 * @param productId
	 * @param devName
	 * @return null if not existed otherwise the subdev
	 */
	private TXGatewaySubdev findSubdev(String productId, String devName) {

		LOG.debug("{}", "The hashed information is " + mSubdevs);
		return mSubdevs.get(productId + devName);
	}

	/**
	 * remove the subdev if it is offline
	 * 
	 * @param subdev
	 * @return the operation results
	 */
	private synchronized TXGatewaySubdev removeSubdev(TXGatewaySubdev subdev) {
		return mSubdevs.remove(subdev.mProductId + subdev.mDevName);
	}

	/**
	 * remove the subdev if it is offline
	 * 
	 * @param productId
	 * @param devName
	 * @return
	 */
	private synchronized TXGatewaySubdev removeSubdev(String productId, String devName) {
		return mSubdevs.remove(productId + devName);
	}

	/**
	 * add a new subdev entry
	 * 
	 * @param dev
	 */
	private synchronized void addSubdev(TXGatewaySubdev dev) {
		mSubdevs.put(dev.mProductId + dev.mDevName, dev);
	}

	/**
	 * Get the subdev status
	 * 
	 * @param productId
	 * @param devName
	 * @return the status of subdev
	 */
	public Status getSubdevStatus(String productId, String devName) {
		TXGatewaySubdev subdev = findSubdev(productId, devName);
		if (subdev == null) {
			return Status.SUBDEV_STAT_NOT_EXIST;
		}
		return subdev.getSubdevStatus();
	}

	/**
	 * set the status of the subdev
	 * 
	 * @param productId
	 * @param devName
	 * @param stat
	 * @return the status of operation
	 */
	public Status setSubdevStatus(String productId, String devName, Status stat) {
		TXGatewaySubdev subdev = findSubdev(productId, devName);
		if (subdev == null) {
			return Status.SUBDEV_STAT_NOT_EXIST;
		}
		subdev.setSubdevStatus(stat);
		return Status.OK;
	}

	/**
	 * publish the offline message for the subdev
	 * 
	 * @param subProductID
	 * @param subDeviceName
	 * @return the result of operation
	 */
	public Status gatewaySubdevOffline(String subProductID, String subDeviceName) {
		LOG.debug("{}", "Try to find " + subProductID + " & " + subDeviceName);
		TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
		if (subdev == null) {
			LOG.debug("{}", "Cant find the subdev");
			subdev = new TXGatewaySubdev(subProductID, subDeviceName);
		}
		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

		LOG.debug("{}", "set " + subProductID + " & " + subDeviceName + " to offline");

		// format the payload
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "offline");
			JSONObject plObj = new JSONObject();
			String strDev = "[{'product_id':'" + subProductID + "','device_name':'" + subDeviceName + "'}]";
			JSONArray devs = new JSONArray(strDev);
			plObj.put("devices", devs);
			obj.put("payload", plObj);
		} catch (JSONException e) {
			return Status.ERROR;
		}
		MqttMessage message = new MqttMessage();
		message.setQos(0);
		message.setPayload(obj.toString().getBytes());
		LOG.debug("{}", "publish message " + message);

		return super.publish(topic, message, null);
	}

	public Status gatewaySubdevOnline(String subProductID, String subDeviceName) {
		TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
		if (subdev == null) {
			LOG.debug("{}", "Cant find the subdev");
			subdev = new TXGatewaySubdev(subProductID, subDeviceName);
		}
		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
		LOG.debug("{}", "set " + subProductID + " & " + subDeviceName + " to Online");
		// format the payload
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "online");
			JSONObject plObj = new JSONObject();
			String strDev = "[{'product_id':'" + subProductID + "','device_name':'" + subDeviceName + "'}]";
			JSONArray devs = new JSONArray(strDev);
			plObj.put("devices", devs);
			obj.put("payload", plObj);
		} catch (JSONException e) {
			return Status.ERROR;
		}
		addSubdev(subdev);

		MqttMessage message = new MqttMessage();
		message.setQos(0);
		message.setPayload(obj.toString().getBytes());
		System.out.println( "publish to "+topic+" message " + message);

		return super.publish(topic, message, null);
	}

	private boolean consumeGwOperationMsg(String topic, MqttMessage message) {
		if (!topic.startsWith(GW_OPERATION_RES_PREFIX)) {
			return false;
		}
		LOG.debug("{}", "got gate operation messga " + topic + message);
		String productInfo = topic.substring(GW_OPERATION_RES_PREFIX.length());
		int splitIdx = productInfo.indexOf('/');
		String productId = productInfo.substring(0, splitIdx);
		String devName = productInfo.substring(splitIdx + 1);

		TXGatewaySubdev subdev = findSubdev(productId, devName);

		// this subdev is not managed by me
		if (subdev == null) {
			return false;
		}

		try {
			byte[] payload = message.getPayload();
			JSONObject jsonObject = new JSONObject(new String(payload));

			String type = jsonObject.getString("type");
			if (type.equalsIgnoreCase("online")) {
				String res = jsonObject.getString("result");

				if (res.equals("0")) {
					subdev.setSubdevStatus(Status.SUBDEV_STAT_ONLINE);
				}

			} else if (type.equalsIgnoreCase("offline")) {
				String res = jsonObject.getString("result");

				if (res.equals("0")) {
					removeSubdev(subdev);
				}
			}

		} catch (JSONException e) {

		}

		return true;
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		LOG.debug("{}", "message received " + topic);
		if (!consumeGwOperationMsg(topic, message)) {
			super.messageArrived(topic, message);
		}
	}

	@Override
	public synchronized Status connect(MqttConnectOptions options, Object userContext) {
		if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
			LOG.info("{}", "The client is connecting. Connect return directly.");
			return Status.MQTT_CONNECT_IN_PROGRESS;
		}

		if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
			LOG.info("{}", "The client is already connected. Connect return directly.");
			return Status.OK;
		}

		this.mConnOptions = options;
		if (mConnOptions == null) {
			LOG.error("{}", "Connect options == null, will not connect.");
			return Status.PARAMETER_INVALID;
		}

		Long timestamp = System.currentTimeMillis() / 1000 + 600;
		String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;
		System.out.println("**** userNameStr is " + userNameStr + " timestamp " + timestamp);

		mConnOptions.setUserName(userNameStr);

		if (mSecretKey != null) {
			try {
				String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(),
						Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
				mConnOptions.setPassword(passWordStr.toCharArray());
			} catch (IllegalArgumentException e) {
				LOG.debug("{}", "Failed to set password");
			}
		}

		mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

		IMqttActionListener mActionListener = new IMqttActionListener() {
			public void onSuccess(IMqttToken token) {
				LOG.info("{}", "onSuccess!");
				setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
				mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(),
						"connected to " + mServerURI);
				// If the connection is established, subscribe the gateway
				// operation topic
				String gwTopic = GW_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
				int qos = TXMqttConstants.QOS1;

				subscribe(gwTopic, qos, "Subscribe GATEWAY result topic");
				LOG.debug("{}", "Connected, then subscribe the gateway result topic");
			}

			public void onFailure(IMqttToken token, Throwable exception) {
				LOG.error("{}", exception, "onFailure!");
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString());
			}
		};

		if (mMqttClient == null) {
			try {
				mPingSender = new TXAlarmPingSender();
				mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
				mMqttClient.setCallback(this);
				mMqttClient.setBufferOpts(super.bufferOpts);
				mMqttClient.setManualAcks(false);
			} catch (Exception e) {
				LOG.error("{}", "new MqttClient failed", e);
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				return Status.ERROR;
			}
		}

		try {
			LOG.info("Start connecting to {}", mServerURI);
			setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
			mMqttClient.connect(mConnOptions, userContext, mActionListener);
		} catch (Exception e) {
			LOG.error("{}", "MqttClient connect failed", e);
			setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
			return Status.ERROR;
		}

		return Status.OK;
	}
}
