package com.tencent.iot.hub.device.java.service;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.service.interfaces.ITXShadowActionListener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shadow远程服务客户端
 */
public class TXShadowClient {

    private static final String TAG = TXShadowClient.class.getSimpleName();
    private static final Logger LOG = LoggerFactory.getLogger(TXShadowClient.class);
    
    /**
     * mqtt客户端，ShadowClient所有功能均通过该成员变量进行
     */
    private TXMqttClient mMqttClient = null;

    /**
     * shadowAction监听器，用于跨进程调用
     */
    private ITXShadowActionListener mShadowActionListener = null;

    /**
     * shadowAction回调接口，用于外部回调
     */
    private TXShadowActionCallBack mShadowActionCallBack = null;

    public TXShadowClient() {
        this.mMqttClient = new TXMqttClient();
    }

    public TXShadowClient(TXMqttClient mMqttClient) {
        this.mMqttClient = mMqttClient;
    }

    /**
     * 设置ShadowAction回调接口
     *
     * @param mShadowActionCallBack shadowAction回调接口
     * @return
     */
    public TXShadowClient setShadowActionCallBack(TXShadowActionCallBack mShadowActionCallBack) {
        this.mShadowActionCallBack = mShadowActionCallBack;
        return this;
    }


    /**
     * 获取Mqtt客户端实例
     *
     * @return
     */
    public TXMqttClient getMqttClient() {
        return mMqttClient;
    }

    /**
     * 初始化远程服务客户端
     *
     * @param clientOptions 客户端选项
     */
    public void init(TXMqttClientOptions clientOptions) {
        initListener();
        mMqttClient.init(clientOptions, mShadowActionListener);
    }


    /**
     * 设置断连状态buffer缓冲区
     *
     * @param bufferOpts
     */
    public void setBufferOpts(TXDisconnectedBufferOptions bufferOpts) {
        mMqttClient.setBufferOpts(bufferOpts);
    }

    /**
     * 与云端建立连接，结果通过回调函数通知
     *
     * @param connectOptions 连接参数
     * @param userContext    用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status connect(TXMqttConnectOptions connectOptions, Object userContext) {
        connectOptions.setUseShadow(true);
        return mMqttClient.connect(connectOptions, userContext);
    }

    /**
     * 断开连接请求，结果通过回调函数通知。
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status disConnect(Object userContext) {
        return mMqttClient.disConnect(userContext);
    }

    /**
     * 获取连接状态
     *
     * @return 连接状态
     */
    public TXMqttConstants.ConnectStatus getConnectStatus() {
        TXMqttConstants.ConnectStatus status = TXMqttConstants.ConnectStatus.kDisconnected;
        try {
            String statusStr = mMqttClient.mRemoteServer.getConnectStatus();
            status = Status.valueOf(TXMqttConstants.ConnectStatus.class, statusStr);
        } catch (Exception e) {
            LOG.error("{}", "invoke remote service[getConnectStatus] failed!", e);
        }
        return status;
    }

    /**
     * 获取设备影子
     */
    public Status get(Object userContext) {
        Status status = Status.ERROR;
        try {
            long requestId = mMqttClient.addUserContext(userContext);
            String statusStr = mMqttClient.mRemoteServer.getShadow(requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            LOG.error("{}", "invoke remote service[getShadow] failed!", e);
        }
        return status;
    }

    /**
     * 更新设备属性信息，结果通过回调函数通知。
     *
     * @param devicePropertyList 需要更新的设备属性集
     * @param userContext        用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status update(List<DeviceProperty> devicePropertyList, Object userContext) {
        Status status = Status.ERROR;
        try {
            long requestId = mMqttClient.addUserContext(userContext);
            String statusStr = mMqttClient.mRemoteServer.updateShadow(devicePropertyList, requestId);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            LOG.error("{}", "invoke remote service[updateShadow] failed!", e);
        }
        return status;
    }

    /**
     * 注册设备属性
     *
     * @param deviceProperty
     */
    public void registerProperty(DeviceProperty deviceProperty) {
        try {
            mMqttClient.mRemoteServer.registerDeviceProperty(deviceProperty);
        } catch (Exception e) {
            LOG.error("{}", "invoke remote service[registerDeviceProperty] failed!", e);
        }
    }

    /**
     * 取消注册设备属性
     *
     * @param deviceProperty
     */
    public void unRegisterProperty(DeviceProperty deviceProperty) {
        try {
            mMqttClient.mRemoteServer.unRegisterDeviceProperty(deviceProperty);
        } catch (Exception e) {
            LOG.error("{}", "invoke remote service[unRegisterDeviceProperty] failed!", e);
        }
    }

    /**
     * 更新delta信息后，上报空的desired信息，通知服务器不再发送delta消息。
     *
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportNullDesiredInfo() {
        return reportNullDesiredInfo(null);
    }

    /**
     * 更新delta信息后，上报空的desired信息，通知服务器不再发送delta消息。
     *
     * @param reportJsonDoc 用户上报的JSON内容
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportNullDesiredInfo(String reportJsonDoc) {
        Status status = Status.ERROR;
        try {
            String statusStr = mMqttClient.mRemoteServer.reportNullDesiredInfo(reportJsonDoc);
            status = Status.valueOf(Status.class, statusStr);
        } catch (Exception e) {
            LOG.error("{}", "invoke remote service[reportNullDesiredInfo] failed!", e);
        }
        return status;
    }

    /**
     * 释放资源
     */
    public void clear() {
        mMqttClient.clear();
    }

    /**
     * 初始化OTA功能。
     *
     * @param storagePath OTA升级包存储路径(调用者必确保路径已存在，并且具有写权限)
     * @param callback    OTA事件回调
     */
    public void initOTA(String storagePath, TXOTACallBack callback) {

        mMqttClient.initOTA(storagePath, callback);
    }

    /**
     * 上报设备当前版本信息到后台服务器。
     *
     * @param currentFirmwareVersion 设备当前版本信息
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportCurrentFirmwareVersion(String currentFirmwareVersion)  {

        return mMqttClient.reportCurrentFirmwareVersion(currentFirmwareVersion);
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
    public Status reportOTAState(TXOTAConstansts.ReportState state, int resultCode, String resultMsg, String version) {

        return mMqttClient.reportOTAState(state, resultCode, resultMsg, version);
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        mShadowActionListener = new ITXShadowActionListener() {
            public void onRequestCallback(String type, int result, String document)  {
                LOG.debug("onRequestCallback, type[{}], result[{}], document[{}]", type, result, document);
                if (null != mShadowActionCallBack) {
                    mShadowActionCallBack.onRequestCallback(type, result, document);
                }
            }

            public void onDevicePropertyCallback(String propertyJSONDocument, List<DeviceProperty> devicePropertyList) {
                LOG.debug("onDevicePropertyCallback, propertyJSONDocument[{}], devicePropertyList size[{}]",
                        propertyJSONDocument, devicePropertyList.size());
                for (DeviceProperty deviceProperty : devicePropertyList) {
                    LOG.debug(deviceProperty.toString());
                }
                if (null != mShadowActionCallBack) {
                    mShadowActionCallBack.onDevicePropertyCallback(propertyJSONDocument, devicePropertyList);
                }
            }

            public void onPublishCompleted(String status, TXMqttToken token, long userContextId, String errMsg) {
                LOG.debug("onPublishCompleted, status[{}], token[{}], errMsg[{}]", status, token, errMsg);
                if (null != mShadowActionCallBack) {
                    mShadowActionCallBack.onPublishCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(),
                            mMqttClient.getUserContext(Long.valueOf(userContextId)), errMsg);
                }
            }

            public void onSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg)  {
                LOG.debug("onSubscribeCompleted, status[{}], token[{}], errMsg[{}]", status, token, errMsg);
                if (null != mShadowActionCallBack) {
                    mShadowActionCallBack.onSubscribeCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(),
                            mMqttClient.getUserContext(Long.valueOf(userContextId)), errMsg);
                }
            }

            public void onUnSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg)  {
                LOG.debug("onUnSubscribeCompleted, status[{}], token[{}], errMsg[{}]", status, token, errMsg);
                if (null != mShadowActionCallBack) {
                    mShadowActionCallBack.onUnSubscribeCompleted(Status.valueOf(Status.class, status), token.transToMqttToken(),
                            mMqttClient.getUserContext(Long.valueOf(userContextId)), errMsg);
                }
            }

            public void onMessageReceived(String topic, TXMqttMessage message)  {
            	LOG.debug("onMessageReceived, topic[{}], message[{}]", topic, message);
                if (null != mShadowActionCallBack) {
                    mShadowActionCallBack.onMessageReceived(topic, message.transToMqttMessage());
                }
            }
        };
    }

}
