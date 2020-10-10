package com.tencent.iot.explorer.device.android.mqtt;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

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

import java.util.HashMap;
import java.util.Iterator;

import static com.tencent.iot.explorer.device.android.mqtt.TXMqttConstants.DEFAULT_SERVER_URI;
import static com.tencent.iot.explorer.device.android.mqtt.TXMqttConstants.MQTT_SDK_VER;


public class TXMqttConnection implements MqttCallbackExtended {

    public static final String TAG = "TXMQTT_" + MQTT_SDK_VER;

    /**
     * tcp://localhost:port
     * ssl://localhost:port
     */
    public String mServerURI;
    public String mClientId;
    public String mProductId;
    public String mDeviceName;
    public String mUserName;
    public String mSecretKey;

    protected Context mContext;

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
     * @param context    用户上下文（这个参数在回调函数时透传给用户）
     * @param productID  产品名
     * @param deviceName 设备名，唯一
	 * @param secretKey  密钥
     * @param callBack   连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, null, callBack);
    }

    /**
     * @param context    用户上下文（这个参数在回调函数时透传给用户）
     * @param productID  产品名
     * @param deviceName 设备名，唯一
	 * @param secretKey  密钥
     * @param bufferOpts 发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param callBack   连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
	 * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        this(context, DEFAULT_SERVER_URI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI         服务器URI，腾讯云默认唯一地址 TXMqttConstants.DEFAULT_SERVER_URI="ssl://connect.iot.qcloud.com:8883"
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        this.mContext = context;
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
     * @param options     连接参数
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            TXLog.i(TAG, "The client is connecting. Connect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
            TXLog.i(TAG, "The client is already connected. Connect return directly.");
            return Status.OK;
        }

        this.mConnOptions = options;
        if (mConnOptions == null) {
            TXLog.e(TAG, "Connect options == null, will not connect.");
            return Status.PARAMETER_INVALID;
        }

        Long timestamp;
        if (options.isAutomaticReconnect()) {
            timestamp = (long) Integer.MAX_VALUE;
        } else {
            timestamp = System.currentTimeMillis()/1000 + 600;
        }
        String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;

        mConnOptions.setUserName(userNameStr);

        if (mSecretKey != null && mSecretKey.length() != 0) {
            try {
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(), Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
                mConnOptions.setPassword(passWordStr.toCharArray());
            }
            catch (IllegalArgumentException e) {
                Log.d(TAG, "Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                TXLog.i(TAG, "onSuccess!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + mServerURI);
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                TXLog.e(TAG, exception, "onFailure!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString());
            }
        };

        if (mMqttClient == null) {
            try {
                mPingSender = new TXAlarmPingSender(mContext);
                mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
                mMqttClient.setCallback(this);
                mMqttClient.setBufferOpts(this.bufferOpts);
                mMqttClient.setManualAcks(false);
            } catch (Exception e) {
                TXLog.e(TAG, "new MqttClient failed", e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        try {
            TXLog.i(TAG, "Start connecting to %s", mServerURI);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
            mMqttClient.connect(mConnOptions, userContext, mActionListener);
        } catch (Exception e) {
            TXLog.e(TAG, "MqttClient connect failed", e);
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
            TXLog.e(TAG, "Reconnect myClient = null. Will not do reconnect");
            return Status.MQTT_NO_CONN;
        }

        if (getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            TXLog.i(TAG, "The client is connecting. Reconnect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnOptions.isAutomaticReconnect() && !getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            TXLog.i(TAG, "Requesting Automatic reconnect using New Java AC");
            try {
                mMqttClient.reconnect();
            } catch (Exception ex) {
                TXLog.e(TAG, "Exception occurred attempting to reconnect: ", ex);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        } else if (getConnectStatus().equals(TXMqttConstants.ConnectStatus.kDisconnected) && !mConnOptions.isCleanSession()) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    TXLog.i(TAG, "onSuccess!");
                    //mActionCallBack.onConnectCompleted(Status.OK, true, asyncActionToken.getUserContext(), "reconnected to " + mServerURI);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    TXLog.e(TAG, exception, "onFailure!");
                    setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                    mActionCallBack.onConnectCompleted(Status.ERROR, true, asyncActionToken.getUserContext(), exception.toString());
                }
            };

            try {
                mMqttClient.connect(mConnOptions, null, listener);
                setConnectingState(TXMqttConstants.ConnectStatus.kDisconnected);
            } catch (Exception e) {
                TXLog.e(TAG, "Exception occurred attempting to reconnect: ", e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        return Status.OK;
    }

    /**
     * MQTT断连，结果通过回调函数通知。
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status disConnect(Object userContext) {
        return disConnect(0, userContext);
    }

    /**
     * MQTT断连, 结果通过回调函数通知。
     *
     * @param timeout     等待时间（必须大于0）。单位：毫秒
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
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
                    mActionCallBack.onDisconnectCompleted(Status.OK, asyncActionToken.getUserContext(), "disconnected to " + mServerURI);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
                    mActionCallBack.onDisconnectCompleted(Status.ERROR, asyncActionToken.getUserContext(), cause.toString());
                }
            };

            try {
                if (timeout <= 0) {
                    mMqttClient.disconnect(userContext, mActionListener);
                } else {
                    mMqttClient.disconnect(timeout, userContext, mActionListener);
                }
            } catch (MqttException e) {
                TXLog.e(TAG, e, "manual disconnect failed.");
                return Status.ERROR;
            }
        }

        return Status.ERROR;
    }

    /**
     * 发布MQTT消息接口, 结果通过回调函数通知。
     *
     * @param topic       topic名称
     * @param message     消息内容
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status publish(String topic, MqttMessage message, Object userContext) {
        IMqttDeliveryToken sendToken = null;

        if (topic == null || topic.trim().length() == 0) {
            TXLog.e(TAG, "Topic is empty!!!");
            return Status.PARAMETER_INVALID;
        }
        if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
            TXLog.e(TAG, "Topic length is too long!!!");
            return Status.PARAMETER_INVALID;
        }

        TXLog.i(TAG, "Starting publish topic: %s Message: %s", topic, message.toString());

        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            try {
                sendToken = mMqttClient.publish(topic, message, userContext, new QcloudMqttActionListener(TXMqttConstants.PUBLISH));
            } catch (Exception e) {
                TXLog.e(TAG, e, "publish topic: %s failed.", topic);
                return Status.ERROR;
            }
        } else if ((mMqttClient != null) && (this.bufferOpts != null) && (this.bufferOpts.isBufferEnabled())) { //放入缓存
            try {
                sendToken = mMqttClient.publish(topic, message, userContext, new QcloudMqttActionListener(TXMqttConstants.PUBLISH));
            } catch (Exception e) {
                TXLog.e(TAG, e, "publish topic: %s failed.", topic);
                return Status.ERROR;
            }
        } else {
            TXLog.e(TAG, "publish topic: %s failed, mMqttClient not connected and disconnect buffer not enough.", topic);
            return Status.ERROR;
        }

        return Status.OK;
    }

    /**
     * 订阅Topic, 结果通过回调函数通知。
     *
     * @param topic       topic名称
     * @param qos         QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status subscribe(final String topic, final int qos, Object userContext) {
        if (topic == null || topic.trim().length() == 0) {
            TXLog.e(TAG, "Topic is empty!!!");
            return Status.PARAMETER_INVALID;
        }
        if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
            TXLog.e(TAG, "Topic length is too long!!!");
            return Status.PARAMETER_INVALID;
        }

        TXLog.i(TAG, "Starting subscribe topic: %s", topic);

        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            try {
                mMqttClient.subscribe(topic, qos, userContext, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
            } catch (Exception e) {
                TXLog.e(TAG, e, "subscribe topic: %s failed.", topic);
                return Status.ERROR;
            }
        } else {
            TXLog.e(TAG, "subscribe topic: %s failed, because mMqttClient not connected.", topic);
            return Status.MQTT_NO_CONN;
        }

        mSubscribedTopicMap.put(topic, qos);

        return Status.OK;
    }

    /**
     * 取消订阅主题, 结果通过回调函数通知。
     *
     * @param topic       要取消订阅的主题
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status unSubscribe(final String topic, Object userContext) {
        if (topic == null || topic.trim().length() == 0) {
            TXLog.e(TAG, "Topic is empty!!!");
            return Status.PARAMETER_INVALID;
        }
        if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
            TXLog.e(TAG, "Topic length is too long!!!");
            return Status.PARAMETER_INVALID;
        }

        TXLog.i(TAG, "Starting unSubscribe topic: %s", topic);

        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            try {
                mMqttClient.unsubscribe(topic, userContext, new QcloudMqttActionListener(TXMqttConstants.UNSUBSCRIBE));
            } catch (Exception e) {
                TXLog.e(TAG, e, "unSubscribe topic: %s failed.", topic);
                return Status.ERROR;
            }
        } else {
            TXLog.e(TAG, "unSubscribe topic: %s failed, because mMqttClient not connected.", topic);
            return Status.MQTT_NO_CONN;
        }

        mSubscribedTopicMap.remove(topic);

        return Status.OK;
    }

    /**
     * 初始化OTA功能。
     *
     * @param storagePath OTA升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param callback    OTA事件回调
     */
    public void initOTA(String storagePath, TXOTACallBack callback) {
        mOTAImpl = new TXOTAImpl(this, storagePath, callback);
    }

    /**
     * 上报设备当前版本信息到后台服务器。
     *
     * @param currentFirmwareVersion 设备当前版本信息
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
     * @param state 状态
     * @param resultCode 结果代码。0：表示成功；其它：表示失败；常见错误码：-1: 下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
     * @param resultMsg 结果描述
     * @param version 版本号
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportOTAState(TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
        if (mOTAImpl != null) {
            return mOTAImpl.reportUpdateFirmwareState(state.toString().toLowerCase(), resultCode, resultMsg, version);
        }

        return  Status.ERROR;
    }

    /**
     * 设置当前连接状态
     *
     * @param connectStatus 当前连接状态
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
        TXLog.i(TAG, "connectComplete. reconnect flag is " + reconnect);
        setConnectingState(TXMqttConstants.ConnectStatus.kConnected);

        if (!reconnect) {
            return;
        }

        Iterator<String> it = mSubscribedTopicMap.keySet().iterator();
        while (it.hasNext()) {
            String topic = it.next();
            Integer qos = mSubscribedTopicMap.get(topic);
            try {
                TXLog.i(TAG, "subscribe to %s...", topic);
                mMqttClient.subscribe(topic, qos, null, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
            } catch (Exception e) {
                TXLog.e(TAG, "subscribe to %s failed.", topic);
            }
        }

        mActionCallBack.onConnectCompleted(Status.OK, reconnect, null, "connected to " + serverURI);

    }

    /**
     * 连接丢失，回调上层
     *
     * @param cause 连接断开原因
     */
    @Override
    public void connectionLost(Throwable cause) {
        TXLog.e(TAG, "connection lost because of: %s", cause.toString());
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
     * @param topic   消息主题
     * @param message 消息内容结构体
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (message.getQos() > 0 && message.getId() == mLastReceivedMessageId) {
            TXLog.e(TAG, "Received topic: %s, id: %d, message: %s, discard repeated message!!!", topic, message.getId(), message);
            return;
        }

        TXLog.i(TAG, "Received topic: %s, id: %d, message: %s", topic, message.getId(), message);

        mLastReceivedMessageId = message.getId();

        boolean consumed = false;
        if (mOTAImpl != null ) {
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
     * @param messageToken 消息内容Token
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken messageToken) {
        TXLog.i(TAG, "deliveryComplete, token.getMessageId:" + messageToken.getMessageId());
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
                    mActionCallBack.onPublishCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.PUBLISH_SUCCESS);
                    break;

                case TXMqttConstants.SUBSCRIBE:
                    int[] qos = ((MqttSuback) mqttWireMessage).getGrantedQos();
                    if (null != qos && qos.length >= 1 && qos[0] == 128) {
                        mActionCallBack.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(), TXMqttConstants.SUBSCRIBE_FAIL);
                    } else {
                        mActionCallBack.onSubscribeCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.SUBSCRIBE_SUCCESS);

                        if (mOTAImpl != null) {
                            mOTAImpl.onSubscribeCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.SUBSCRIBE_SUCCESS);
                        }
                    }
                    break;

                case TXMqttConstants.UNSUBSCRIBE:
                    mActionCallBack.onUnSubscribeCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.UNSUBSCRIBE_SUCCESS);
                    break;

                default:
                    TXLog.e(TAG, "Unknown message on Success:" + token);
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
                    mActionCallBack.onUnSubscribeCompleted(Status.ERROR, token, token.getUserContext(), exception.toString());
                    break;
                default:
                    TXLog.e(TAG, "Unknown message on onFailure:" + token);
                    break;
            }
        }
    }

}
