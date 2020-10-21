package com.tencent.iot.hub.device.java.core.shadow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SERVER_PORT_TLS;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.QCLOUD_IOT_MQTT_DIRECT_DOMAIN;

public class TXShadowConnection {

	public static final String TAG = TXShadowConnection.class.getName();
	 private static final Logger LOG = LoggerFactory.getLogger(TXShadowConnection.class);
	/**
	 * clientToken formatter
	 */
	private static final String CLIENT_TOKEN = "%s-%d";

	/**
	 * shadow action回调接口
	 */
	private TXShadowActionCallBack mShadowActionCallback = null;

	/**
	 * mqtt 连接实例
	 */
	private TXMqttConnection mMqttConnection = null;

	private int mQos = TXMqttConstants.QOS0;

	private AtomicInteger mClientTokenNum = new AtomicInteger(0);

	/**
	 * 保存用户注册的属性
	 */
	private HashMap<String, DeviceProperty> mRegisterPropertyMap = new HashMap<>();

	/**
	 * 是否已经成功订阅OPERATION_RESULT_TOPIC
	 */
	private boolean mIsOperationResultSubscribeSuccess = false;

	/**
	 * 文档版本号
	 */
	private int mDocumentVersion = 0;

	private String OPERATION_TOPIC = null;
	private String OPERATION_RESULT_TOPIC = null;

	private static final int MAX_MESSAGE_ID = 65535;
	private int mPublishMessageId = 0;

	public ShadowUponMqttCallBack mShadowUponMqttCallBack = null;

	/**
	 * @param productID
	 *            产品名
	 * @param deviceName
	 *            设备名，唯一
	 * @param secretKey
	 *            密钥
	 * @param callBack
	 *            连接、消息发布、消息订阅回调接口
	 */
	public TXShadowConnection( String productID, String deviceName, String secretKey,
			TXShadowActionCallBack callBack) {
		this(productID, deviceName, secretKey, null, callBack);
	}

	/**
	 * @param productID
	 *            产品名
	 * @param deviceName
	 *            设备名，唯一
	 * @param secretKey
	 *            密钥
	 * @param bufferOpts
	 *            发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
	 * @param callBack
	 *            连接、消息发布、消息订阅回调接口
	 */
	public TXShadowConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, TXShadowActionCallBack callBack) {
		this(productID, deviceName, secretKey, bufferOpts, null, callBack);
	}

	/**
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
	public TXShadowConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXShadowActionCallBack callBack) {
		this( null, productID, deviceName, secretKey, bufferOpts,
				clientPersistence, callBack);
	}

	/**
	 * Shadow连接器构造器
	 *
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
	public TXShadowConnection(String serverURI, String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXShadowActionCallBack callBack) {
		this.mShadowActionCallback = callBack;

		mShadowUponMqttCallBack = new ShadowUponMqttCallBack();
		mMqttConnection = new TXMqttConnection(serverURI, productID, deviceName, secretKey, bufferOpts,
				clientPersistence, mShadowUponMqttCallBack);

		OPERATION_TOPIC = "$shadow/operation/" + productID + "/" + mMqttConnection.mDeviceName;
		OPERATION_RESULT_TOPIC = "$shadow/operation/result/" + productID + "/" + mMqttConnection.mDeviceName;

		mPublishMessageId = new Random().nextInt(MAX_MESSAGE_ID);
	}

	public void setMqttConnection(TXMqttConnection connection) {
		mMqttConnection = connection;
	}

	/**
	 * 获取TXMqttConnection句柄
	 */
	public TXMqttConnection getMqttConnection() {
		return mMqttConnection;
	}

	/**
	 * 设置断连状态buffer缓冲区
	 *
	 * @param bufferOpts
	 *            缓冲参数
	 */
	public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
		mMqttConnection.setBufferOpts(bufferOpts);
	}

	/**
	 * 与云端建立连接，结果通过回调函数通知。
	 *
	 * @param options
	 *            连接参数
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public synchronized Status connect(MqttConnectOptions options, Object userContext) {
		Status status = mMqttConnection.connect(options, userContext);
		if (status != Status.OK) {
			return status;
		}

		long startTimeMills = System.currentTimeMillis();
		while (!mIsOperationResultSubscribeSuccess) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}

			if (mMqttConnection.getConnectStatus() == TXMqttConstants.ConnectStatus.kConnectFailed) {
				return Status.ERROR;
			}

			if (System.currentTimeMillis() - startTimeMills > 20000) { // 20
																		// seconds
				LOG.error("Subscribe topic [{}] timeout!!!", OPERATION_RESULT_TOPIC);
				return Status.ERROR;
			}
		}

		return status;
	}

	/**
	 * 断开连接请求，结果通过回调函数通知。
	 *
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status disConnect(Object userContext) {
		Status status = mMqttConnection.unSubscribe(OPERATION_RESULT_TOPIC, userContext);
		if (status != Status.OK) {
			return status;
		}

		return mMqttConnection.disConnect("disconnect context");
	}
	
	public Status publish(String topic, MqttMessage msg, Object userCtx) {
		return mMqttConnection.publish(topic, msg, userCtx);
	}
	
	public Status subcribe(String topic, int qos, Object userContext) {
		return mMqttConnection.subscribe(topic, qos, userContext);
	}

	/**
	 * 获取连接状态
	 *
	 * @return 连接状态
	 */
	public TXMqttConstants.ConnectStatus getConnectStatus() {
		return mMqttConnection.getConnectStatus();
	}

	/**
	 * 更新设备属性信息，结果通过回调函数通知。
	 *
	 * @param devicePropertyList
	 *            需要更新的设备属性集
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status update(List<? extends DeviceProperty> devicePropertyList, Object userContext) {
		Status status = checkMqttStatus();
		if (Status.OK != status) {
			return status;
		}

		if (!mIsOperationResultSubscribeSuccess) {
			LOG.debug("******subscribe topic:" + OPERATION_RESULT_TOPIC);
			mMqttConnection.subscribe(OPERATION_RESULT_TOPIC, mQos, "subscribe context");
			return Status.ERROR_TOPIC_UNSUBSCRIBED;
		}

		String clientToken = String.format(CLIENT_TOKEN, mMqttConnection.mClientId, mClientTokenNum.getAndIncrement());
		String jsonDocument = buildUpdateJsonDocument(devicePropertyList, clientToken);

		return publish(OPERATION_TOPIC, jsonDocument, userContext);
	}

	/**
	 * 更新delta信息后，上报空的desired信息，通知服务器不再发送delta消息。
	 *
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status reportNullDesiredInfo() {
		Status status = checkMqttStatus();
		if (Status.OK != status) {
			return status;
		}

		String clientToken = String.format(CLIENT_TOKEN, mMqttConnection.mClientId, mClientTokenNum.getAndIncrement());
		String jsonDocument = buildDesiredNullJsonDocument(null, clientToken);

		LOG.debug("reportNullDesiredInfo, document: {}", jsonDocument);

		return publish(OPERATION_TOPIC, jsonDocument, null);
	}

	/**
	 * 更新delta信息后，上报空的desired信息，通知服务器不再发送delta消息。
	 *
	 * @param reportJsonDoc
	 *            用户上报的JSON内容
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status reportNullDesiredInfo(String reportJsonDoc) {
		Status status = checkMqttStatus();
		if (Status.OK != status) {
			return status;
		}

		String clientToken = String.format(CLIENT_TOKEN, mMqttConnection.mClientId, mClientTokenNum.getAndIncrement());
		String jsonDocument = buildDesiredNullJsonDocument(reportJsonDoc, clientToken);

		LOG.debug("reportNullDesiredInfo, document: {}", jsonDocument);

		return publish(OPERATION_TOPIC, jsonDocument, null);
	}

	/**
	 * 获取设备影子文档，结果通过回调函数通知。
	 *
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status get(Object userContext) {
		Status status = checkMqttStatus();
		if (Status.OK != status) {
			return status;
		}

		if (!mIsOperationResultSubscribeSuccess) {
			LOG.debug("***subscribe topic:" + OPERATION_RESULT_TOPIC);
			mMqttConnection.subscribe(OPERATION_RESULT_TOPIC, mQos, "subscribe context");

			return Status.ERROR_TOPIC_UNSUBSCRIBED;
		}

		String clientToken = String.format(CLIENT_TOKEN, mMqttConnection.mClientId, mClientTokenNum.getAndIncrement());
		String jsonDocument = buildGetJsonDocument(clientToken);

		LOG.debug("get document: {}", jsonDocument);

		return publish(OPERATION_TOPIC, jsonDocument, userContext);
	}

	/**
	 * 注册当前设备的设备属性
	 *
	 * @param property
	 *            设备属性
	 */
	public void registerProperty(DeviceProperty property) {
		mRegisterPropertyMap.put(property.mKey, property);
	}

	/**
	 * 取消注册当前设备的指定属性
	 *
	 * @param property
	 */
	public void unRegisterProperty(DeviceProperty property) {
		mRegisterPropertyMap.remove(property.mKey);
	}

	/**
	 * 向指定TOPIC发布设备影子文档，结果通过回调函数通知。
	 *
	 * @param topic
	 *            指定的topic
	 * @param document
	 *            json文档
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	private Status publish(String topic, String document, Object userContext) {
		Status status;
		status = checkMqttStatus();
		if (status != Status.OK) {
			return status;
		}

		MqttMessage mqttMessage = new MqttMessage();
		if ((null != document) && (document.length() != 0)) {
			mqttMessage.setId(getMessageId());
			mqttMessage.setPayload(document.getBytes());
		}
		mqttMessage.setQos(TXMqttConstants.QOS0);
		LOG.debug("******publish message id:" + mqttMessage.getId());

		status = mMqttConnection.publish(topic, mqttMessage, userContext);
		if (status != Status.OK) {
			return status;
		}

		return Status.OK;
	}

	/**
	 * 检查mqtt状态
	 *
	 * @return 当前状态
	 */
	private Status checkMqttStatus() {
		if (null == mMqttConnection || mMqttConnection.getConnectStatus() != TXMqttConstants.ConnectStatus.kConnected) {
			LOG.error("mqtt is disconnected!");
			return Status.MQTT_NO_CONN;
		}

		return Status.OK;
	}

	/**
	 * 构建json信息
	 *
	 * @param devicePropertyList
	 *            需要上报的设备属性集
	 * @param clientToken
	 *            clientToken字段
	 * @return json字符串
	 */
	private String buildUpdateJsonDocument(List<? extends DeviceProperty> devicePropertyList, String clientToken) {
		JSONObject documentJSONObj = new JSONObject();

		try {
			documentJSONObj.put(TXShadowConstants.TYPE, TXShadowConstants.UPDATE);

			JSONObject stateJSONObj = new JSONObject();
			if (devicePropertyList != null && !devicePropertyList.isEmpty()) {

				JSONObject reportedJSONObj = new JSONObject();
				for (DeviceProperty deviceProperty : devicePropertyList) {
					if (TXShadowConstants.JSONDataType.INT == deviceProperty.mDataType) {
						reportedJSONObj.put(deviceProperty.mKey, Integer.parseInt((String) deviceProperty.mData));
					} else if (TXShadowConstants.JSONDataType.LONG == deviceProperty.mDataType) {
						reportedJSONObj.put(deviceProperty.mKey, Long.parseLong((String) deviceProperty.mData));
					} else if (TXShadowConstants.JSONDataType.FLOAT == deviceProperty.mDataType) {
						reportedJSONObj.put(deviceProperty.mKey, Float.parseFloat((String) deviceProperty.mData));
					} else if (TXShadowConstants.JSONDataType.DOUBLE == deviceProperty.mDataType) {
						reportedJSONObj.put(deviceProperty.mKey, Double.parseDouble((String) deviceProperty.mData));
					} else if (TXShadowConstants.JSONDataType.BOOLEAN == deviceProperty.mDataType) {
						reportedJSONObj.put(deviceProperty.mKey, Boolean.parseBoolean((String) deviceProperty.mData));
					} else {
						reportedJSONObj.put(deviceProperty.mKey, deviceProperty.mData);
					}
				}
				stateJSONObj.put(TXShadowConstants.REPORTED, reportedJSONObj);
			}

			documentJSONObj.put(TXShadowConstants.STATE, stateJSONObj);
			documentJSONObj.put(TXShadowConstants.CLIENT_TOKEN, clientToken);
			documentJSONObj.put(TXShadowConstants.VERSION, mDocumentVersion);

		} catch (JSONException e) {
			LOG.error("{}", "build report info failed", e);
			return "";
		}

		return documentJSONObj.toString();
	}

	private String buildDesiredNullJsonDocument(String reportJsonDoc, String clientToken) {
		JSONObject documentJSONObj = new JSONObject();

		try {
			documentJSONObj.put(TXShadowConstants.TYPE, TXShadowConstants.UPDATE);

			JSONObject stateJSONObj = new JSONObject();
			if (reportJsonDoc != null) {
				JSONObject reportedJSONObj = new JSONObject(reportJsonDoc);
				stateJSONObj.put(TXShadowConstants.REPORTED, reportedJSONObj);
			}
			stateJSONObj.put(TXShadowConstants.DESIRED, "");

			documentJSONObj.put(TXShadowConstants.STATE, stateJSONObj);
			documentJSONObj.put(TXShadowConstants.CLIENT_TOKEN, clientToken);
			documentJSONObj.put(TXShadowConstants.VERSION, mDocumentVersion);

		} catch (JSONException e) {
			LOG.error("{}", "build report info failed", e);
			return "";
		}

		return documentJSONObj.toString();
	}

	private String buildGetJsonDocument(String clientToken) {
		JSONObject documentJSONObj = new JSONObject();

		try {
			documentJSONObj.put(TXShadowConstants.TYPE, TXShadowConstants.GET);
			documentJSONObj.put(TXShadowConstants.CLIENT_TOKEN, clientToken);
		} catch (JSONException e) {
			LOG.error("{}", "build report info failed", e);
			return "";
		}

		return documentJSONObj.toString();
	}

	private String buildDeleteJsonDocument(String clientToken) {
		JSONObject documentJSONObj = new JSONObject();

		try {
			documentJSONObj.put(TXShadowConstants.TYPE, TXShadowConstants.DELETE);
			documentJSONObj.put(TXShadowConstants.CLIENT_TOKEN, clientToken);
		} catch (JSONException e) {
			LOG.error("{}", "build report info failed", e);
			return "";
		}

		return documentJSONObj.toString();
	}

	private int getMessageId() {
		mPublishMessageId++;
		if (mPublishMessageId > MAX_MESSAGE_ID) {
			mPublishMessageId = 1;
		}

		return mPublishMessageId;
	}

	/**
	 * 处理接收到的影子消息
	 *
	 * @param topic
	 *            来自哪个TOPIC的消息
	 * @param message
	 *            MQTT消息
	 */
	private void processShadowMessageReceived(String topic, MqttMessage message) {
		if (null == message || null == message.getPayload()) {
			LOG.error("handle mqtt message failed, reason[{}]!", "message or payload is empty");
			return;
		}

		String jsonStr = new String(message.getPayload());
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(jsonStr);

			String type = jsonObj.getString(TXShadowConstants.TYPE);
			if (TXShadowConstants.DELTA.equals(type)) {
				processDeltaMessage(message);
			}
		} catch (Exception e) {

		}
	}

	/**
	 * 处理delta消息函数。 当服务端发送delta消息到终端, 将会调用此方法
	 *
	 * @param message
	 *            mqtt消息
	 */
	private void processDeltaMessage(MqttMessage message) {
		JSONObject jsonObj = null;

		try {
			jsonObj = new JSONObject(new String(message.getPayload()));
			//edited by v_vweisun 2020/09/22 start
			jsonObj = jsonObj.getJSONObject(TXShadowConstants.PAYLOAD);//new JSONObject(jsonObj.getString(TXShadowConstants.PAYLOAD));
			//edited by v_vweisun 2020/09/22 end

			if (jsonObj.has(TXShadowConstants.VERSION)) {
				int versionNum = jsonObj.getInt(TXShadowConstants.VERSION);
				if (versionNum > mDocumentVersion) {
					mDocumentVersion = versionNum;
					LOG.debug("New Version number : {}", mDocumentVersion);
				} else {
					LOG.warn("Old Delta Message received - Ignoring rx : {} local : {}", versionNum,
							mDocumentVersion);
					return;
				}
			}
		} catch (JSONException e) {
			LOG.error("{}", "Received JSON is not valid!", e);
			return;
		}

		if (!jsonObj.has(TXShadowConstants.STATE)) {
			return;
		}

		List<DeviceProperty> propertyList = new ArrayList<>();
		String stateJsonStr = "";

		try {
			//edited by v_vweisun 2020/09/22 start
//			stateJsonStr = jsonObj.getString(TXShadowConstants.STATE);

			JSONObject stateObj = jsonObj.getJSONObject(TXShadowConstants.STATE);//new JSONObject(stateJsonStr);
			//edited by v_vweisun 2020/09/22 end

			Iterator it = mRegisterPropertyMap.keySet().iterator();
			while (it.hasNext()) {
				DeviceProperty property = mRegisterPropertyMap.get(it.next());

				if (stateObj.has(property.mKey)) {
					// edited by v_vweisun 2020/09/22 start
					String value = "";
					if (TXShadowConstants.JSONDataType.INT == property.mDataType) {
						property.data(stateObj.getInt(property.mKey));
						value = String.valueOf(stateObj.getInt(property.mKey));
					} else if (TXShadowConstants.JSONDataType.LONG == property.mDataType) {
						property.data(stateObj.getLong(property.mKey));
						value = String.valueOf(stateObj.getLong(property.mKey));
					} else if (TXShadowConstants.JSONDataType.FLOAT == property.mDataType) {
						property.data(stateObj.getFloat(property.mKey));
						value = String.valueOf(stateObj.getFloat(property.mKey));
					} else if (TXShadowConstants.JSONDataType.DOUBLE == property.mDataType) {
						property.data(stateObj.getDouble(property.mKey));
						value = String.valueOf(stateObj.getDouble(property.mKey));
					} else if (TXShadowConstants.JSONDataType.BOOLEAN == property.mDataType) {
						property.data(stateObj.getBoolean(property.mKey));
						value = String.valueOf(stateObj.getBoolean(property.mKey));
					} else {
						property.data(stateObj.getString(property.mKey));
						value = String.valueOf(stateObj.getString(property.mKey));
					}
					// edited by v_vweisun 2020/09/22 end
					propertyList.add(property);
					LOG.debug("******{}, {}", property.mKey, value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (null != mShadowActionCallback && !propertyList.isEmpty()) {
			mShadowActionCallback.onDevicePropertyCallback(stateJsonStr, propertyList);
		}
	}

	private class ShadowUponMqttCallBack extends TXMqttActionCallBack {
		@Override
		public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
			LOG.debug("onConnectCompleted, status[{}], reconnect[{}], msg[{}]", status, reconnect, msg);
			if (Status.OK == status) {
				LOG.debug("******subscribe topic:" + OPERATION_RESULT_TOPIC);
				mMqttConnection.subscribe(OPERATION_RESULT_TOPIC, mQos, "subscribe context");
			}
			mShadowActionCallback.onConnectCompleted(status, reconnect, userContext, msg);
		}

		@Override
		public void onConnectionLost(Throwable cause) {
			LOG.error("{}", "mqtt connection lost!", cause);
			mIsOperationResultSubscribeSuccess = false;
			mShadowActionCallback.onConnectionLost(cause);
		}

		@Override
		public void onDisconnectCompleted(Status status, Object userContext, String msg) {
			LOG.debug("onDisconnectCompleted, status[{}], msg[{}]", status.name(), msg);

			mIsOperationResultSubscribeSuccess = false;
		}

		@Override
		public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			super.onPublishCompleted(status, token, userContext, errMsg);

			String[] topics = token.getTopics();
			LOG.debug("onPublishCompleted, status[{}], errMsg[{}], topics[{}]", status.name(), errMsg,
					Arrays.toString(topics));
			for (String topic : topics) {
				if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
				} else {
					mShadowActionCallback.onPublishCompleted(status, token, userContext, errMsg);
				}
			}
		}

		@Override
		public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			super.onSubscribeCompleted(status, token, userContext, errMsg);

			String[] topics = token.getTopics();
			LOG.debug("onSubscribeCompleted, status[{}], errMsg[{}], topics[{}]", status.name(), errMsg,
					Arrays.toString(topics));
			for (String topic : topics) {
				if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
					if (status == Status.OK) {
						LOG.debug("***subscribe topic:" + OPERATION_RESULT_TOPIC + " success!!!!");
						mIsOperationResultSubscribeSuccess = true;
					}
				} else {
					mShadowActionCallback.onSubscribeCompleted(status, token, userContext, errMsg);
				}
			}
		}

		@Override
		public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
			super.onUnSubscribeCompleted(status, token, userContext, errMsg);

			String[] topics = token.getTopics();
			LOG.debug("onUnSubscribeCompleted, status[{}], errMsg[{}], topics[{}]", status.name(), errMsg,
					Arrays.toString(topics));
			for (String topic : topics) {
				if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
					if (status == Status.OK) {
						mIsOperationResultSubscribeSuccess = false;
					}
				} else {
					mShadowActionCallback.onUnSubscribeCompleted(status, token, userContext, errMsg);
				}
			}
		}

		@Override
		public void onMessageReceived(String topic, MqttMessage message) {
			super.onMessageReceived(topic, message);

			LOG.debug("onMessageReceived,  topics[{}]", topic);

			if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
				if (null == message || null == message.getPayload()) {
					LOG.error("handle mqtt message failed, reason[{}]!", "message or payload is empty");
					return;
				}

				String jsonStr = new String(message.getPayload());

				try {
					JSONObject jsonObj = new JSONObject(jsonStr);

					if (jsonObj.has(TXShadowConstants.TYPE)) {
						String type = jsonObj.getString(TXShadowConstants.TYPE);

						if (TXShadowConstants.GET.equals(type) || TXShadowConstants.UPDATE.equals(type)
								|| TXShadowConstants.DELETE.equals(type)) {

							if (jsonObj.has(TXShadowConstants.RESULT)) {
								int result = jsonObj.getInt(TXShadowConstants.RESULT);
								if (jsonObj.has(TXShadowConstants.PAYLOAD)) {
									String payloadStr = jsonObj.getJSONObject(TXShadowConstants.PAYLOAD).toString();

									mShadowActionCallback.onRequestCallback(type, result, payloadStr);

									JSONObject payloadJsonObj = new JSONObject(payloadStr);
									if (payloadJsonObj.has(TXShadowConstants.VERSION)) {
										mDocumentVersion = payloadJsonObj.getInt(TXShadowConstants.VERSION);
										LOG.debug("******update local mDocumentVersion to " + mDocumentVersion);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				processShadowMessageReceived(topic, message);
			} else {
				mShadowActionCallback.onMessageReceived(topic, message);
			}
		}
	}
}
