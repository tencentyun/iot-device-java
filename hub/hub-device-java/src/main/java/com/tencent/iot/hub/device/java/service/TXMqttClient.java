package com.tencent.iot.hub.device.java.service;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.service.interfaces.ITXMqttActionListener;
import com.tencent.iot.hub.device.java.service.interfaces.ITXMqttService;
import com.tencent.iot.hub.device.java.service.interfaces.ITXOTAListener;
import com.tencent.iot.hub.device.java.service.interfaces.ITXShadowActionListener;
import com.tencent.iot.hub.device.java.utils.Loggor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mqtt远程服务客户端
 */
public class TXMqttClient {

    private static final String TAG = TXMqttClient.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TXMqttClient.class);
    static { Loggor.setLogger(logger); }

    /**
     * mqtt客户端选项
     */
    private TXMqttClientOptions mMqttClientOptions = null;


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

    private ITXOTAListener mOTAListener = new ITXOTAListener() {
        @Override
        public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
            if (mOTACallback != null) {
                mOTACallback.onReportFirmwareVersion(resultCode, version, resultMsg);
            }
        }

        @Override
        public void onDownloadProgress(int percent, String version){
            if (mOTACallback != null) {
                mOTACallback.onDownloadProgress(percent, version);
            }
        }

        @Override
        public void onDownloadCompleted(String outputFile, String version) {
            if (mOTACallback != null) {
                mOTACallback.onDownloadCompleted(outputFile, version);
            }
        }

        @Override
        public void onDownloadFailure(int errCode, String version) {
            if (mOTACallback != null) {
                mOTACallback.onDownloadFailure(errCode, version);
            }
        }
    };

    /**
     * 构造函数
     */
    public TXMqttClient() {
    }

    /**
     * 设置 MqttAction 回调接口
     *
     * @param mMqttActionCallBack mqttAction 回调接口
     * @return
     */
    public TXMqttClient setMqttActionCallBack(TXMqttActionCallBack mMqttActionCallBack) {
        this.mMqttActionCallBack = mMqttActionCallBack;
        return this;
    }

  
    /**
     * 初始化远程服务客户端
     *
     * @param clientOptions 客户端选项
     */
    public void init( TXMqttClientOptions clientOptions) {

        internalInit(clientOptions);

        try {
             mRemoteServer.registerMqttActionListener(mMqttActionListener);
             mRemoteServer.initDeviceInfo(mMqttClientOptions);
          } catch (Exception e) {
             Loggor.error(TAG, "invoke remote service failed! " + e);
          }
            
        
    }

    /**
     * 初始化远程服务客户端（内部接口不对外，仅供 TXShadowClient 调用）
     *
     * @param clientOptions 客户端选项
     * @param shadowActionListener shadowAction监听器
     */
    protected void init( TXMqttClientOptions clientOptions, final ITXShadowActionListener shadowActionListener) {

        internalInit(clientOptions);
        try {
             mRemoteServer.registerMqttActionListener(mMqttActionListener);
             mRemoteServer.registerShadowActionListener(shadowActionListener);
             mRemoteServer.initDeviceInfo(mMqttClientOptions);

        } catch (Exception e) {
             Loggor.error(TAG, "invoke remote service failed! " + e);
        }

    }


    /**
     * 设置断连状态 buffer 缓冲区
     *
     * @param bufferOpts
     */
    public void setBufferOpts(TXDisconnectedBufferOptions bufferOpts) {
        if (null == bufferOpts) {
            return;
        }
        try {
            mRemoteServer.setBufferOpts(bufferOpts);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[setBufferOpts] failed! " + e);
        }
    }

    /**
     * 连接 MQTT 服务器，结果通过回调函数通知。
     *
     * @param connectOptions 连接参数
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status connect(TXMqttConnectOptions connectOptions, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            Loggor.error(TAG,  "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);

        try {
            String statusStr = mRemoteServer.connect(connectOptions, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[connect] failed! " + e);
        }

        return status;
    }

    /**
     * 重新连接, 结果通过回调函数通知。
     *
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status reconnect() {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            Loggor.error(TAG, "remote service is not start!");
            return status;
        }

        try {
            String statusStr = mRemoteServer.reconnect();
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[reconnect] failed! " + e);
        }

        return status;
    }

    /**
     * MQTT断连，结果通过回调函数通知。
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status disConnect(Object userContext) {
        return disConnect(0, userContext);
    }

    /**
     * MQTT断连, 结果通过回调函数通知。
     *
     * @param timeout     等待时间（必须大于0）。单位：毫秒
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status disConnect(long timeout, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            Loggor.error(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.disConnect(timeout, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[disConnect] failed! " + e);
        }
        return status;
    }

    /**
     * 订阅Topic, 结果通过回调函数通知。
     *
     * @param topic topic名称
     * @param qos QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status subscribe(String topic, int qos, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            Loggor.error(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.subscribe(topic, qos, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[subscribe] failed!" + e);
        }
        return status;
    }

    /**
     * 取消订阅主题, 结果通过回调函数通知。
     *
     * @param topic 要取消订阅的主题
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status unSubscribe(String topic, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            Loggor.error(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.unSubscribe(topic, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[unSubscribe] failed! " + e);
        }

        return status;
    }

    /**
     * 发布MQTT消息接口, 结果通过回调函数通知。
     *
     * @param topic topic名称
     * @param message 消息内容
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status publish(String topic, TXMqttMessage message, Object userContext) {
        Status status = Status.ERROR;
        if (null == mRemoteServer) {
            Loggor.error(TAG, "remote service is not start!");
            return status;
        }
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        try {
            String statusStr = mRemoteServer.publish(topic, message, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[publish] failed! " + e);
        }

        return status;
    }

    /**
     * 释放资源
     */
    public void clear() {
        mUserContextMap.clear();
    }


    /**
     * 初始化 OTA 功能
     *
     * @param storagePath OTA升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param callback OTA事件回调
     */
    public void initOTA(String storagePath, TXOTACallBack callback) {
        mOTACallback = callback;

        try {
            mRemoteServer.initOTA(storagePath, mOTAListener);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[initOTA] failed! " + e);
        }
    }

    /**
     * 上报设备当前版本信息到后台服务器。
     *
     * @param currentFirmwareVersion 设备当前版本信息
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status reportCurrentFirmwareVersion(String currentFirmwareVersion)  {
        Status status = Status.ERROR;

        try {
            String statusStr = mRemoteServer.reportCurrentFirmwareVersion(currentFirmwareVersion);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[reportCurrentFirmwareVersion] failed! " + e);
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
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败 {@link Status}
     */
    public Status reportOTAState(TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {
        Status status = Status.ERROR;

        try {
            String statusStr = mRemoteServer.reportOTAState(state.name(), resultCode, resultMsg, version);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            Loggor.error(TAG, "invoke remote service[reportOTAState] failed! " + e);
        }

        return status;
    }

    /**
     * 添加用户上下文
     *
     * @param userContext 用户上下文
     * @return 请求 ID
     */
    protected long addUserContext(Object userContext) {
        long requestId = mRequestId.getAndIncrement();
        mUserContextMap.put(requestId, userContext);
        return requestId;
    }

    /**
     * 获取用户上下文
     *
     * @param userContextId 请求 ID
     * @return 用户上下文
     */
    protected Object getUserContext(long userContextId) {
        return mUserContextMap.get(userContextId);
    }

    /**
     * 内部初始化
     */
    private void internalInit(TXMqttClientOptions clientOptions) {
     
        mMqttClientOptions = clientOptions;
        mUserContextMap = new HashMap<Long, Object>();
        mRequestId = new AtomicLong(0);

        mMqttActionListener = new ITXMqttActionListener() {

            @Override
            public void onConnectCompleted(String status, boolean reconnect, long userContextId, String msg) {
                Loggor.error(TAG, String.format("onConnectCompleted, status[%s], reconnect[%b], msg[%s]", status, reconnect, msg));
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onConnectCompleted(Status.valueOf(Status.class, status),
                            reconnect, userContext, msg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onConnectionLost(String cause)  {
                Loggor.error(TAG, String.format("onConnectionLost, cause[%s]", cause));
                if (null != mMqttActionCallBack) {
                    mMqttActionCallBack.onConnectionLost(new Throwable(cause));
                }
            }

            @Override
            public void onDisconnectCompleted(String status, long userContextId, String msg) {
                Loggor.error(TAG, String.format("onDisconnectCompleted, status[%s], msg[%s]", status, msg));
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onDisconnectCompleted(Status.valueOf(Status.class, status), userContext, msg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onPublishCompleted(String status, TXMqttToken token, long userContextId, String errMsg) {
                Loggor.error(TAG, String.format("onPublishCompleted, status[%s], token[%s], errMsg[%s]", status, token, errMsg));
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onPublishCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(), userContext, errMsg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg) {
                Loggor.error(TAG, String.format("onSubscribeCompleted, status[%s], token[%s], errMsg[%s]", status, token, errMsg));
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onSubscribeCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(), userContext, errMsg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onUnSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg) {
                Loggor.error(TAG, String.format("onUnSubscribeCompleted, status[%s], token[%s], errMsg[%s]", status, token, errMsg));
                if (null != mMqttActionCallBack) {
                    Object userContext = mUserContextMap.get(Long.valueOf(userContextId));
                    mMqttActionCallBack.onUnSubscribeCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(), userContext, errMsg);
                    mUserContextMap.remove(Long.valueOf(userContextId));
                }
            }

            @Override
            public void onMessageReceived(String topic, TXMqttMessage message) {
                Loggor.error(TAG, String.format("onMessageReceived, topic[%s], message[%s]", topic, message));
                if (null != mMqttActionCallBack) {
                    mMqttActionCallBack.onMessageReceived(topic, message.transToMqttMessage());
                }
            }

            @Override
            public void onServiceStartedCallback(){

            }

            @Override
            public void onServiceDestroyCallback(){
                Loggor.error(TAG, "onServiceDestroyCallback");
            }
        };
    }
}
