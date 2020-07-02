package com.qcloud.iot.core.mqtt;

import com.qcloud.iot.core.common.Status;
import com.qcloud.iot.core.util.Base64;
import com.qcloud.iot.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;


import static  com.qcloud.iot.core.mqtt.TXMqttConstants.DEFAULT_SERVER_URI;
import static  com.qcloud.iot.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

public class TXMqttConnection implements MqttCallbackExtended {

    private static final Logger LOG = LoggerFactory.getLogger(TXMqttConnection.class);
	/**
	 * tcp://localhost:port ssl://localhost:port
	 */
	public String mServerURI = TXMqttConstants.DEFAULT_SERVER_URI;
	public String mClientId;
	public String mProductId;
	public String mDeviceName;
	public String mUserName;
	public String mSecretKey;

	protected MqttClientPersistence mMqttPersist = null;
	protected MqttConnectOptions mConnOptions = null;

	protected MqttAsyncClient mMqttClient = null;

	protected TXAlarmPingSender mPingSender = null;
	protected TXMqttActionCallBack mActionCallBack = null;

	private HashMap<String, Integer> mSubscribedTopicMap = new HashMap<>();

	private static int INVALID_MESSAGE_ID = -1;
	protected int mLastReceivedMessageId = INVALID_MESSAGE_ID;

	private TXOTAImpl mOTAImpl = null;

	/**
	 * 断连状态下buffer缓冲区，当连接重新建立成功后自动将buffer中数据写出
	 */
	protected DisconnectedBufferOptions bufferOpts = null;

	protected volatile TXMqttConstants.ConnectStatus mConnectStatus = TXMqttConstants.ConnectStatus.kConnectIdle;

	/**
	 * @param context
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @param productID
	 *            产品名
	 * @param deviceName
	 *            设备名，唯一
	 * @param secretKey
	 *            密钥
	 * @param callBack
	 *            连接、消息发布、消息订阅回调接口
	 */
	public TXMqttConnection(String productID, String deviceName, String secretKey,
			TXMqttActionCallBack callBack) {
		this( productID, deviceName, secretKey, null, callBack);
	}
	
	public TXMqttConnection(String productID, String deviceName,
			TXMqttActionCallBack callBack) {
		this( productID, deviceName, null, null, callBack);
	}

	/**
	 * @param context
	 *            用户上下文（这个参数在回调函数时透传给用户）
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
	public TXMqttConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
		this(productID, deviceName, secretKey, bufferOpts, null, callBack);
	}

	/**
	 * @param context
	 *            用户上下文（这个参数在回调函数时透传给用户）
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
	public TXMqttConnection(String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXMqttActionCallBack callBack) {
		this(DEFAULT_SERVER_URI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
	}

	/**
	 * @param context
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @param serverURI
	 *            服务器URI，腾讯云默认唯一地址 TXMqttConstants.DEFAULT_SERVER_URI=
	 *            "ssl://connect.iot.qcloud.com:8883"
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
	public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,
			DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
			TXMqttActionCallBack callBack) {

		this.mSecretKey = secretKey;
		this.mServerURI = serverURI;
		this.mProductId = productID;
		this.mClientId = productID + deviceName;
		this.mDeviceName = deviceName;
		this.mUserName = mClientId + ";" + TXMqttConstants.APPID;
		this.bufferOpts = bufferOpts;
		this.mMqttPersist = clientPersistence;

		this.mActionCallBack = callBack;
	}

	/**
	 * 设置断连状态buffer缓冲区
	 *
	 * @param bufferOpts
	 */
	public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
		this.bufferOpts = bufferOpts;
		mMqttClient.setBufferOpts(bufferOpts);
	}

	/**
	 * 连接MQTT服务器，结果通过回调函数通知。
	 *
	 * @param options
	 *            连接参数
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public synchronized Status connect(MqttConnectOptions options, Object userContext) {
		if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
			LOG.info("The client is connecting. Connect return directly.");
			return Status.MQTT_CONNECT_IN_PROGRESS;
		}

		if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
			LOG.info("The client is already connected. Connect return directly.");
			return Status.OK;
		}

		this.mConnOptions = options;
		if (mConnOptions == null) {
			LOG.error("Connect options == null, will not connect.");
			return Status.PARAMETER_INVALID;
		}

		Long timestamp;
		if (options.isAutomaticReconnect()) {
			timestamp = (long) Integer.MAX_VALUE;
		} else {
			timestamp = System.currentTimeMillis() / 1000 + 600;
		}
		String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;

		mConnOptions.setUserName(userNameStr);

		if (mSecretKey != null) {
			try {
				LOG.debug("secret is " + mSecretKey);
				String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(),
						Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
				mConnOptions.setPassword(passWordStr.toCharArray());
			} catch (IllegalArgumentException e) {
				LOG.debug("Failed to set password");
			}
		}

		mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

		IMqttActionListener mActionListener = new IMqttActionListener() {
			@Override
			public void onSuccess(IMqttToken token) {
				LOG.info("onSuccess!");
				setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
				mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(),
						"connected to " + mServerURI);
			}

			@Override
			public void onFailure(IMqttToken token, Throwable exception) {
				LOG.error(exception + "onFailure!");
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString());
			}
		};

		if (mMqttClient == null) {
			try {
				mPingSender = new TXAlarmPingSender();
				mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
				mMqttClient.setCallback(this);
				mMqttClient.setBufferOpts(this.bufferOpts);
				mMqttClient.setManualAcks(false);
			} catch (Exception e) {
				LOG.error("new MqttClient failed", e);
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				return Status.ERROR;
			}
		}

		try {
			IMqttToken token;
			LOG.info("Start connecting to " + mServerURI);
			setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
			token = mMqttClient.connect(mConnOptions, userContext, mActionListener);
			token.waitForCompletion(-1);
			LOG.info("wait_for completion return");
		} catch (Exception e) {
			LOG.error("MqttClient connect failed", e);
			setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
			return Status.ERROR;
		}

		return Status.OK;
	}

	/**
	 * 重新连接, 结果通过回调函数通知。
	 *
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public synchronized Status reconnect() {
		if (mMqttClient == null) {
			LOG.error("Reconnect myClient = null. Will not do reconnect");
			return Status.MQTT_NO_CONN;
		}

		if (getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnecting)) {
			LOG.info("The client is connecting. Reconnect return directly.");
			return Status.MQTT_CONNECT_IN_PROGRESS;
		}

		if (mConnOptions.isAutomaticReconnect()
				&& !getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnecting)) {
			LOG.info("Requesting Automatic reconnect using New Java AC");
			try {
				mMqttClient.reconnect();
			} catch (Exception ex) {
				LOG.error("Exception occurred attempting to reconnect: ", ex);
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				return Status.ERROR;
			}
		} else if (getConnectStatus().equals(TXMqttConstants.ConnectStatus.kDisconnected)
				&& !mConnOptions.isCleanSession()) {
			IMqttActionListener listener = new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					LOG.info("onSuccess!");
					// mActionCallBack.onConnectCompleted(Status.OK, true,
					// asyncActionToken.getUserContext(), "reconnected to " +
					// mServerURI);
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					LOG.error(exception+"onFailure!");
					setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
					mActionCallBack.onConnectCompleted(Status.ERROR, true, asyncActionToken.getUserContext(),
							exception.toString());
				}
			};

			try {
				mMqttClient.connect(mConnOptions, null, listener);
				setConnectingState(TXMqttConstants.ConnectStatus.kDisconnected);
			} catch (Exception e) {
				LOG.error("Exception occurred attempting to reconnect: ", e);
				setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
				return Status.ERROR;
			}
		}

		return Status.OK;
	}

	/**
	 * MQTT断连，结果通过回调函数通知。
	 *
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status disConnect(Object userContext) {
		return disConnect(0, userContext);
	}

	/**
	 * MQTT断连, 结果通过回调函数通知。
	 *
	 * @param timeout
	 *            等待时间（必须大于0）。单位：毫秒
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status disConnect(long timeout, Object userContext) {
		mLastReceivedMessageId = INVALID_MESSAGE_ID;

		if (mOTAImpl != null) {
			mOTAImpl.setSubscribedState(false);
		}

		if (mMqttClient != null && mMqttClient.isConnected()) {
			IMqttActionListener mActionListener = new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					setConnectingState(TXMqttConstants.ConnectStatus.kDisconnected);
					mActionCallBack.onDisconnectCompleted(Status.OK, asyncActionToken.getUserContext(),
							"disconnected to " + mServerURI);
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
					mActionCallBack.onDisconnectCompleted(Status.ERROR, asyncActionToken.getUserContext(),
							cause.toString());
				}
			};

			try {
				if (timeout <= 0) {
					mMqttClient.disconnect(userContext, mActionListener);
				} else {
					mMqttClient.disconnect(timeout, userContext, mActionListener);
				}
			} catch (MqttException e) {
				LOG.error(e + "manual disconnect failed.");
				return Status.ERROR;
			}
		}

		return Status.ERROR;
	}

	/**
	 * 发布MQTT消息接口, 结果通过回调函数通知。
	 *
	 * @param topic
	 *            topic名称
	 * @param message
	 *            消息内容
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status publish(String topic, MqttMessage message, Object userContext) {
		IMqttDeliveryToken sendToken = null;

		if (topic == null || topic.trim().length() == 0) {
			LOG.error("Topic is empty!!!");
			return Status.PARAMETER_INVALID;
		}
		if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
			LOG.error("Topic length is too long!!!");
			return Status.PARAMETER_INVALID;
		}

		LOG.info("Starting publish topic: %s Message: %s", topic, message.toString());
		System.out.println("topic = " + topic);
		System.out.println("message.toString() = " + message.toString());
		//System.out.println("mMqttClient.isConnected() = " + mMqttClient.isConnected());
		
		System.out.println("mMqttClient != null = " + mMqttClient != null);
		
		if ((mMqttClient != null) && (mMqttClient.isConnected())) {
			try {
				sendToken = mMqttClient.publish(topic, message, userContext,
						new QcloudMqttActionListener(TXMqttConstants.PUBLISH));
			} catch (Exception e) {
				LOG.error(e + "publish topic: " + topic + " failed1.");
				return Status.ERROR;
			}
		} else if ((mMqttClient != null) && (this.bufferOpts != null) && (this.bufferOpts.isBufferEnabled())) { // 放入缓存
			try {
				sendToken = mMqttClient.publish(topic, message, userContext,
						new QcloudMqttActionListener(TXMqttConstants.PUBLISH));
			} catch (Exception e) {
				LOG.error(e + "publish topic: " + topic + " failed2.");
				return Status.ERROR;
			}
		} else {
			System.out.println("1111111111111111111111111111 topic = " + topic);
			LOG.error( "publish topic: %s failed, mMqttClient not connected and disconnect buffer not enough."+
					topic);
			return Status.ERROR;
		}

		return Status.OK;
	}

	/**
	 * 订阅Topic, 结果通过回调函数通知。
	 *
	 * @param topic
	 *            topic名称
	 * @param qos
	 *            QOS等级
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status subscribe(final String topic, final int qos, Object userContext) {
		if (topic == null || topic.trim().length() == 0) {
			LOG.error("Topic is empty!!!");
			return Status.PARAMETER_INVALID;
		}
		if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
			LOG.error("Topic length is too long!!!");
			return Status.PARAMETER_INVALID;
		}

		LOG.info("Starting subscribe topic: %s" + topic);

		if ((mMqttClient != null) && (mMqttClient.isConnected())) {
			try {
				mMqttClient.subscribe(topic, qos, userContext, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
			} catch (Exception e) {
				LOG.error(e + "subscribe topic: %s failed.", topic);
				return Status.ERROR;
			}
		} else {
			LOG.error("subscribe topic: %s failed, because mMqttClient not connected." + topic);
			return Status.MQTT_NO_CONN;
		}

		mSubscribedTopicMap.put(topic, qos);

		return Status.OK;
	}

	/**
	 * 取消订阅主题, 结果通过回调函数通知。
	 *
	 * @param topic
	 *            要取消订阅的主题
	 * @param userContext
	 *            用户上下文（这个参数在回调函数时透传给用户）
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status unSubscribe(final String topic, Object userContext) {
		if (topic == null || topic.trim().length() == 0) {
			LOG.error("Topic is empty!!!");
			return Status.PARAMETER_INVALID;
		}
		if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
			LOG.error("Topic length is too long!!!");
			return Status.PARAMETER_INVALID;
		}

		LOG.info("Starting unSubscribe topic: %s" + topic);

		if ((mMqttClient != null) && (mMqttClient.isConnected())) {
			try {
				mMqttClient.unsubscribe(topic, userContext, new QcloudMqttActionListener(TXMqttConstants.UNSUBSCRIBE));
			} catch (Exception e) {
				LOG.error(e + "unSubscribe topic: %s failed.", topic);
				return Status.ERROR;
			}
		} else {
			LOG.error("unSubscribe topic: %s failed, because mMqttClient not connected." + topic);
			return Status.MQTT_NO_CONN;
		}

		mSubscribedTopicMap.remove(topic);

		return Status.OK;
	}

	/**
	 * 初始化OTA功能。
	 *
	 * @param storagePath
	 *            OTA升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
	 * @param callback
	 *            OTA事件回调
	 */
	public void initOTA(String storagePath, TXOTACallBack callback) {
		mOTAImpl = new TXOTAImpl(this, storagePath, callback);
	}

	/**
	 * 上报设备当前版本信息到后台服务器。
	 *
	 * @param currentFirmwareVersion
	 *            设备当前版本信息
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status reportCurrentFirmwareVersion(String currentFirmwareVersion) {
		if (mOTAImpl != null && currentFirmwareVersion != null) {
			return mOTAImpl.reportCurrentFirmwareVersion(currentFirmwareVersion);
		}

		return Status.ERROR;
	}

	/**
	 * 上报设备升级状态到后台服务器。
	 *
	 * @param state
	 *            状态
	 * @param resultCode
	 *            结果代码。0：表示成功；其它：表示失败；常见错误码：-1: 下载超时;
	 *            -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
	 * @param resultMsg
	 *            结果描述
	 * @param version
	 *            版本号
	 * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
	 */
	public Status reportOTAState(TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
		if (mOTAImpl != null) {
			return mOTAImpl.reportUpdateFirmwareState(state.toString().toLowerCase(), resultCode, resultMsg, version);
		}

		return Status.ERROR;
	}

	/**
	 * 设置当前连接状态
	 *
	 * @param connectStatus
	 *            当前连接状态
	 */
	protected synchronized void setConnectingState(TXMqttConstants.ConnectStatus connectStatus) {
		this.mConnectStatus = connectStatus;
	}

	/**
	 * @return 当前连接状态
	 */
	public TXMqttConstants.ConnectStatus getConnectStatus() {
		return this.mConnectStatus;
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		LOG.info("connectComplete. reconnect flag is " + reconnect);
		setConnectingState(TXMqttConstants.ConnectStatus.kConnected);

//		if (!reconnect) {
//			return;
//		}
//
//		Iterator<String> it = mSubscribedTopicMap.keySet().iterator();
//		while (it.hasNext()) {
//			String topic = it.next();
//			Integer qos = mSubscribedTopicMap.get(topic);
//			try {
//				LOG.info("subscribe to %s..." + topic);
//				mMqttClient.subscribe(topic, qos, null, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
//			} catch (Exception e) {
//				LOG.error( "subscribe to %s failed." + topic);
//			}
//		}

		mActionCallBack.onConnectCompleted(Status.OK, reconnect, null, "connected to " + serverURI);
	}

	/**
	 * 连接丢失，回调上层
	 *
	 * @param cause
	 *            连接断开原因
	 */
	@Override
	public void connectionLost(Throwable cause) {
		LOG.error("connection lost because of: %s" + cause.toString());

		setConnectingState(TXMqttConstants.ConnectStatus.kDisconnected);

		mActionCallBack.onConnectionLost(cause);

		mLastReceivedMessageId = INVALID_MESSAGE_ID;

		if (mOTAImpl != null) {
			mOTAImpl.setSubscribedState(false);
		}
	}

	/**
	 * 收到MQTT消息
	 *
	 * @param topic
	 *            消息主题
	 * @param message
	 *            消息内容结构体
	 * @throws Exception
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		if (message.getQos() > 0 && message.getId() == mLastReceivedMessageId) {
			LOG.error("Received topic: %s, id: %d, message: %s, discard repeated message!!!" +  topic + message.getId() + 
					message);
			return;
		}

		LOG.info("Received topic: %s, id: %d, message: %s" + topic +  message.getId() + message);

		mLastReceivedMessageId = message.getId();

		boolean consumed = false;
		if (mOTAImpl != null) {
			consumed = mOTAImpl.processMessage(topic, message);
		}

		if (mActionCallBack != null) {
			if (!consumed) {
				mActionCallBack.onMessageReceived(topic, message);
			}
		}
	}

	/**
	 * 发布消息成功回调
	 *
	 * @param messageToken
	 *            消息内容Token
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken messageToken) {
		LOG.info("deliveryComplete, token.getMessageId:" + messageToken.getMessageId());
	}

	/**
	 * 获取连接ID（长度为5的数字字母随机字符串）
	 */
	protected String getConnectId() {
		StringBuffer connectId = new StringBuffer();
		for (int i = 0; i < TXMqttConstants.MAX_CONN_ID_LEN; i++) {
			int flag = (int) (Math.random() * Integer.MAX_VALUE) % 3;
			int randNum = (int) (Math.random() * Integer.MAX_VALUE);
			switch (flag) {
			case 0:
				connectId.append((char) (randNum % 26 + 'a'));
				break;
			case 1:
				connectId.append((char) (randNum % 26 + 'A'));
				break;
			case 2:
				connectId.append((char) (randNum % 10 + '0'));
				break;
			}
		}

		return connectId.toString();
	}

	/**
	 * 事件回调
	 */
	private class QcloudMqttActionListener implements IMqttActionListener {
		private int command;

		public QcloudMqttActionListener(int command) {
			this.command = command;
		}

		@Override
		public void onSuccess(IMqttToken token) {

			MqttWireMessage mqttWireMessage = token.getResponse();

			switch (command) {
			case TXMqttConstants.PUBLISH:
				mActionCallBack.onPublishCompleted(Status.OK, token, token.getUserContext(),
						TXMqttConstants.PUBLISH_SUCCESS);
				break;

			case TXMqttConstants.SUBSCRIBE:
				int[] qos = ((MqttSuback) mqttWireMessage).getGrantedQos();
				if (null != qos && qos.length >= 1 && qos[0] == 128) {
					mActionCallBack.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(),
							TXMqttConstants.SUBSCRIBE_FAIL);
				} else {
					mActionCallBack.onSubscribeCompleted(Status.OK, token, token.getUserContext(),
							TXMqttConstants.SUBSCRIBE_SUCCESS);

					if (mOTAImpl != null) {
						mOTAImpl.onSubscribeCompleted(Status.OK, token, token.getUserContext(),
								TXMqttConstants.SUBSCRIBE_SUCCESS);
					}
				}
				break;

			case TXMqttConstants.UNSUBSCRIBE:
				mActionCallBack.onUnSubscribeCompleted(Status.OK, token, token.getUserContext(),
						TXMqttConstants.UNSUBSCRIBE_SUCCESS);
				break;

			default:
				LOG.error("Unknown message on Success:" + token);
				break;
			}
		}

		@Override
		public void onFailure(IMqttToken token, Throwable exception) {
			switch (command) {
			case TXMqttConstants.PUBLISH:
				mActionCallBack.onPublishCompleted(Status.ERROR, token, token.getUserContext(), exception.toString());
				break;
			case TXMqttConstants.SUBSCRIBE:
				mActionCallBack.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(), exception.toString());
				break;
			case TXMqttConstants.UNSUBSCRIBE:
				mActionCallBack.onUnSubscribeCompleted(Status.ERROR, token, token.getUserContext(),
						exception.toString());
				break;
			default:
				LOG.error("Unknown message on onFailure:" + token);
				break;
			}
		}
	}
}
