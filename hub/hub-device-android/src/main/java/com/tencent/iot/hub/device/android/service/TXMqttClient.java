package com.tencent.iot.hub.device.android.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * mqtt远程服务客户端
 */

public class TXMqttClient {

    private static final String TAG = TXMqttClient.class.getSimpleName();

    private Context mContext = null;

    /**
     * mqtt客户端选项
     */
    private TXMqttClientOptions mMqttClientOptions = null;

    private Intent mServiceIntent = null;

    /**
     * 内部service连接回调接口
     */
    private ServiceConnection mInternalServiceConnection = null;

    /**
     * 外部service连接回调接口，用于回调service连接状态，便于外部进行mqtt操作
     */
    private ServiceConnection mExternalServiceConnection = null;

    /**
     * mqttAction监听器，用于跨进程调用
     */
    private ITXMqttActionListener mMqttActionListener = null;

    /**
     * mqttAction回调接口，用于外部回调
     */
    private TXMqttActionCallBack mMqttActionCallBack = null;

    private AtomicLong mRequestId = null;

    /**
     * 因userContext在外部定义，sdk内部不便于实现序列化。
     * 因此在客户端保存requestId与userContext的映射关系，在跨进程调用时通过requestId代替userContext进行传递。
     */
    private Map<Long, Object> mUserContextMap = null;

    /**
     * mqtt远程服务
     */
    protected ITXMqttService mRemoteServer = null;

    private TXOTACallBack mOTACallback = null;

    private ITXOTAListener mOTAListener = new ITXOTAListener.Stub() {
        @Override
        public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
            if (mOTACallback != null) {
                mOTACallback.onReportFirmwareVersion(resultCode, version, resultMsg);
            }
        }

        @Override
        public void onDownloadProgress(int percent, String version) throws RemoteException {
            if (mOTACallback != null) {
                mOTACallback.onDownloadProgress(percent, version);
            }
        }

        @Override
        public void onDownloadCompleted(String outputFile, String version) throws RemoteException {
            if (mOTACallback != null) {
                mOTACallback.onDownloadCompleted(outputFile, version);
            }
        }

        @Override
        public void onDownloadFailure(int errCode, String version) throws RemoteException {
            if (mOTACallback != null) {
                mOTACallback.onDownloadFailure(errCode, version);
            }
        }
    };

    public TXMqttClient() {
    }

    /**
     * 设置MqttAction回调接口
     *
     * @param mMqttActionCallBack mqttAction回调接口
     * @return
     */
    public TXMqttClient setMqttActionCallBack(TXMqttActionCallBack mMqttActionCallBack) {
        this.mMqttActionCallBack = mMqttActionCallBack;
        return this;
    }

    /**
     * 设置远程服务连接回调接口
     *
     * @param serviceConnection 远程服务连接回调接口
     * @return
     */
    public TXMqttClient setServiceConnection(ServiceConnection serviceConnection) {
        mExternalServiceConnection = serviceConnection;
        return this;
    }

    /**
     * 初始化远程服务客户端
     *
     * @param context
     * @param clientOptions 客户端选项
     */
    public void init(Context context, TXMqttClientOptions clientOptions) {

        internalInit(context, clientOptions);

        mInternalServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                TXLog.d(TAG, "onServiceDisconnected, ComponentName[%s]", name.getClassName());
                mRemoteServer = null;
                if (null != mExternalServiceConnection) {
                    mExternalServiceConnection.onServiceDisconnected(name);
                }
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                TXLog.d(TAG, "onServiceConnected, ComponentName[%s]", name.getClassName());

                if (null != mExternalServiceConnection) {
                    mExternalServiceConnection.onServiceConnected(name, null);
                }

                // 再次开启远程服务时，会回调ITXMqttActionListener.aidl的onServiceStartedCallback()接口，用于返回服务已开启状态
                // 同时该接口会调用mInternalServiceConnection的onServiceConnected()接口，此时传递的IBinder为null，因此需判断service是否为空。
                if (null == service) {
                    return;
                }

                mRemoteServer = ITXMqttService.Stub.asInterface(service);

                try {
                    mRemoteServer.registerMqttActionListener(mMqttActionListener);
                    mRemoteServer.initDeviceInfo(mMqttClientOptions);
                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote service failed!");
                }
            }
        };
    }

    /**
     * 初始化远程服务客户端（内部接口不对外，仅供TXShadowClient调用）
     *
     * @param context
     * @param clientOptions        客户端选项
     * @param shadowActionListener shadowAction监听器
     */
    protected void init(Context context, TXMqttClientOptions clientOptions, final ITXShadowActionListener shadowActionListener) {

        internalInit(context, clientOptions);

        mInternalServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                TXLog.d(TAG, "onServiceDisconnected, ComponentName[%s]", name.getClassName());
                mRemoteServer = null;

                if (null != mExternalServiceConnection) {
                    mExternalServiceConnection.onServiceDisconnected(name);
                }
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                TXLog.d(TAG, "onServiceConnected, ComponentName[%s]", name.getClassName());

                // 再次开启远程服务时，会回调ITXMqttActionListener.aidl的onServiceStartedCallback()接口，用于返回服务已开启状态
                // 同时该接口会调用mInternalServiceConnection的onServiceConnected()接口，此时传递的IBinder为null，因此需判断service是否为空。
                if (null == service) {
                    if (null != mExternalServiceConnection) {
                        mExternalServiceConnection.onServiceConnected(name, null);
                    }

                    return;
                }

                mRemoteServer = ITXMqttService.Stub.asInterface(service);

                try {
                    mRemoteServer.registerMqttActionListener(mMqttActionListener);
                    mRemoteServer.registerShadowActionListener(shadowActionListener);
                    mRemoteServer.initDeviceInfo(mMqttClientOptions);

                } catch (RemoteException e) {
                    TXLog.e(TAG, e, "invoke remote service failed!");
                }

                if (null != mExternalServiceConnection) {
                    mExternalServiceConnection.onServiceConnected(name, null);
                }
            }
        };

    }

    /**
     * 开启远程服务
     */
    public void startRemoteService() {
        if (null == mContext) {
            TXLog.e(TAG, "TXMqttClient is not initialized!");
            return;
        }

        if (null == mServiceIntent) {
            mServiceIntent = new Intent(mContext, TXMqttService.class);
        }

        try {
            mContext.startService(mServiceIntent);
            mContext.bindService(mServiceIntent, mInternalServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            TXLog.e(TAG, e, "start remote service failed!");
        }
    }

    /**
     * 停止远程服务
     */
    public void stopRemoteService() {
        if (null == mContext || null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return;
        }

        try {
            mContext.unbindService(mInternalServiceConnection);
            mContext.stopService(mServiceIntent);
        } catch (Exception e) {
            TXLog.e(TAG, e, "stop and unbind remote service is failed!");
        }
    }

    /**
     * 设置断连状态buffer缓冲区
     *
     * @param bufferOpts
     */
    public void setBufferOpts(TXDisconnectedBufferOptions bufferOpts) {
        if (null == bufferOpts) {
            return;
        }
        try {
            mRemoteServer.setBufferOpts(bufferOpts);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[setBufferOpts] failed!");
        }
    }

    /**
     * 连接MQTT服务器，结果通过回调函数通知。
     *
     * @param connectOptions 连接参数
     * @param userContext    用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status connect(TXMqttConnectOptions connectOptions, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);

        try {
            String statusStr = mRemoteServer.connect(connectOptions, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[connect] failed!");
        }

        return status;
    }

    /**
     * 重新连接, 结果通过回调函数通知。
     *
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reconnect() {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }

        try {
            String statusStr = mRemoteServer.reconnect();
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[reconnect] failed!");
        }

        return status;
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
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.disConnect(timeout, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[disConnect] failed!");
        }
        return status;
    }

    /**
     * 订阅广播Topic, 结果通过回调函数通知。
     * 广播Topic格式: $broadcast/rxd/${ProductId}/${DeviceName}
     *
     * @param qos         QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status subscribeBroadcastTopic(final int qos, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        String broadCastTopic = String.format("$broadcast/rxd/%s/%s", mMqttClientOptions.getProductId(),
                mMqttClientOptions.getDeviceName());
        try {
            String statusStr = mRemoteServer.subscribe(broadCastTopic, qos, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[subscribe] failed!");
        }
        return status;
    }

    /**
     * 订阅Topic, 结果通过回调函数通知。
     *
     * @param topic       topic名称
     * @param qos         QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status subscribe(String topic, int qos, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.subscribe(topic, qos, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[subscribe] failed!");
        }
        return status;
    }

    /**
     * 取消订阅主题, 结果通过回调函数通知。
     *
     * @param topic       要取消订阅的主题
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status unSubscribe(String topic, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.unSubscribe(topic, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[unSubscribe] failed!");
        }

        return status;
    }

    /**
     * 发布MQTT消息接口, 结果通过回调函数通知。
     *
     * @param topic       topic名称
     * @param message     消息内容
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status publish(String topic, TXMqttMessage message, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            TXLog.e(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.publish(topic, message, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[publish] failed!");
        }

        return status;
    }

    /**
     * 订阅RRPC主题, 结果通过回调函数通知。
     * topic格式: $rrpc/rxd/${ProductId}/${DeviceName}/+
     *
     * @param qos         QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status subscribeRRPCTopic(int qos, Object userContext) {
        String rrpcTopic = String.format("$rrpc/rxd/%s/%s/+", mMqttClientOptions.getProductId(),
                mMqttClientOptions.getDeviceName());
        return subscribe(rrpcTopic, qos, userContext);
    }

    /**
     * 释放资源
     */
    public void clear() {
        mUserContextMap.clear();
    }


    /**
     * 初始化OTA功能。
     *
     * @param storagePath OTA升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param callback    OTA事件回调
     */
    public void initOTA(String storagePath, TXOTACallBack callback) {
        mOTACallback = callback;

        try {
            mRemoteServer.initOTA(storagePath, mOTAListener);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[initOTA] failed!");
        }
    }

    /**
     * 上报设备当前版本信息到后台服务器。
     *
     * @param currentFirmwareVersion 设备当前版本信息
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentFirmwareVersion(String currentFirmwareVersion)  {
        Status status = Status.ERROR;

        try {
            String statusStr = mRemoteServer.reportCurrentFirmwareVersion(currentFirmwareVersion);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[reportCurrentFirmwareVersion] failed!");
        }

        return status;
    }

    /**
     * 上报设备升级状态到后台服务器。
     *
     * @param state 状态
     * @param resultCode 结果代码。0：表示成功；其它：表示失败；常见错误码：-1:下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
     * @param resultMsg 结果描述
     * @param version 版本号
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportOTAState(TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
        Status status = Status.ERROR;

        try {
            String statusStr = mRemoteServer.reportOTAState(state.name(), resultCode, resultMsg, version);
            status = Status.valueOf(Status.class, statusStr);
        } catch (RemoteException e) {
            TXLog.e(TAG, e, "invoke remote service[reportOTAState] failed!");
        }

        return status;
    }

    /**
     * 添加用户上下文
     *
     * @param userContext
     * @return
     */
    protected long addUserContext(Object userContext) {
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        return requestId;
    }

    /**
     * 获取用户上下文
     *
     * @param userContextId
     * @return
     */
    protected Object getUserContext(long userContextId) {
        return mUserContextMap.get(userContextId);
    }

    /**
     * 内部初始化
     */
    private void internalInit(Context context, TXMqttClientOptions clientOptions) {
        mContext = context.getApplicationContext();
        mMqttClientOptions = clientOptions;
        mUserContextMap = new HashMap<Long, Object>();
        mRequestId = new AtomicLong(0);

        mMqttActionListener = new ITXMqttActionListener.Stub() {

            @Override
            public void onConnectCompleted(String status, boolean reconnect, long userContextId, String msg) throws RemoteException {
                TXLog.d(TAG, "onConnectCompleted, status[%s], reconnect[%b], msg[%s]", status, reconnect, msg);
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onConnectCompleted(Status.valueOf(Status.class, status),
                            reconnect, userContext, msg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onConnectionLost(String cause) throws RemoteException {
                TXLog.d(TAG, "onConnectionLost, cause[%s]", cause);
                if (null != mMqttActionCallBack) {
                    mMqttActionCallBack.onConnectionLost(new Throwable(cause));
                }
            }

            @Override
            public void onDisconnectCompleted(String status, long userContextId, String msg) throws RemoteException {
                TXLog.d(TAG, "onDisconnectCompleted, status[%s], msg[%s]", status, msg);
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onDisconnectCompleted(Status.valueOf(Status.class, status), userContext, msg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onPublishCompleted(String status, TXMqttToken token, long userContextId, String errMsg) throws RemoteException {
                TXLog.d(TAG, "onPublishCompleted, status[%s], token[%s], errMsg[%s]", status, token, errMsg);
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onPublishCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(), userContext, errMsg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg) throws RemoteException {
                TXLog.d(TAG, "onSubscribeCompleted, status[%s], token[%s], errMsg[%s]", status, token, errMsg);
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onSubscribeCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(), userContext, errMsg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onUnSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg) throws RemoteException {
                TXLog.d(TAG, "onUnSubscribeCompleted, status[%s], token[%s], errMsg[%s]", status, token, errMsg);
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onUnSubscribeCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(), userContext, errMsg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onMessageReceived(String topic, TXMqttMessage message) throws RemoteException {
                TXLog.d(TAG, "onMessageReceived, topic[%s], message[%s]", topic, message);
                if (null != mMqttActionCallBack) {
                    mMqttActionCallBack.onMessageReceived(topic, message.transToMqttMessage());
                }
            }

            @Override
            public void onServiceStartedCallback() throws RemoteException {
                if (null != mInternalServiceConnection) {
                    ComponentName componentName = null;
                    if (null != mServiceIntent) {
                        componentName = mServiceIntent.getComponent();
                    }
                    if (null != componentName) {
                        mInternalServiceConnection.onServiceConnected(componentName, null);
                    }
                }
            }

            @Override
            public void onServiceDestroyCallback() throws RemoteException {
                TXLog.d(TAG, "onServiceDestroyCallback");
                if (null != mInternalServiceConnection) {
                    ComponentName componentName = null;
                    if (null != mServiceIntent) {
                        componentName = mServiceIntent.getComponent();
                    }
                    if (null != componentName) {
                        mInternalServiceConnection.onServiceDisconnected(componentName);
                    }
                }
            }
        };
    }
}
