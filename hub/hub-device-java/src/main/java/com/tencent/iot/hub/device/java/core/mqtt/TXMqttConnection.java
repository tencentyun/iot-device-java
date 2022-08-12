package com.tencent.iot.hub.device.java.core.mqtt;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.CER_PREFIX;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SERVER_PORT_CER;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SERVER_PORT_PSK;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.PSK_PREFIX;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.QCLOUD_IOT_MQTT_DIRECT_DOMAIN;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.log.TXMqttLog;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogConstants;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;
import com.tencent.iot.hub.device.java.core.ssh.MqttSshProxy;
import com.tencent.iot.hub.device.java.utils.Loggor;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * MQTT 连接类
 */
public class TXMqttConnection implements MqttCallbackExtended {
    private static final String TAG = "TXMQTT_" + MQTT_SDK_VER;
    private static final Logger logger = LoggerFactory.getLogger(TXMqttConnection.class);
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String PRODUCT_CONFIG_PREFIX = "$config/operation/result/";
    private static final String NTP_OPERATION_RES_PREFIX = "$sys/operation/result/";
    private static final String NTP_OPERATION_PREFIX = "$sys/operation/";
    static { Loggor.setLogger(logger); }
    private String subDevVersion = "0.0"; // 未设置，则默认当前的版本是 0.0  用于上报版本号
    /**
     * 服务器 URI，tcp://localhost:port ssl://localhost:port
     */
    public String mServerURI;
    /**
     * 客户端段 ID
     */
    public String mClientId;
    /**
     * 产品 ID
     */
    public String mProductId;
    /**
     * 设备名
     */
    public String mDeviceName;
    /**
     * 用户名
     */
    public String mUserName;
    /**
     * 密钥
     */
    public String mSecretKey;
    /**
     * 日志 URL
     */
    public String mLogUrl;
    /**
     * ssh websocket URL
     */
    public String mSshWsUrl;

    private String mSubProductID;
    private String mSubDevName;
    private String mSubDevProductKey;

    protected MqttClientPersistence mMqttPersist = null;
    protected MqttConnectOptions mConnOptions = null;

    protected MqttAsyncClient mMqttClient = null;

    protected TXAlarmPingSender mPingSender = null;
    protected TXMqttActionCallBack mActionCallBack = null;

    protected HashMap<String, Integer> mSubscribedTopicMap = new HashMap<>();

    private static int INVALID_MESSAGE_ID = -1;
    protected int mLastReceivedMessageId = INVALID_MESSAGE_ID;

    protected TXOTAImpl mOTAImpl = null;


    protected boolean mMqttLogFlag;
    /**
     * MQTT 日志回调接口 {@link TXMqttLogCallBack}
     */
    public TXMqttLogCallBack mMqttLogCallBack = null;
    protected TXMqttLog mMqttLog = null;

    // ssh 要访问的IP
    public String sshHost;
    // ssh 端口号 sshd服务一般默认端口22
    public int sshPort;

    public MqttSshProxy mqttSshProxy = null;

    /**
     * 设置日志Flag
     *
     * @param value
     */
    public void setMqttLogFlag(Boolean value) {
        this.mMqttLogFlag = value;
    }

    /**
     * 设置日志回调
     *
     * @param mMqttLogCallBack {@link TXMqttLogCallBack}
     */
    public void setmMqttLogCallBack(TXMqttLogCallBack mMqttLogCallBack) {
        this.mMqttLogCallBack = mMqttLogCallBack;
    }

    /**
     * 获取子设备版本号
     *
     * @return 子设备版本号
     */
    public String getSubDevVersion() {
        return subDevVersion;
    }

    /**
     * 设置子设备版本号
     *
     * @param version 版本号
     */
    public void setSubDevVersion(String version) {
        this.subDevVersion = version;
    }

    /**
     * 设置子产品 ID
     *
     * @param subProductID 子产品 ID
     */
    public void setSubProductID(String subProductID) {
        mSubProductID = subProductID;
    }

    /**
     * 获取子产品 ID
     *
     * @return 子产品 ID
     */
    public String getSubProductID() {
        return mSubProductID;
    }

    /**
     * 设置子设备名
     *
     * @param subDevName 子设备名
     */
    public void setSubDevName(String subDevName) {
        this.mSubDevName = subDevName;
    }

    /**
     * 获取子设备名
     *
     * @return 子设备名
     */
    public String getSubDevName() {
        return mSubDevName;
    }

    /**
     * 设置子设备密钥
     *
     * @param subDevKey 子设备密钥
     */
    public void setSubDevProductKey(String subDevKey) {
        this.mSubDevProductKey = subDevKey;
    }

    /**
     * 获取子设备密钥
     *
     * @return 子设备密钥
     */
    public String getSubDevProductKey() {
        return mSubDevProductKey;
    }

    /**
     * 断连状态下buffer缓冲区，当连接重新建立成功后自动将buffer中数据写出
     */
    protected DisconnectedBufferOptions bufferOpts = null;

    protected volatile TXMqttConstants.ConnectStatus mConnectStatus = TXMqttConstants.ConnectStatus.kConnectIdle;

    /** 构造函数
     *
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String productID, String deviceName, String secretKey,
            TXMqttActionCallBack callBack) {
        this(productID, deviceName, secretKey, null, callBack);
    }

    /** 构造函数
     *
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String productID, String deviceName,
            TXMqttActionCallBack callBack) {
        this(productID, deviceName, null, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String productID, String deviceName, String secretKey,
            DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
        this(productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * 构造函数，使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
     *
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String productID, String deviceName, String secretKey,
            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
            TXMqttActionCallBack callBack) {
        this(null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,
            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
            TXMqttActionCallBack callBack) {
        this.mSecretKey = secretKey;
        if (serverURI == null) {
            if (this.mSecretKey != null && this.mSecretKey.length() != 0) {
                this.mServerURI = PSK_PREFIX + productID + QCLOUD_IOT_MQTT_DIRECT_DOMAIN + MQTT_SERVER_PORT_PSK;
            } else {
                this.mServerURI = CER_PREFIX + productID + QCLOUD_IOT_MQTT_DIRECT_DOMAIN + MQTT_SERVER_PORT_CER;
            }
        } else {
            this.mServerURI = serverURI;
        }
        this.mProductId = productID;
        this.mClientId = productID + deviceName;
        this.mDeviceName = deviceName;
        this.mUserName = mClientId + ";" + TXMqttConstants.APPID;
        this.bufferOpts = bufferOpts;
        this.mMqttPersist = clientPersistence;
        this.mActionCallBack = callBack;
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 URL
     */
    public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
                            TXMqttActionCallBack callBack, String logUrl) {

        this(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mLogUrl = logUrl;
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param logCallBack 日子上传回调接口 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        this(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, true, logCallBack, callBack);
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        this(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mMqttLogFlag = mqttLogFlag;
        this.mMqttLogCallBack = logCallBack;
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 url
     */
    public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl) {
        this(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack, logUrl);
        this.mMqttLogFlag = mqttLogFlag;
        this.mMqttLogCallBack = logCallBack;
    }

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 url
     * @param sshHost ssh 要访问的IP
     * @param sshPort ssh 端口号
     */
    public TXMqttConnection(String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl, String sshHost, int sshPort) {
        this(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack, logUrl);
        this.mMqttLogFlag = mqttLogFlag;
        this.mMqttLogCallBack = logCallBack;
        this.sshHost = sshHost;
        this.sshPort = sshPort;
    }

    /**
     * 设置断连状态 buffer 缓冲区
     *
     * @param bufferOpts {@link DisconnectedBufferOptions}
     */
    public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
        this.bufferOpts = bufferOpts;
        mMqttClient.setBufferOpts(bufferOpts);
    }

    /**
     * 连接 MQTT 服务器，结果通过回调函数通知，无需设置 username 和 password，内部会自动填充
     *
     * @param options 连接参数
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败；
     */
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
            Loggor.error(TAG,  "Connect options == null, will not connect.");
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

        if (mSecretKey != null && mSecretKey.length() != 0) {
            try {
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(),
                        Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
                mConnOptions.setPassword(passWordStr.toCharArray());
            } catch (IllegalArgumentException e) {
                Loggor.debug(TAG,  "Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                Loggor.info(TAG, "onSuccess! hashcode: " + System.identityHashCode(this));
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(),
                        "connected to " + mServerURI, null);
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
                Loggor.error(TAG,  exception + "onFailure!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString(), exception);
            }
        };

        if (mMqttClient == null) {
            try {
                mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist);
                mMqttClient.setCallback(this);
                mMqttClient.setBufferOpts(this.bufferOpts);
                mMqttClient.setManualAcks(false);
            } catch (Exception e) {
                Loggor.error(TAG,  "new MqttClient failed " + e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        try {
            IMqttToken token;
            Loggor.info(TAG, "Start connecting to " + mServerURI);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
            token = mMqttClient.connect(mConnOptions, userContext, mActionListener);
            token.waitForCompletion(-1);
            Loggor.info(TAG, "wait_for completion return");
        } catch (Exception e) {
            Loggor.error(TAG,  "MqttClient connect failed " + e);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
            return Status.ERROR;
        }

        return Status.OK;
    }

    /**
     * 重新连接, 结果通过回调函数通知
     *
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败；
     */
    public synchronized Status reconnect() {
        if (mMqttClient == null) {
            Loggor.error(TAG,  "Reconnect myClient = null. Will not do reconnect");
            return Status.MQTT_NO_CONN;
        }

        if (getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            Loggor.info(TAG, "The client is connecting. Reconnect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnOptions.isAutomaticReconnect()
                && !getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            Loggor.info(TAG, "Requesting Automatic reconnect using New Java AC");
            try {
                mMqttClient.reconnect();
            } catch (Exception ex) {
                Loggor.error(TAG,  "Exception occurred attempting to reconnect: " + ex);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        } else if (getConnectStatus().equals(TXMqttConstants.ConnectStatus.kDisconnected)
                && !mConnOptions.isCleanSession()) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Loggor.info(TAG, "onSuccess!");
                    // mActionCallBack.onConnectCompleted(Status.OK, true,
                    // asyncActionToken.getUserContext(), "reconnected to " +
                    // mServerURI);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Loggor.error(TAG,  exception+"onFailure!");
                    setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                    mActionCallBack.onConnectCompleted(Status.ERROR, true, asyncActionToken.getUserContext(),
                            exception.toString(), exception);
                }
            };

            try {
                mMqttClient.connect(mConnOptions, null, listener);
                setConnectingState(TXMqttConstants.ConnectStatus.kDisconnected);
            } catch (Exception e) {
                Loggor.error(TAG,  "Exception occurred attempting to reconnect: " + e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        return Status.OK;
    }

    /**
     * MQTT 断连，结果通过回调函数通知
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK；其它返回值表示发送请求失败；
     */
    public Status disConnect(Object userContext) {
        return disConnect(0, userContext);
    }

    /**
     * MQTT 断连, 结果通过回调函数通知
     *
     * @param timeout 等待时间（必须大于0）。单位：毫秒
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败；
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
                            "disconnected to " + mServerURI, null);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
                    mActionCallBack.onDisconnectCompleted(Status.ERROR, asyncActionToken.getUserContext(),
                            cause.toString(), cause);
                }
            };

            try {
                if (timeout <= 0) {
                    mMqttClient.disconnect(userContext, mActionListener);
                } else {
                    mMqttClient.disconnect(timeout, userContext, mActionListener);
                }
            } catch (MqttException e) {
                Loggor.error(TAG,  e + "manual disconnect failed.");
                return Status.ERROR;
            }
        }

        return Status.ERROR;
    }

    /**
     * 发布 MQTT 消息接口, 结果通过回调函数通知
     *
     * @param topic topic名称
     * @param message 消息内容
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status publish(String topic, MqttMessage message, Object userContext) {
        IMqttDeliveryToken sendToken = null;

        if (topic == null || topic.trim().length() == 0) {
            Loggor.error(TAG,  "Topic is empty!!!");
            return Status.PARAMETER_INVALID;
        }
        if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
            Loggor.error(TAG,  "Topic length is too long!!!");
            return Status.PARAMETER_INVALID;
        }

        Loggor.info(TAG, String.format("Starting publish topic: %s Message: %s", topic, message.toString()));
        Loggor.debug(TAG, "topic = " + topic);
        Loggor.debug(TAG, "message.toString() = " + message.toString());
        //Loggor.debug(TAG, "mMqttClient.isConnected() = " + mMqttClient.isConnected());
        
        Loggor.debug(TAG, "mMqttClient != null = " + (mMqttClient != null));
        
        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            try {
                sendToken = mMqttClient.publish(topic, message, userContext,
                        new QcloudMqttActionListener(TXMqttConstants.PUBLISH));
            } catch (Exception e) {
                Loggor.error(TAG,  e + "publish topic: " + topic + " failed1.");
                return Status.ERROR;
            }
        } else if ((mMqttClient != null) && (this.bufferOpts != null) && (this.bufferOpts.isBufferEnabled())) { // 放入缓存
            try {
                sendToken = mMqttClient.publish(topic, message, userContext,
                        new QcloudMqttActionListener(TXMqttConstants.PUBLISH));
            } catch (Exception e) {
                Loggor.error(TAG,  e + "publish topic: " + topic + " failed2.");
                return Status.ERROR;
            }
        } else {
            Loggor.debug(TAG, "1111111111111111111111111111 topic = " + topic);
            Loggor.error(TAG, String.format("publish topic: %s failed, mMqttClient not connected and disconnect buffer not enough.", topic));
            return Status.ERROR;
        }

        return Status.OK;
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
        return publish(topic, message, null);
    }

    /**
     * 关注远程配置变化
     *
     * @return 操作结果 {@link Status}
     */
    public Status concernConfig() {
        String subscribeConfigTopic = PRODUCT_CONFIG_PREFIX + mProductId + "/" + mDeviceName;
        return this.subscribe(subscribeConfigTopic, 1, "subscribe config topic");
    }

    /**
     * 获取网关子设备拓扑关系
     *
     * @return 操作结果 {@link Status}
     */
    public Status gatewayGetSubdevRelation() {

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "describe_sub_devices");
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        // 这里添加获取到的数据
        message.setPayload(obj.toString().getBytes());
        message.setQos(1);
        String topic = String.format("$gateway/operation/%s/%s", mProductId, mDeviceName);
        Loggor.debug(TAG, "topic=" + topic);
        return publish(topic, message, null);
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
            String signStr = sign(text2Sgin, psk);
            dev.put("signature", signStr);
            JSONArray devs = new JSONArray();
            devs.put(dev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        // 这里添加获取到的数据
        message.setPayload(obj.toString().getBytes());
        message.setQos(1);
        String topic = String.format("$gateway/operation/%s/%s", mProductId, mDeviceName);
        Loggor.debug(TAG, "topic=" + topic);
        return publish(topic, message, null);
    }

    /**
     * 签名
     *
     * @param src 签名源
     * @param psk 签名密钥
     * @return 签名后的内容
     */
    private String sign(String src, String psk) {
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
            hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
            return hmacSign;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 网关解绑子设备
     *
     * @param subProductID 子产品 ID
     * @param subDeviceName 子设备名
     * @return 操作结果 {@link Status}
     */
    public Status gatewayUnbindSubdev(String subProductID, String subDeviceName) {

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
        // 这里添加获取到的数据
        message.setPayload(obj.toString().getBytes());
        message.setQos(1);
        String topic = String.format("$gateway/operation/%s/%s", mProductId, mDeviceName);
        Loggor.debug(TAG, "topic=" + topic);
        return publish(topic, message, null);
    }

    /**
     * 订阅 Topic, 结果通过回调函数通知
     *
     * @param topic topic 名称
     * @param qos QOS 等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status subscribe(final String topic, final int qos, Object userContext) {
        if (topic == null || topic.trim().length() == 0) {
            Loggor.error(TAG,  "Topic is empty!!!");
            return Status.PARAMETER_INVALID;
        }
        if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
            Loggor.error(TAG,  "Topic length is too long!!!");
            return Status.PARAMETER_INVALID;
        }

        Loggor.info(TAG, "Starting subscribe topic: " + topic);

        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            try {
                mMqttClient.subscribe(topic, qos, userContext, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
            } catch (Exception e) {
                Loggor.error(TAG,  String.format(e + "subscribe topic: %s failed.", topic));
                return Status.ERROR;
            }
        } else {
            Loggor.error(TAG,  String.format("subscribe topic: %s failed, because mMqttClient not connected.", topic));
            return Status.MQTT_NO_CONN;
        }

        mSubscribedTopicMap.put(topic, qos);

        return Status.OK;
    }

    /**
     * 取消订阅主题, 结果通过回调函数通知
     *
     * @param topic 要取消订阅的主题
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status unSubscribe(final String topic, Object userContext) {
        if (topic == null || topic.trim().length() == 0) {
            Loggor.error(TAG,  "Topic is empty!!!");
            return Status.PARAMETER_INVALID;
        }
        if (topic.length() > TXMqttConstants.MAX_SIZE_OF_CLOUD_TOPIC) {
            Loggor.error(TAG,  "Topic length is too long!!!");
            return Status.PARAMETER_INVALID;
        }

        Loggor.info(TAG, "Starting unSubscribe topic: " + topic);

        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            try {
                mMqttClient.unsubscribe(topic, userContext, new QcloudMqttActionListener(TXMqttConstants.UNSUBSCRIBE));
            } catch (Exception e) {
                Loggor.error(TAG,  String.format(e + "unSubscribe topic: %s failed.", topic));
                return Status.ERROR;
            }
        } else {
            Loggor.error(TAG,  String.format("unSubscribe topic: %s failed, because mMqttClient not connected.", topic));
            return Status.MQTT_NO_CONN;
        }

        mSubscribedTopicMap.remove(topic);

        return Status.OK;
    }

    /**
     * 初始化 OTA 功能
     *
     * @param storagePath OTA 升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param callback OTA事件回调
     */
    public void initOTA(String storagePath, TXOTACallBack callback) {
        mOTAImpl = new TXOTAImpl(this, storagePath, callback);
    }

    /**
     * 初始化 OTA 功能
     *
     * @param storagePath OTA 升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param cosServerCaCrtList OTA 升级包下载服务器的 CA 证书链
     * @param callback OTA 事件回调
     */
    public void initOTA(String storagePath, String[] cosServerCaCrtList, TXOTACallBack callback) {
        mOTAImpl = new TXOTAImpl(this, storagePath, cosServerCaCrtList, callback);
    }

    /**
     * 上报设备当前版本信息到后台服务器
     *
     * @param currentFirmwareVersion 设备当前版本信息
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status reportCurrentFirmwareVersion(String currentFirmwareVersion) {
        if (mOTAImpl != null && currentFirmwareVersion != null) {
            return mOTAImpl.reportCurrentFirmwareVersion(currentFirmwareVersion);
        }

        return Status.ERROR;
    }

    /**
     * 停止下载ota固件
     */
    public void stopDownloadOTATask () {
        if (mOTAImpl != null) {
            mOTAImpl.stopDownloadOTATask();
        }
    }

    /**
     * 网关子设备上报版本号
     *
     * @param currentVersion 子设备固件版本号
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevReportVer(String currentVersion) {
        if (mOTAImpl != null && currentVersion != null) {
            return mOTAImpl.gatewaySubdevReportVer(currentVersion);
        }

        return Status.ERROR;
    }

    /**
     * 子设备上报默认的固件版本
     *
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevReportVer() {
        if (mOTAImpl != null && getSubDevVersion() != null) {
            return mOTAImpl.gatewaySubdevReportVer(getSubDevVersion());
        }

        return Status.ERROR;
    }

    /**
     * 网关上报子设备固件下载进度
     *
     * @param firmwareURL 固件 URL
     * @param outputFile 输出文件
     * @param md5Sum md5 值
     * @param version 子设备固件版本号
     */
    public void gatewayDownSubdevApp(String firmwareURL, String outputFile, String md5Sum, String version) {
        if (mOTAImpl != null && getSubDevVersion() != null) {
            mOTAImpl.gatewayDownSubdevApp(firmwareURL, outputFile, md5Sum, version);
        }
    }

    /**
     * 网关上报子设备固件下载进度
     *
     * @param percent 当前进度
     * @param targetVersion 子设备固件版本号
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevReportProgress(int percent, String targetVersion) {
        if (mOTAImpl != null && getSubDevVersion() != null) {
            return mOTAImpl.gatewaySubdevReportProgress(percent, targetVersion);
        }

        return Status.ERROR;
    }

    /**
     * 子设备上报固件开始升级
     *
     * @param targetVersion 子设备固件目标版本号
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevReportStart(String targetVersion) {
        if (mOTAImpl != null && getSubDevVersion() != null) {
            return mOTAImpl.reportBurnngMessage(targetVersion);
        }

        return Status.ERROR;
    }

    /**
     * 子设备上报固件升级成功
     *
     * @param targetVersion 子设备固件版本号
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevReportSuccess(String targetVersion) {
        if (mOTAImpl != null && getSubDevVersion() != null) {
            return mOTAImpl.reportSuccessMessage(targetVersion);
        }

        return Status.ERROR;
    }

    /**
     * 上报子设备升级过程中的失败原因
     *
     * @param errorCode 错误码
     * @param errorMsg 错误信息
     * @param targetVersion 子设备固件版本号
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevReportFail(int errorCode, String errorMsg,String targetVersion) {
        if (mOTAImpl != null && getSubDevVersion() != null) {
            return mOTAImpl.reportFailedMessage(errorCode, errorMsg, targetVersion);
        }

        return Status.ERROR;
    }

    /**
     * 上报设备升级状态到后台服务器
     *
     * @param state 状态
     * @param resultCode 结果代码。0：表示成功；
     *                   其它：表示失败；常见错误码：
     *                   -1:下载超时；
     *                   -2:文件不存在；
     *                   -3:签名过期；
     *                   -4:校验错误；
     *                   -5:更新固件失败
     * @param resultMsg 结果描述
     * @param version 子设备固件版本号
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status reportOTAState(TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
        if (mOTAImpl != null) {
            return mOTAImpl.reportUpdateFirmwareState(state.toString().toLowerCase(), resultCode, resultMsg, version);
        }

        return Status.ERROR;
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
            Loggor.info(TAG, "Init MqttLog failed!" );
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
    public void mLog(int logLevel, final String tag, final String format, final Object... obj) {
        if( mMqttLog != null) {
            if( !(mMqttLog.saveMqttLog(logLevel, tag, format, obj))) {
                Loggor.warn(TAG,  String.format("Save %s Level Log failed!", TXMqttLog.level_str[logLevel]));
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
                Loggor.warn(TAG,  String.format("Save %s Level Log failed!", TXMqttLog.level_str[logLevel]));
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
     * 订阅 RRPC Topic, 结果通过回调函数通知，topic 格式: $rrpc/rxd/${ProductId}/${DeviceName}/+
     *
     * @param qos QOS 等级(仅支持 QOS=0 的消息)
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status subscribeRRPCTopic(final int qos, Object userContext) {
        String topic = String.format("$rrpc/rxd/%s/%s/+", mProductId, mDeviceName);
        return subscribe(topic, qos, userContext);
    }

    private Status publishRRPCToCloud(Object userContext, String processId, Map<String, String> replyMsg) {
        // 应答topic格式: $rrpc/txd/${ProductId}/${DeviceName}/${messageid}
        String topic  = String.format("$rrpc/txd/%s/%s/%s", mProductId, mDeviceName, processId);
        //TODO 通过replyMsg构建mqtt messge
        MqttMessage message = new MqttMessage();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("test-key", "test-value"); // for test
            for (Map.Entry<String, String> entrys : replyMsg.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            Loggor.error(TAG,  e.getMessage() + "pack json data failed!");
        }
        message.setQos(TXMqttConstants.QOS0);
        message.setPayload(jsonObject.toString().getBytes());
        return publish(topic, message ,userContext);
    }

    /**
     * 订阅广播 Topic，结果通过回调函数通知。广播 Topic 格式: $broadcast/rxd/${ProductId}/${DeviceName}
     *
     * @param qos QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败；
     */
    public Status subscribeBroadcastTopic(final int qos, Object userContext) {
        String broadCastTopic = "";
        if ((mMqttClient != null) && (mMqttClient.isConnected())) {
            broadCastTopic = String.format("$broadcast/rxd/%s/%s", mProductId, mDeviceName);
            try {
                mMqttClient.subscribe(broadCastTopic, qos ,userContext, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
            } catch (Exception e) {
                Loggor.error(TAG,  String.format(e.getMessage() + " subscribe topic: %s failed.", broadCastTopic));
                mLog(TXMqttLogConstants.LEVEL_FATAL, TAG, "subscribe topic: %s failed.", broadCastTopic);
                return Status.ERROR;
            }
        } else {
            Loggor.error(TAG,  String.format("subscribe topic: %s failed, because mMqttClient not connected.", broadCastTopic));
            mLog(TXMqttLogConstants.LEVEL_FATAL, TAG, "subscribe topic: %s failed, because mMqttClient not connected.", broadCastTopic);
            return Status.MQTT_NO_CONN;
        }
        return Status.OK;
    }

    /**
     * 发布请求 NTP 服务
     *
     * @return 操作结果 {@link Status}
     */
    public Status getNTPService() {

        String topic = NTP_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", "get");

            JSONArray timeArray = new JSONArray();
            timeArray.put("time");

            jsonObject.put("resource", timeArray);
        } catch (JSONException e) {

        }

        message.setQos(0);
        message.setPayload(jsonObject.toString().getBytes());

        Status status = this.publish(topic, message, null);
        return status;
    }

    /**
     * 订阅 NTP Topic, 结果通过回调函数通知，topic 格式: $sys/operation/result/${ProductId}/${DeviceName}/+
     *
     * @param qos QOS 等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status subscribeNTPTopic(final int qos, Object userContext) {
        String topic = NTP_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
        return subscribe(topic, qos, userContext);
    }

    /**
     * 设置当前连接状态
     *
     * @param connectStatus 当前连接状态 {@link TXMqttConstants.ConnectStatus}
     */
    protected synchronized void setConnectingState(TXMqttConstants.ConnectStatus connectStatus) {
        this.mConnectStatus = connectStatus;
    }

    /**
     * 获取当前连接状态
     *
     * @return 当前连接状态 {@link TXMqttConstants.ConnectStatus}
     */
    public TXMqttConstants.ConnectStatus getConnectStatus() {
        return this.mConnectStatus;
    }

    /**
     * 连接完成
     *
     * @param reconnect 重连标记
     * @param serverURI 服务器 URI
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Loggor.info(TAG, "connectComplete. reconnect flag is " + reconnect);
        setConnectingState(TXMqttConstants.ConnectStatus.kConnected);

        if (!reconnect) {
            return;
        }

        Iterator<String> it = mSubscribedTopicMap.keySet().iterator();
        while (it.hasNext()) {
            String topic = it.next();
            Integer qos = mSubscribedTopicMap.get(topic);
            try {
                Loggor.info(TAG, String.format("subscribe to %s...", topic));
                mMqttClient.subscribe(topic, qos, null, new QcloudMqttActionListener(TXMqttConstants.SUBSCRIBE));
            } catch (Exception e) {
                Loggor.error(TAG, String.format("subscribe to %s failed.", topic));
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
     * 连接丢失，回调上层
     *
     * @param cause 连接断开原因
     */
    @Override
    public void connectionLost(Throwable cause) {
        Loggor.error(TAG, "connection lost because of: " + cause.toString());

        setConnectingState(TXMqttConstants.ConnectStatus.kDisconnected);

        mActionCallBack.onConnectionLost(cause);

        mLastReceivedMessageId = INVALID_MESSAGE_ID;

        if (mOTAImpl != null) {
            mOTAImpl.setSubscribedState(false);
        }
    }

    /**
     * 收到 MQTT 消息
     *
     * @param topic 消息主题
     * @param message 消息内容结构体
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (message.getQos() > 0 && message.getId() == mLastReceivedMessageId) {
            Loggor.error(TAG, String.format("Received topic: %s, id: %d, message: %s, discard repeated message!!!",
                    topic, message.getId(), message));
            mLog(TXMqttLogConstants.LEVEL_FATAL, TAG,"Received topic: %s, id: %d, message: %s, discard repeated message!!!", topic, message.getId(), message);
            return;
        }

        Loggor.info(TAG, String.format("Received topic: %s, id: %d, message: %s", topic, message.getId(), message));

        if (topic != null && topic.contains("rrpc/rxd")) {
            String[] items = topic.split("/");
            String processId = items[items.length-1];
            //TODO：数据格式暂不确定
            Map<String, String> replyMessage = new HashMap<>();
            publishRRPCToCloud(null, processId, replyMessage);
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
        if (mOTAImpl != null) {
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
                        Loggor.debug(TAG,  "******Set mqttLogLevel to " + logLevel);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Loggor.debug(TAG,  "******Get mqttLogLevel failed ");
        }
    }

    /**
     * 发布消息成功回调
     *
     * @param messageToken 消息内容 Token
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken messageToken) {
        Loggor.info(TAG, "deliveryComplete, token.getMessageId:" + messageToken.getMessageId());
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
                        TXMqttConstants.PUBLISH_SUCCESS, null);
                break;

            case TXMqttConstants.SUBSCRIBE:
                int[] qos = ((MqttSuback) mqttWireMessage).getGrantedQos();
                if (null != qos && qos.length >= 1 && qos[0] == 128) {
                    mActionCallBack.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(),
                            TXMqttConstants.SUBSCRIBE_FAIL, new Throwable("qos don't support"));
                } else {
                    mActionCallBack.onSubscribeCompleted(Status.OK, token, token.getUserContext(),
                            TXMqttConstants.SUBSCRIBE_SUCCESS, null);

                    if (mOTAImpl != null) {
                        mOTAImpl.onSubscribeCompleted(Status.OK, token, token.getUserContext(),
                                TXMqttConstants.SUBSCRIBE_SUCCESS);
                    }
                }
                break;

            case TXMqttConstants.UNSUBSCRIBE:
                mActionCallBack.onUnSubscribeCompleted(Status.OK, token, token.getUserContext(),
                        TXMqttConstants.UNSUBSCRIBE_SUCCESS, null);
                break;

            default:
                Loggor.error(TAG,  "Unknown message on Success:" + token);
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
                mActionCallBack.onUnSubscribeCompleted(Status.ERROR, token, token.getUserContext(),
                        exception.toString(), exception);
                break;
            default:
                Loggor.error(TAG,  "Unknown message on onFailure:" + token);
                mLog(TXMqttLogConstants.LEVEL_FATAL, TAG,"Unknown message on onFailure:" + token);
                break;
            }
        }
    }
}
