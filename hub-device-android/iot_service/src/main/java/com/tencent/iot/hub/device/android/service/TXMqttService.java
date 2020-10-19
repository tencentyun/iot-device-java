package com.tencent.iot.hub.device.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.shadow.TXShadowConnection;
import com.tencent.iot.hub.device.android.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.android.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SERVER_PORT_TLS;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.PREFIX;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.QCLOUD_IOT_MQTT_DIRECT_DOMAIN;

/**
 * MQTT 远程服务
 * <p>
 * 如需将MQTT功能运行在独立进程中时，使用TXMqttService开启服务；
 * 不需要时，直接调用TXMqttConnection或TXShadowConnection中相关接口即可。
 */
public class TXMqttService extends Service {

    private static final String TAG = TXMqttService.class.getSimpleName();

    private Context mContext = null;

    /**
     * 服务器URI
     */
    private String mServerURI;

    /**
     * Iot Hub控制台获取产品ID
     */
    private String mProductId;

    /**
     * 设备名，唯一
     */
    private String mDeviceName;

    private String mSecretKey;

    /**
     * MQTT 连接器
     */
    private TXMqttConnection mMqttConnection = null;

    /**
     * shadow连接器
     */
    private TXShadowConnection mShadowConnection = null;

    /**
     * mqttAction回调接口
     */
    private TXMqttActionCallBack mMqttActionCallBack = null;

    /**
     * shadowAction回调接口
     */
    private TXShadowActionCallBack mShadowActionCallBack = null;

    /**
     * 客户端MqttAction监听器
     */
    private ITXMqttActionListener mMqttActionListener = null;

    /**
     * 客户端ShadowAction监听器
     */
    private ITXShadowActionListener mShadowActionListener = null;

    /**
     * 断连缓存选项
     */
    private DisconnectedBufferOptions mDisconnectedBufferOptions = null;

    private MqttClientPersistence mClientPersistence = null;

    /**
     * MQTT
     */
    private ITXMqttService.Stub mMqttService = null;

    /**
     * 是否使用shadow
     */
    private boolean mUseShadow = false;

    private boolean isInit = false;


    private ITXOTAListener mOTAListener = null;
    private TXOTACallBack mInternalOTACallback = new TXOTACallBack() {
        @Override
        public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
            if (mOTAListener != null) {
                try {
                    mOTAListener.onReportFirmwareVersion(resultCode, version, resultMsg);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean onLastestFirmwareReady(String url, String md5, String version) {
            TXLog.e(TAG, "TXMqttService onLastestFirmwareReady");
            return false;
        }

        @Override
        public void onDownloadProgress(int percent, String version) {
            if (mOTAListener != null) {
                try {
                    mOTAListener.onDownloadProgress(percent, version);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDownloadCompleted(String outputFile, String version) {
            try {
                mOTAListener.onDownloadCompleted(outputFile, version);
            }catch (Exception e) {

            }
        }

        @Override
        public void onDownloadFailure(int errCode, String version) {
            try {
                mOTAListener.onDownloadFailure(errCode, version);
            }catch (Exception e) {

            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        TXLog.d(TAG, "onCreate");
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String intentStr = (null == intent) ? "" : intent.toString();
        TXLog.d(TAG, "onStartCommand, intent[%s], flags[%d], startId[%d]", intentStr, flags, startId);
        if (startId > 1 && null != mMqttActionListener) {
            try {
                mMqttActionListener.onServiceStartedCallback();
            } catch (RemoteException e) {
                TXLog.e(TAG, e, "invoke remote method[onServiceStartedCallback] failed!");
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        TXLog.d(TAG, "onBind");
        return mMqttService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        TXLog.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        TXLog.d(TAG, "onDestroy");
        super.onDestroy();
        if (mUseShadow) {
            if (null != mShadowConnection) {
                mShadowConnection.disConnect(null);
            }
        } else {
            if (null != mMqttConnection) {
                mMqttConnection.disConnect(null);
            }
        }

        try {
            mMqttActionListener.onServiceDestroyCallback();
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote method[onServiceDestroyCallback] failed!");
        }

        mMqttService = null;
        mClientPersistence = null;
        mDisconnectedBufferOptions = null;

        mMqttConnection = null;
        mShadowConnection = null;
        mMqttActionListener = null;
        mShadowActionListener = null;
    }

    /**
     * 初始化设备信息
     *
     * @param mqttClientOptions
     */
    private void initDeviceInfo(TXMqttClientOptions mqttClientOptions) {
        mProductId = mqttClientOptions.getProductId();
        mDeviceName = mqttClientOptions.getDeviceName();
        mServerURI = mqttClientOptions.getServerURI();
        mSecretKey = mqttClientOptions.getSecretKey();
        TXLog.d(TAG, "initDeviceInfo, productId[%s], deviceName[%s], serverURI[%s]", mProductId, mDeviceName, mServerURI);
        isInit = true;
    }

    /**
     * mqtt连接服务器
     *
     * @param options
     * @param userContextId
     */
    private String connect(TXMqttConnectOptions options, long userContextId) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        // 检查连接类型是否发生改变，若发生改变，则先关闭之前的连接
        if (mUseShadow != options.isUseShadow()) {
            if (mUseShadow && null != mShadowConnection
                    && mShadowConnection.getConnectStatus() == TXMqttConstants.ConnectStatus.kConnected) {
                mShadowConnection.disConnect(null);
            } else if (null != mMqttConnection
                    && mMqttConnection.getConnectStatus() == TXMqttConstants.ConnectStatus.kConnected) {
                mMqttConnection.disConnect(null);
            }
        }

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setConnectionTimeout(options.getConnectionTimeout());
        connectOptions.setAutomaticReconnect(options.isAutomaticReconnect());
        connectOptions.setCleanSession(options.isCleanSession());
        connectOptions.setKeepAliveInterval(options.getKeepAliveInterval());

        if (options.isAsymcEncryption()) {

            String certFile = options.getDeviceCertName();
            String keyFile  = options.getDeviceKeyName();
            String secretKey = options.getSecretKey();

            if (secretKey != null) {
                connectOptions.setSocketFactory(AsymcSslUtils.getSocketFactory());
            }else {

                if (certFile.startsWith("/")) {
                    connectOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(certFile, keyFile));
                } else if (certFile.startsWith("file://")) {
                    certFile = certFile.substring(7);
                    keyFile = keyFile.substring(7);
                    connectOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(certFile, keyFile));
                } else {
                    connectOptions.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, certFile, keyFile));
                }
            }
        }

        mUseShadow = options.isUseShadow();
        if (mUseShadow) {
            if (TextUtils.isEmpty(mServerURI)) {
                mShadowConnection = new TXShadowConnection(mContext, PREFIX + mProductId + QCLOUD_IOT_MQTT_DIRECT_DOMAIN + MQTT_SERVER_PORT_TLS, mProductId, mDeviceName, mSecretKey,
                        mDisconnectedBufferOptions, mClientPersistence, mShadowActionCallBack);
            } else {
                mShadowConnection = new TXShadowConnection(mContext, mServerURI, mProductId, mDeviceName, mSecretKey,
                        mDisconnectedBufferOptions, mClientPersistence, mShadowActionCallBack);
            }
            status = mShadowConnection.connect(connectOptions, null);
        } else {
            mMqttConnection = new TXMqttConnection(mContext, mProductId, mDeviceName, mSecretKey, mMqttActionCallBack);
            status = mMqttConnection.connect(connectOptions, Long.valueOf(userContextId));
        }
        return status.name();
    }

    /**
     * 重新连接
     *
     * @return
     */
    private String reconnect() {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        if (!mUseShadow && null != mMqttConnection) {
            status = mMqttConnection.reconnect();
        }
        return status.name();
    }

    /**
     * mqtt断连
     */
    private String disConnect(long timeout, long userContextId) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.disConnect(null);
        } else if (null != mMqttConnection) {
            status = mMqttConnection.disConnect(timeout, Long.valueOf(userContextId));
        }
        return status.name();
    }

    private String publish(String topic, TXMqttMessage txMessage, long userContext) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        MqttMessage message = txMessage.transToMqttMessage();
        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().publish(topic, message, userContext);
        } else if (null != mMqttConnection) {
            status = mMqttConnection.publish(topic, message, userContext);
        }
        return status.name();
    }

    /**
     * 初始化OTA功能。
     *
     * @param storagePath OTA升级包存储路径(调用者必确保路径已存在，并且具有写权限)
     * @param otaListener OTA事件回调
     */
    public void initOTA(String storagePath, ITXOTAListener otaListener) {
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return;
        }
        
        mOTAListener = otaListener;

        if (mUseShadow && null != mShadowConnection) {
            mShadowConnection.getMqttConnection().initOTA(storagePath, mInternalOTACallback);
        } else if (null != mMqttConnection) {
            mMqttConnection.initOTA(storagePath, mInternalOTACallback);
        }
    }

    /**
     * 上报设备当前版本信息到后台服务器。
     *
     * @param currentFirmwareVersion 设备当前版本信息
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentFirmwareVersion(String currentFirmwareVersion) {
        Status status = Status.ERROR;

        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().reportCurrentFirmwareVersion(currentFirmwareVersion);
        } else if (null != mMqttConnection) {
            status = mMqttConnection.reportCurrentFirmwareVersion(currentFirmwareVersion);
        }
        return status;
    }

    /**
     * 上报设备升级状态到后台服务器。
     *
     * @param state
     * @param resultCode
     * @param resultMsg
     * @param version
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportOTAState(String state, int resultCode, String resultMsg, String version) {
        Status status = Status.ERROR;


        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().reportOTAState(TXOTAConstansts.ReportState.valueOf(TXOTAConstansts.ReportState.class, state), resultCode, resultMsg, version);
        } else if (null != mMqttConnection) {
            status = mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.valueOf(TXOTAConstansts.ReportState.class, state), resultCode, resultMsg, version);
        }
        return status;
    }

    private String subscribeBroadcastTopic(int qos, long userContextId) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }
        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().subscribeBroadcastTopic(qos, Long.valueOf(userContextId));
        } else if (null != mMqttConnection) {
            status = mMqttConnection.subscribeBroadcastTopic(qos, Long.valueOf(userContextId));
        }
        return status.name();
    }

    private String subscribe(String topic, int qos, long userContextId) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().subscribe(topic, qos, Long.valueOf(userContextId));
        } else if (null != mMqttConnection) {
            status = mMqttConnection.subscribe(topic, qos, Long.valueOf(userContextId));
        }

        return status.name();
    }

    private String unSubscribe(String topic, long userContextId) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().unSubscribe(topic, Long.valueOf(userContextId));
        } else if (null != mMqttConnection) {
            status = mMqttConnection.unSubscribe(topic, Long.valueOf(userContextId));
        }

        return status.name();
    }

    private String subscribeRRPCTopic(int qos, long userContextId) {
        Status status = Status.ERROR;
        if (!isInit) {
            TXLog.d(TAG, "device is not initialized!");
            return status.name();
        }

        if (mUseShadow && null != mShadowConnection) {
            status = mShadowConnection.getMqttConnection().subscribeRRPCTopic(qos, Long.valueOf(userContextId));
        } else if (null != mMqttConnection) {
            status = mMqttConnection.subscribeRRPCTopic(qos, Long.valueOf(userContextId));
        }

        return status.name();
    }

    private void handlePublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
        if (null == userContext) {
            return;
        }

        if (mUseShadow) {
            if (null == mShadowActionListener) {
                TXLog.d(TAG, "ITXShadowActionListener instance is null!");
                return;
            }

            try {
                if (userContext instanceof Long) {
                    mShadowActionListener.onPublishCompleted(status.name(), new TXMqttToken(token),
                            ((Long) userContext).longValue(), errMsg);
                }
            } catch (RemoteException e) {
                TXLog.e(TAG, e, "invoke remote method[onPublishCompleted] failed!");
            } catch (Exception ex) {
                TXLog.e(TAG, ex, "invoke remote method[onPublishCompleted] failed!");
            }

            return;
        }

        if (null == mMqttActionListener) {
            TXLog.d(TAG, "ITXMqttActionListener instance is null!");
            return;
        }

        try {
            if (userContext instanceof Long) {
                mMqttActionListener.onPublishCompleted(status.name(), new TXMqttToken(token), ((Long) userContext).longValue(), errMsg);
            }
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote method[onPublishCompleted] failed!");
        } catch (Exception ex) {
            TXLog.e(TAG, ex, "invoke remote method[onPublishCompleted] failed!");
        }
    }

    private void handleSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
        if (null == userContext) {
            return;
        }

        if (mUseShadow) {
            if (null == mShadowActionListener) {
                TXLog.d(TAG, "ITXShadowActionListener instance is null!");
                return;
            }

            try {
                if (userContext instanceof Long) {
                    mShadowActionListener.onSubscribeCompleted(status.name(), new TXMqttToken(asyncActionToken), ((Long) userContext).longValue(), errMsg);
                }
            } catch (RemoteException e) {
                TXLog.e(TAG, e, "invoke remote method[onSubscribeCompleted] failed!");
            } catch (Exception ex) {
                TXLog.e(TAG, ex, "invoke remote method[onSubscribeCompleted] failed!");
            }

            return;
        }

        if (null == mMqttActionListener) {
            TXLog.d(TAG, "ITXMqttActionListener instance is null!");
            return;
        }

        try {
            if (userContext instanceof Long) {
                mMqttActionListener.onSubscribeCompleted(status.name(), new TXMqttToken(asyncActionToken),
                        ((Long) userContext).longValue(), errMsg);
            }
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote method[onSubscribeCompleted] failed!");
        } catch (Exception ex) {
            TXLog.e(TAG, ex, "invoke remote method[onSubscribeCompleted] failed!");
        }
    }

    private void handleUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
        if (null == userContext) {
            return;
        }

        if (mUseShadow) {
            if (null == mShadowActionListener) {
                TXLog.d(TAG, "ITXShadowActionListener instance is null!");
                return;
            }

            try {
                if (userContext instanceof Long) {
                    mShadowActionListener.onUnSubscribeCompleted(status.name(), new TXMqttToken(asyncActionToken), ((Long) userContext).longValue(), errMsg);
                }
            } catch (RemoteException e) {
                TXLog.e(TAG, e, "invoke remote method[onUnSubscribeCompleted] failed!");
            } catch (Exception ex) {
                TXLog.e(TAG, ex, "invoke remote method[onUnSubscribeCompleted] failed!");
            }

            return;
        }

        if (null == mMqttActionListener) {
            TXLog.d(TAG, "ITXMqttActionListener instance is null!");
            return;
        }

        try {
            if (userContext instanceof Long) {
                mMqttActionListener.onUnSubscribeCompleted(status.name(), new TXMqttToken(asyncActionToken), ((Long) userContext).longValue(), errMsg);
            }
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote method[onUnSubscribeCompleted] failed!");
        } catch (Exception ex) {
            TXLog.e(TAG, ex, "invoke remote method[onUnSubscribeCompleted] failed!");
        }
    }

    private void handleMessageReceived(String topic, MqttMessage message) {
        if (mUseShadow) {
            if (null == mShadowActionListener) {
                TXLog.d(TAG, "ITXShadowActionListener instance is null!");
                return;
            }

            try {
                mShadowActionListener.onMessageReceived(topic, new TXMqttMessage(message));
            } catch (RemoteException e) {
                TXLog.e(TAG, e, "invoke remote method[onUnSubscribeCompleted] failed!");
            } catch (Exception ex) {
                TXLog.e(TAG, ex, "invoke remote method[onUnSubscribeCompleted] failed!");
            }

            return;
        }


        if (null == mMqttActionListener) {
            TXLog.d(TAG, "ITXMqttActionListener instance is null!");
            return;
        }

        try {
            mMqttActionListener.onMessageReceived(topic, new TXMqttMessage(message));
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote method[onMessageReceived] failed!");
        }
    }

    private void init() {
        mContext = this.getApplicationContext();

        mMqttActionCallBack = new TXMqttActionCallBack() {
            @Override
            public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
                if (null == mMqttActionListener) {
                    TXLog.d(TAG, "ITXMqttActionListener instance is null!");
                    return;
                }

                try {
                    if (userContext instanceof Long) {
                        mMqttActionListener.onConnectCompleted(status.name(), reconnect, ((Long) userContext).longValue(), msg);
                    }
                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote method[onConnectCompleted] failed!");
                }
            }

            @Override
            public void onConnectionLost(Throwable cause) {
                if (null == mMqttActionListener) {
                    TXLog.d(TAG, "ITXMqttActionListener instance is null!");
                    return;
                }

                try {
                    mMqttActionListener.onConnectionLost(cause.getMessage());
                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote method[onConnectionLost] failed!");
                }
            }

            @Override
            public void onDisconnectCompleted(Status status, Object userContext, String msg) {
                if (null == mMqttActionListener) {
                    TXLog.d(TAG, "ITXMqttActionListener instance is null!");
                    return;
                }

                try {
                    if (userContext instanceof Long) {
                        mMqttActionListener.onDisconnectCompleted(status.name(), ((Long) userContext).longValue(), msg);
                    }
                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote method[onDisconnectCompleted] failed!");
                }
            }

            @Override
            public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                TXMqttService.this.handlePublishCompleted(status, token, userContext, errMsg);
            }

            @Override
            public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
                TXMqttService.this.handleSubscribeCompleted(status, asyncActionToken, userContext, errMsg);
            }

            @Override
            public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
                TXMqttService.this.handleUnSubscribeCompleted(status, asyncActionToken, userContext, errMsg);
            }

            @Override
            public void onMessageReceived(String topic, MqttMessage message) {
                TXMqttService.this.handleMessageReceived(topic, message);
            }
        };

        mShadowActionCallBack = new TXShadowActionCallBack() {
            @Override
            public void onRequestCallback(String type, int result, String document) {
                if (null == mShadowActionListener) {
                    TXLog.d(TAG, "ITXShadowActionListener instance is null!");
                    return;
                }

                try {
                    mShadowActionListener.onRequestCallback(type, result, document);
                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote method[onRequestCallback] failed!");
                }
            }

            @Override
            public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends com.tencent.iot.hub.device.java.core.shadow.DeviceProperty> devicePropertyList) {
                if (null == mShadowActionListener) {
                    TXLog.d(TAG, "ITXShadowActionListener instance is null!");
                    return;
                }

                try {
                    mShadowActionListener.onDevicePropertyCallback(propertyJSONDocument, (List<DeviceProperty>) devicePropertyList);
                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote method[onRequestCallback] failed!");
                }
            }

            @Override
            public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
                TXMqttService.this.handlePublishCompleted(status, token, userContext, errMsg);
            }

            @Override
            public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
                TXMqttService.this.handleSubscribeCompleted(status, asyncActionToken, userContext, errMsg);
            }

            @Override
            public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
                TXMqttService.this.handleUnSubscribeCompleted(status, asyncActionToken, userContext, errMsg);
            }

            @Override
            public void onMessageReceived(String topic, MqttMessage message) {
                TXMqttService.this.handleMessageReceived(topic, message);
            }
        };

        mMqttService = new ITXMqttService.Stub() {
            @Override
            public void registerMqttActionListener(ITXMqttActionListener mqttActionListener) throws RemoteException {
                mMqttActionListener = mqttActionListener;
            }

            @Override
            public void registerShadowActionListener(ITXShadowActionListener shadowActionListener) throws RemoteException {
                mShadowActionListener = shadowActionListener;
            }

            /**
             * 初始化设备信息
             *
             * @param mqttClientOptions   mqtt客户端选项
             * @throws RemoteException
             */
            @Override
            public void initDeviceInfo(TXMqttClientOptions mqttClientOptions) throws RemoteException {
                TXMqttService.this.initDeviceInfo(mqttClientOptions);
            }

            @Override
            public void setBufferOpts(TXDisconnectedBufferOptions bufferOptions) throws RemoteException {
                if (mUseShadow && null != mShadowConnection) {
                    mShadowConnection.setBufferOpts(bufferOptions.transToDisconnectedBufferOptions());
                } else if (null != mMqttConnection) {
                    mMqttConnection.setBufferOpts(bufferOptions.transToDisconnectedBufferOptions());
                }
            }

            @Override
            public String connect(TXMqttConnectOptions options, long userContextId) throws RemoteException {
                return TXMqttService.this.connect(options, userContextId);
            }

            @Override
            public String reconnect() throws RemoteException {
                return TXMqttService.this.reconnect();
            }

            @Override
            public String disConnect(long timeout, long userContextId) throws RemoteException {
                return TXMqttService.this.disConnect(timeout, userContextId);
            }

            @Override
            public String subscribeBroadcastTopic(int qos, long userContextId) throws RemoteException {
                return TXMqttService.this.subscribeBroadcastTopic(qos, userContextId);
            }

            @Override
            public String subscribe(String topic, int qos, long userContextId) throws RemoteException {
                return TXMqttService.this.subscribe(topic, qos, userContextId);
            }

            @Override
            public String unSubscribe(String topic, long userContextId) throws RemoteException {
                return TXMqttService.this.unSubscribe(topic, userContextId);
            }

            @Override
            public String publish(String topic, TXMqttMessage message, long userContextId) throws RemoteException {
                return TXMqttService.this.publish(topic, message, userContextId);
            }

            @Override
            public String subscribeRRPCTopic(int qos, long userContextId) throws RemoteException {
                return TXMqttService.this.subscribeRRPCTopic(qos, userContextId);
            }

            @Override
            public String getConnectStatus() throws RemoteException {
                return mShadowConnection.getConnectStatus().name();
            }

            @Override
            public String updateShadow(List<DeviceProperty> devicePropertyList, long userContextId) throws RemoteException {
                for (DeviceProperty deviceProperty : devicePropertyList) {
                    TXLog.d(TAG, "updateShadow, deviceProperty[%s]", deviceProperty.toString());

                    if (deviceProperty.mDataType == TXShadowConstants.JSONDataType.OBJECT) {
                        try {
                            deviceProperty.mData = new JSONObject((String) deviceProperty.mData);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else if (deviceProperty.mDataType == TXShadowConstants.JSONDataType.ARRAY) {
                        try {
                            deviceProperty.mData = new JSONArray((String) deviceProperty.mData);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                Status status = Status.ERROR;
                if (mUseShadow && null != mShadowConnection) {
                    status = mShadowConnection.update(devicePropertyList, Long.valueOf(userContextId));
                }
                return status.name();
            }

            @Override
            public String getShadow(long userContextId) throws RemoteException {
                Status status = Status.ERROR;
                if (mUseShadow && null != mShadowConnection) {
                    status = mShadowConnection.get(Long.valueOf(userContextId));
                }
                return status.name();
            }

            @Override
            public void registerDeviceProperty(DeviceProperty deviceProperty) throws RemoteException {
                if (mUseShadow && null != mShadowConnection) {
                    mShadowConnection.registerProperty(deviceProperty);
                }
            }

            @Override
            public void unRegisterDeviceProperty(DeviceProperty deviceProperty) throws RemoteException {
                if (mUseShadow && null != mShadowConnection) {
                    mShadowConnection.unRegisterProperty(deviceProperty);
                }
            }

            @Override
            public String reportNullDesiredInfo(String reportJsonDoc) throws RemoteException {
                Status status = Status.ERROR;
                if (mUseShadow && null != mShadowConnection) {
                    if (TextUtils.isEmpty(reportJsonDoc)) {
                        status = mShadowConnection.reportNullDesiredInfo();
                    } else {
                        status = mShadowConnection.reportNullDesiredInfo(reportJsonDoc);
                    }
                }
                return status.name();
            }

            @Override
            public void initOTA(String storagePath, ITXOTAListener listener) throws RemoteException {

                TXMqttService.this.initOTA(storagePath, listener);
            }

            @Override
            public String reportCurrentFirmwareVersion(String currentFirmwareVersion) throws RemoteException {
                Status status = TXMqttService.this.reportCurrentFirmwareVersion(currentFirmwareVersion);
                return status.name();
            }

            @Override
            public String reportOTAState(String state, int resultCode, String resultMsg, String version) throws RemoteException {
                Status status = TXMqttService.this.reportOTAState(state, resultCode, resultMsg, version);
                return status.name();
            }
        };
    }

}
