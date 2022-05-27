package com.tencent.iot.hub.device.java.core.gateway;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXAlarmPingSender;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 网关连接类
 */
public class TXGatewayConnection extends TXMqttConnection {
	private static final String TAG = "TXMQTT" + MQTT_SDK_VER;
	private static final String HMAC_SHA_256 = "HmacSHA256";
	private static final Logger logger = LoggerFactory.getLogger(TXGatewayConnection.class);

	private HashMap<String, TXGatewaySubdev> mSubdevs = new HashMap<String, TXGatewaySubdev>();
	private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";
	private static final String GW_OPERATION_PREFIX = "$gateway/operation/";
	private static final String PRODUCT_CONFIG_PREFIX = "$config/operation/result/";

	static { Loggor.setLogger(logger); }

	/**
	 * 构造函数
	 *
	 * @param serverURI 服务器 URI
	 * @param productID 网关产品 ID
	 * @param deviceName 网关设备名，唯一
	 * @param secretKey 网关密钥
	 * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
	 * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
	 * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
	 */
	public TXGatewayConnection(String serverURI, String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXMqttActionCallBack callBack) {
		super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
	}

	/**
	 * 构造函数，使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
	 *
	 * @param productID 网关产品 ID
	 * @param deviceName 网关设备名，唯一
	 * @param secretKey 网关密钥
	 * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
	 * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
	 * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
	 */
	public TXGatewayConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXMqttActionCallBack callBack) {
		this(null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
	}

	/**
	 * 构造函数
	 *
	 * @param productID 网关产品 ID
	 * @param deviceName 网关设备名，唯一
	 * @param secretKey 网关密钥
	 * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
	 * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
	 */
	public TXGatewayConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
		this(productID, deviceName, secretKey, bufferOpts, null, callBack);
	}

	/**
	 * 构造函数
	 *
	 * @param productID 网关产品 ID
	 * @param deviceName 网关设备名，唯一
	 * @param secretKey 网关密钥
	 * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
	 */
	public TXGatewayConnection(String productID, String deviceName, String secretKey, TXMqttActionCallBack callBack) {
		this(productID, deviceName, secretKey, null, null, callBack);
	}

	/**
	 * 构造函数
	 *
	 * @param srvURL 服务器 URI
	 * @param productID 网关产品 ID
	 * @param deviceName 网关设备名，唯一
	 * @param secretKey 网关密钥
	 * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
	 */
	public TXGatewayConnection(String srvURL, String productID, String deviceName, String secretKey,
			TXMqttActionCallBack callBack) {
		this(srvURL, productID, deviceName, secretKey, null, null, callBack);
	}

	/**
	 * 查找子设备
	 *
	 * @param productId 子产品 ID
	 * @param devName 子设备名
	 * @return {@link TXGatewaySubdev}
	 */
	private TXGatewaySubdev findSubdev(String productId, String devName) {

		Loggor.debug(TAG, "The hashed information is " + mSubdevs);
		return mSubdevs.get(productId + devName);
	}

	/**
	 * 当设备离线时，移除子设备
	 *
	 * @param subdev 子设备 {@link TXGatewaySubdev}
	 * @return 操作结果 {@link TXGatewaySubdev}
	 */
	private synchronized TXGatewaySubdev removeSubdev(TXGatewaySubdev subdev) {
		return mSubdevs.remove(subdev.mProductId + subdev.mDevName);
	}

	/**
	 * 当设备离线时，移除子设备
	 *
	 * @param productId 子产品 ID
	 * @param devName 子设备名
	 * @return 操作结果 {@link TXGatewaySubdev}
	 */
	private synchronized TXGatewaySubdev removeSubdev(String productId, String devName) {
		return mSubdevs.remove(productId + devName);
	}

	/**
	 * 添加子设备
	 *
	 * @param dev 子设备 {@link TXGatewaySubdev}
	 */
	private synchronized void addSubdev(TXGatewaySubdev dev) {
		mSubdevs.put(dev.mProductId + dev.mDevName, dev);
	}

	/**
	 * 获取子设备状态
	 *
	 * @param productId 子产品 ID
	 * @param devName 子设备名
	 * @return 子设备状态 {@link Status}
	 */
	public Status getSubdevStatus(String productId, String devName) {
		TXGatewaySubdev subdev = findSubdev(productId, devName);
		if (subdev == null) {
			return Status.SUBDEV_STAT_NOT_EXIST;
		}
		return subdev.getSubdevStatus();
	}

	/**
	 * 设置子设备状态
	 *
	 * @param productId 子产品 ID
	 * @param devName 子设备名
	 * @param state 状态 {@link Status}
	 * @return 操作结果 {@link Status}
	 */
	public Status setSubdevStatus(String productId, String devName, Status state) {
		TXGatewaySubdev subdev = findSubdev(productId, devName);
		if (subdev == null) {
			return Status.SUBDEV_STAT_NOT_EXIST;
		}
		subdev.setSubdevStatus(state);
		return Status.OK;
	}

	/**
	 * 发布子设备离线消息
	 *
	 * @param subProductID 子产品 ID
	 * @param subDeviceName 子设备名
	 * @return 操作结果 {@link Status}
	 */
	public Status gatewaySubdevOffline(String subProductID, String subDeviceName) {
		Loggor.debug(TAG, "Try to find " + subProductID + " & " + subDeviceName);
		TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
		if (subdev == null) {
			Loggor.debug(TAG, "Cant find the subdev");
			subdev = new TXGatewaySubdev(subProductID, subDeviceName);
		}
		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

		Loggor.debug(TAG, "set " + subProductID + " & " + subDeviceName + " to offline");

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
		Loggor.debug(TAG, "publish message " + message);

		return super.publish(topic, message, null);
	}

	/**
	 * 子设备上线消息
	 *
	 * @param subProductID 子产品 ID
	 * @param subDeviceName 子设备名
	 * @return 操作结果 {@link Status}
	 */
	public Status gatewaySubdevOnline(String subProductID, String subDeviceName) {
		TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
		if (subdev == null) {
			Loggor.debug(TAG, "Cant find the subdev");
			subdev = new TXGatewaySubdev(subProductID, subDeviceName);
		}
		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
		Loggor.debug(TAG, "set " + subProductID + " & " + subDeviceName + " to Online");
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
		Loggor.debug(TAG,  "publish to "+topic+" message " + message);

		return super.publish(topic, message, null);
	}

	private static String sign(String src, String psk) {
		Mac mac;

		try {
			mac = Mac.getInstance(HMAC_SHA_256);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		String hmacSign;
		SecretKeySpec signKey = new SecretKeySpec(psk.getBytes(), HMAC_SHA_256);

		try {
			mac.init(signKey);
			byte[] rawHmac = mac.doFinal(src.getBytes());
			hmacSign = com.tencent.iot.hub.device.java.core.util.Base64.encodeToString(rawHmac, com.tencent.iot.hub.device.java.core.util.Base64.NO_WRAP);
			return hmacSign;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 网关绑定子设备
	 *
	 * @param subProductID 子产品 ID
	 * @param subDeviceName 子设备名
	 * @param psk 子设备密钥
	 * @return 操作结果 {@link Status}
	 */
	public Status gatewayBindSubdev(String subProductID, String subDeviceName, String psk) {

		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

		// format the payload
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "bind");
			JSONObject plObj = new JSONObject();
			JSONObject dev = new JSONObject();
			dev.put("product_id", subProductID);
			dev.put("device_name", subDeviceName);
			int randNum = (int) (Math.random() * 999999);
			dev.put("random", randNum);
			long timestamp = System.currentTimeMillis() / 1000;
			dev.put("timestamp", timestamp);
			dev.put("signmethod", "hmacsha256");
			dev.put("authtype", "psk");
			String text2Sgin = subProductID + subDeviceName + ";" + randNum + ";" + timestamp;
			dev.put("signature", sign(text2Sgin, psk));
			JSONArray devs = new JSONArray();
			devs.put(dev);
			plObj.put("devices", devs);
			obj.put("payload", plObj);
		} catch (JSONException e) {
			return Status.ERROR;
		}

		MqttMessage message = new MqttMessage();
		message.setQos(0);
		message.setPayload(obj.toString().getBytes());
		Loggor.debug(TAG,  "publish message " + message);

		return super.publish(topic, message, null);
	}

	/**
	 * 解绑网关子设备
	 *
	 * @param subProductID 子产品 ID
	 * @param subDeviceName 子设备名
	 * @return 操作结果 {@link Status}
	 */
	public Status gatewayUnbindSubdev(String subProductID, String subDeviceName) {

		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

		// format the payload
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "unbind");
			JSONObject plObj = new JSONObject();
			JSONObject dev = new JSONObject();
			dev.put("product_id", subProductID);
			dev.put("device_name", subDeviceName);
			JSONArray devs = new JSONArray();
			devs.put(dev);
			plObj.put("devices", devs);
			obj.put("payload", plObj);
		} catch (JSONException e) {
			return Status.ERROR;
		}

		MqttMessage message = new MqttMessage();
		message.setQos(0);
		message.setPayload(obj.toString().getBytes());
		Loggor.debug(TAG, "publish message " + message);

		return super.publish(topic, message, null);
	}

	/**
	 * 获取网关子设备拓扑关系
	 *
	 * @return 操作结果 {@link Status}
	 */
	public Status getGatewaySubdevRealtion() {
		String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "describe_sub_devices");
		} catch (JSONException e) {
			return Status.ERROR;
		}

		MqttMessage message = new MqttMessage();
		message.setQos(0);
		message.setPayload(obj.toString().getBytes());
		Loggor.debug(TAG, "publish message " + message);

		return super.publish(topic, message, null);
	}

	/**
	 * 获取远程配置
	 *
	 * @return 操作结果 {@link Status}
	 */
	public Status getRemoteConfig() {
		// format the payload
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "get");
		} catch (JSONException e) {
			return Status.ERROR;
		}

		MqttMessage message = new MqttMessage();
		// 这里添加获取到的数据
		message.setPayload(obj.toString().getBytes());
		message.setQos(1);
		String topic = String.format("$config/report/%s/%s", mProductId, mDeviceName);
		return super.publish(topic, message, null);
	}

	private boolean consumeGwOperationMsg(String topic, MqttMessage message) {
		if (!topic.startsWith(GW_OPERATION_RES_PREFIX)) {
			return false;
		}
		Loggor.debug(TAG, "got gate operation message " + topic + message);
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

	/**
	 * 关注配置变化
	 *
	 * @return 操作结果 {@link Status}
	 */
	public Status concernConfig() {
		String subscribeConfigTopic = PRODUCT_CONFIG_PREFIX + mProductId + "/" + mDeviceName;
		return this.subscribe(subscribeConfigTopic, 1, "subscribe config topic");
	}

	/**
	 * 消息到达回调
	 *
	 * @param topic 消息主题
	 * @param message 消息内容结构体
	 * @throws Exception
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		Loggor.debug(TAG, "message received " + topic);
		if (!consumeGwOperationMsg(topic, message)) {
			super.messageArrived(topic, message);
		}
	}

	/**
	 * 连接 MQTT 服务，无需设置 username 和 password，内部会自动填充
	 *
	 * @param options 连接参数
	 * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
	 * @return 操作结果 {@link Status}
	 */
	@Override
	public synchronized Status connect(MqttConnectOptions options, Object userContext) {
		if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
			Loggor.info(TAG, "The client is connecting. Connect return directly.");
			return Status.MQTT_CONNECT_IN_PROGRESS;
		}

		if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
			Loggor.info(TAG, "The client is already connected. Connect return directly.");
			return Status.OK;
		}

		this.mConnOptions = options;
		if (mConnOptions == null) {
			Loggor.error(TAG, "Connect options == null, will not connect.");
			return Status.PARAMETER_INVALID;
		}

		Long timestamp;
		if (options.isAutomaticReconnect()) {
			timestamp = (long) Integer.MAX_VALUE;
		} else {
			timestamp = System.currentTimeMillis() / 1000 + 600;
		}

		String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;
		Loggor.debug(TAG, "**** userNameStr is " + userNameStr + " timestamp " + timestamp);

		mConnOptions.setUserName(userNameStr);

		if (mSecretKey != null) {
			try {
				String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(),
						Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
				mConnOptions.setPassword(passWordStr.toCharArray());
			} catch (IllegalArgumentException e) {
				Loggor.debug(TAG, "Failed to set password");
			}
		}

		mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

		IMqttActionListener mActionListener = new IMqttActionListener() {
			@Override
			public void onSuccess(IMqttToken token) {
				Loggor.info(TAG, "onSuccess!");
				setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
				mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(),
						"connected to " + mServerURI, null);
				// If the connection is established, subscribe the gateway
				// operation topic
				String gwTopic = GW_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
				int qos = TXMqttConstants.QOS1;

				subscribe(gwTopic, qos, "Subscribe GATEWAY result topic");
				Loggor.debug(TAG, "Connected, then subscribe the gateway result topic");
			}

			@Override
			public void onFailure(IMqttToken token, Throwable exception) {
				Loggor.error(TAG, "onFailure!" + exception);
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString(), exception);
			}
		};

		if (mMqttClient == null) {
			try {
				mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist);
				mMqttClient.setCallback(this);
				mMqttClient.setBufferOpts(super.bufferOpts);
				mMqttClient.setManualAcks(false);
			} catch (Exception e) {
				Loggor.error(TAG, "new MqttClient failed" + e);
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				return Status.ERROR;
			}
		}

		try {
			Loggor.info(TAG, "Start connecting to " + mServerURI);
			setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
			mMqttClient.connect(mConnOptions, userContext, mActionListener);
		} catch (Exception e) {
			Loggor.error(TAG, "MqttClient connect failed" + e);
			setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
			return Status.ERROR;
		}

		return Status.OK;
	}
}
