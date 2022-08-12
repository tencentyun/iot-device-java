package com.tencent.iot.hub.device.android.core.mqtt;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SERVER_PORT_TID;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.QCLOUD_IOT_MQTT_DIRECT_DOMAIN;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.TID_PREFIX;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLog;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;
import com.tencent.iot.hub.device.java.core.ssh.MqttSshProxy;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * MQTT 连接类
 */
public class TXMqttConnection extends com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection implements MqttCallbackExtended {

    /**
     * 类标记
     */
    private static final String TAG = "TXMQTT_" + MQTT_SDK_VER;

    /**
     * tcp://localhost:port ssl://localhost:port tid://localhost:port
     */
    protected Context mContext;
    protected TXAlarmPingSender mPingSender = null;
    private String mTid;
    private String mHid;

    /**
     * 断连状态下buffer缓冲区，当连接重新建立成功后自动将buffer中数据写出
     */
    protected DisconnectedBufferOptions bufferOpts = null;

    protected volatile TXMqttConstants.ConnectStatus mConnectStatus = TXMqttConstants.ConnectStatus.kConnectIdle;

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
	 * @param secretKey 设备密钥
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
	 * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * 构造函数，使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
	 * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        this(context, null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,TXMqttActionCallBack callBack) {
        this(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, false, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
	 * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param logCallBack 日子上传回调接口 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttLogCallBack logCallBack,TXMqttActionCallBack callBack) {
        this(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, true, logCallBack, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param mqttLogFlag 是否开启日志功能
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mContext = context;
        this.mMqttLogFlag = mqttLogFlag;
        this.mMqttLogCallBack = logCallBack;
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param mqttLogFlag 是否开启日志功能
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 url
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl) {
        super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack, logUrl);
        this.mContext = context;
        this.mMqttLogFlag = mqttLogFlag;
        this.mMqttLogCallBack = logCallBack;
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param mqttLogFlag 是否开启日志功能
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 url
     * @param sshHost ssh 要访问的IP
     * @param sshPort ssh 端口号
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl, String sshHost, int sshPort) {
        super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack, logUrl);
        this.mContext = context;
        this.mMqttLogFlag = mqttLogFlag;
        this.mMqttLogCallBack = logCallBack;
        this.sshHost = sshHost;
        this.sshPort = sshPort;
    }

    /**
     * 设置 TrustID 和 HardwareID
     *
     * @param tid trust id
     * @param hid hardware id
     */
    public void setTidAndHid(String tid, String hid) {
        mTid = tid;
        mHid = hid;
        if (!TextUtils.isEmpty(mTid) && !TextUtils.isEmpty(mHid)) {
            mClientId = String.format("%s;%s;%s", mClientId, mTid, mHid);
            mServerURI = TID_PREFIX + mProductId + QCLOUD_IOT_MQTT_DIRECT_DOMAIN + MQTT_SERVER_PORT_TID;
        }
    }

    /**
     * 连接 MQTT 服务器，结果通过回调函数通知，结果通过回调函数通知，无需设置 username 和 password，内部会自动填充
     *
     * @param options 连接参数 {@link MqttConnectOptions}
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
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
                TXLog.d(TAG, "Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                TXLog.i(TAG, "onSuccess!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + mServerURI, null);

                // 连接建立后，如果需要日志，则初始化日志功能
                if (mMqttLogFlag) {
                    initMqttLog(TAG);
                }
                if (sshHost != null && !sshHost.equals("")) {// 用户上下文（请求实例）
                    subscribeNTPTopic(TXMqttConstants.QOS1, null);
                }
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                TXLog.e(TAG, exception, "onFailure!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString(), exception);
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
     * 初始化日志上传功能
     *
     * @param tag 日志标记
     */
    protected void initMqttLog(final String tag) {
        if (mMqttLog == null) {
            if (mLogUrl != null) {
                this.mMqttLog = new TXMqttLog(this, mLogUrl);
            } else {
                this.mMqttLog = new TXMqttLog(this);
            }
        }

        if (Status.OK != mMqttLog.initMqttLog()){
            TXLog.i(tag,"Init MqttLog failed!" );
        }
    }

    /**
     * 生成一条设备日志
     *
     * @param logLevel 日志级别：
     *                 MQTT错误：TXMqttLogConstants.LEVEL_FATAL    
     *                 错误：TXMqttLogConstants.LEVEL_ERROR
     *                 警告：TXMqttLogConstants.LEVEL_WARN
     *                 通知：TXMqttLogConstants.LEVEL_INFO
     *                 调试：TXMqttLogConstants.LEVEL_DEBUG
     * @param tag 日志标记
     * @param format 日志格式
     * @param obj 日志内容
     */
    public void mLog(int logLevel, final String tag,final String format, final Object... obj) {
        if( mMqttLog != null) {
            if( !(mMqttLog.saveMqttLog(logLevel, tag, format, obj))) {
                TXLog.w(tag, "Save %s Level Log failed!", TXMqttLog.level_str[logLevel] );
            }
        }
    }

    /**
     * 生成一条设备日志
     *
     * @param logLevel 日志级别：
     *                 MQTT错误：TXMqttLogConstants.LEVEL_FATAL
     *                 错误：TXMqttLogConstants.LEVEL_ERROR
     *                 警告：TXMqttLogConstants.LEVEL_WARN
     *                 通知：TXMqttLogConstants.LEVEL_INFO
     *                 调试：TXMqttLogConstants.LEVEL_DEBUG
     * @param tag 日志标记
     * @param msg 日志内容
     */
    public void mLog(int logLevel, final String tag, final String msg) {
        if( mMqttLog != null) {
            if( !(mMqttLog.saveMqttLog(logLevel, tag, msg))) {
                TXLog.w(tag, "Save %s Level Log failed!", TXMqttLog.level_str[logLevel] );
            }
        }
    }

    /**
     * 触发一次日志上传
     */
    public void uploadLog() {
        if(mMqttLog != null) {
            mMqttLog.uploadMqttLog();
        }
    }

    /**
     * 订阅 RRPC Topic, 结果通过回调函数通知。topic格式: $rrpc/rxd/${ProductId}/${DeviceName}/+
     *
     * @param qos QOS等级(仅支持 QOS=0 的消息)
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status subscribeRRPCTopic(final int qos, Object userContext) {
        String topic = String.format("$rrpc/rxd/%s/%s/+", mProductId, mDeviceName);
        return subscribe(topic, qos, userContext);
    }


    public Status publishRRPCToCloud(Object userContext, String processId, Map<String, String> replyMsg) {
        // 应答topic格式: $rrpc/txd/${ProductId}/${DeviceName}/${messageid}
        String topic  = String.format("$rrpc/txd/%s/%s/%s", mProductId, mDeviceName, processId);
        MqttMessage message = new MqttMessage();
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entrys : replyMsg.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            TXLog.e(TAG, e, "pack json data failed!");
        }
        message.setQos(TXMqttConstants.QOS0);
        message.setPayload(jsonObject.toString().getBytes());
        return publish(topic, message ,userContext);
    }


    /**
     * 订阅广播 Topic, 结果通过回调函数通知。广播 Topic 格式: $broadcast/rxd/${ProductId}/${DeviceName}
     *
     * @param qos QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status subscribeBroadcastTopic(final int qos, Object userContext) {
        String broadCastTopic = "";
        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            broadCastTopic = String.format("$broadcast/rxd/%s/%s", mProductId, mDeviceName);
            try {
                mMqttClient.subscribe(broadCastTopic, qos ,userContext, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
            } catch (Exception e) {
                TXLog.e(TAG, e, "subscribe topic: %s failed.", broadCastTopic);
                mLog(TXMqttLogConstants.LEVEL_FATAL, TAG, "subscribe topic: %s failed.", broadCastTopic);
                return Status.ERROR;
            }
        } else {
            TXLog.e(TAG, "subscribe topic: %s failed, because mMqttClient not connected.", broadCastTopic);
            mLog(TXMqttLogConstants.LEVEL_FATAL, TAG, "subscribe topic: %s failed, because mMqttClient not connected.", broadCastTopic);
            return Status.MQTT_NO_CONN;
        }
        return Status.OK;
    }

    /**
     * 连接完成
     *
     * @param reconnect 重连标记
     * @param serverURI 服务器 URI
     */
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
                mLog(TXMqttLogConstants.LEVEL_FATAL, TAG,"subscribe to %s failed.", topic);
            }
        }

        mActionCallBack.onConnectCompleted(Status.OK, reconnect, null, "connected to " + serverURI, null);

        //重新连接，处理离线日志，重新获取日志级别
        if (mMqttLogFlag) {
            initMqttLog(TAG);
        }
    }

    /**
     * 收到 MQTT 消息
     *
     * @param topic 消息主题
     * @param message 消息内容结构体 {@link MqttMessage}
     * @throws Exception 抛出异常 {@link Exception}
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (message.getQos() > 0 && message.getId() == mLastReceivedMessageId) {
            TXLog.e(TAG, "Received topic: %s, id: %d, message: %s, discard repeated message!!!", topic, message.getId(), message);
            mLog(TXMqttLogConstants.LEVEL_FATAL, TAG,"Received topic: %s, id: %d, message: %s, discard repeated message!!!", topic, message.getId(), message);
            return;
        }

        TXLog.i(TAG, "Received topic: %s, id: %d, message: %s", topic, message.getId(), message);

        if (topic != null && topic.contains("rrpc/rxd")) {
            String[] items = topic.split("/");
            String processId = items[items.length-1];
            TXLog.d(TAG, "Call publishRRPCToCloud method to reply when a rrpc msg received in onMessageArrived method, args: processId=%s, replyMsg=user-define", processId);
        }

        if (topic != null && topic.contains("sys/operation/result/")) {
            String jsonStr = new String(message.getPayload());

            try {
                JSONObject jsonObj = new JSONObject(jsonStr);

                if (jsonObj.has("type")) {
                    String type = jsonObj.getString("type");
                    if (type.equals("ssh")) {
                        Integer ssh_switch = jsonObj.getInt("switch");
                        if (ssh_switch == 1) {
                            if (mqttSshProxy == null) {
                                mqttSshProxy = new MqttSshProxy(this, this.sshHost, this.sshPort);
                            }
                        } else {
                            if (mqttSshProxy != null) {
                                mqttSshProxy.stopWebsocketSshPing();
                                mqttSshProxy = null;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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

        //判断获取日志等级
        if (mMqttLog != null) {
            if (topic.startsWith("$" + TXMqttLogConstants.LOG)) {
                String jsonStr = new String(message.getPayload());

                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    if (jsonObj.has(TXMqttLogConstants.LOG_LEVEL)) {
                        int logLevel = jsonObj.getInt(TXMqttLogConstants.LOG_LEVEL);
                        mMqttLog.setMqttLogLevel(logLevel);
                        uploadLog();
                        TXLog.d(TAG, "******Set mqttLogLevel to " + logLevel);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            TXLog.d(TAG, "******Get mqttLogLevel failed ");
        }
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
                    mActionCallBack.onPublishCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.PUBLISH_SUCCESS, null);
                    break;

                case TXMqttConstants.SUBSCRIBE:
                    int[] qos = ((MqttSuback) mqttWireMessage).getGrantedQos();
                    if (null != qos && qos.length >= 1 && qos[0] == 128) {
                        mActionCallBack.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(), TXMqttConstants.SUBSCRIBE_FAIL, new Throwable("qos don't support"));
                    } else {
                        mActionCallBack.onSubscribeCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.SUBSCRIBE_SUCCESS, null);

                        if (mOTAImpl != null) {
                            mOTAImpl.onSubscribeCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.SUBSCRIBE_SUCCESS);
                        }
                    }
                    break;

                case TXMqttConstants.UNSUBSCRIBE:
                    mActionCallBack.onUnSubscribeCompleted(Status.OK, token, token.getUserContext(), TXMqttConstants.UNSUBSCRIBE_SUCCESS, null);
                    break;

                default:
                    TXLog.e(TAG, "Unknown message on Success:" + token);
                    mLog(TXMqttLogConstants.LEVEL_FATAL, TAG,"Unknown message on Success:" + token);
                    break;
            }
        }

        @Override
        public void onFailure(IMqttToken token, Throwable exception) {
            switch (command) {
                case TXMqttConstants.PUBLISH:
                    mActionCallBack.onPublishCompleted(Status.ERROR, token, token.getUserContext(), exception.toString(), exception);
                    break;
                case TXMqttConstants.SUBSCRIBE:
                    mActionCallBack.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(), exception.toString(), exception);
                    break;
                case TXMqttConstants.UNSUBSCRIBE:
                    mActionCallBack.onUnSubscribeCompleted(Status.ERROR, token, token.getUserContext(), exception.toString(), exception);
                    break;
                default:
                    TXLog.e(TAG, "Unknown message on onFailure:" + token);
                    mLog(TXMqttLogConstants.LEVEL_FATAL, TAG,"Unknown message on onFailure:" + token);
                    break;
            }
        }
    }

}
