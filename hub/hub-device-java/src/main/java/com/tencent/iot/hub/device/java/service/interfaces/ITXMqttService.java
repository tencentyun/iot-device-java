package com.tencent.iot.hub.device.java.service.interfaces;

import com.tencent.iot.hub.device.java.core.shadow.DeviceProperty;
import com.tencent.iot.hub.device.java.service.TXDisconnectedBufferOptions;
import com.tencent.iot.hub.device.java.service.TXMqttClientOptions;
import com.tencent.iot.hub.device.java.service.TXMqttConnectOptions;
import com.tencent.iot.hub.device.java.service.TXMqttMessage;

import java.util.List;

/**
 * MQTT 服务接口
 */
public interface ITXMqttService {

    /**
     * 注册 mqttAction 监听器
     *
     * @param mqttActionListener {@link ITXMqttActionListener}
     */
    void registerMqttActionListener(ITXMqttActionListener mqttActionListener);

    /**
     * 注册 shadowAction 监听器
     *
     * @param shadowActionListener {@link ITXShadowActionListener}
     */
    void registerShadowActionListener(ITXShadowActionListener shadowActionListener);

    /**
     * 初始化设备信息
     *
     * @param clientOptions 客户端选项 {@link TXMqttClientOptions}
     */
    void initDeviceInfo(TXMqttClientOptions clientOptions);

    /**
     * 设置断连状态 buffer 缓冲区
     *
     * @param bufferOptions {@link TXDisconnectedBufferOptions}
     */
    void setBufferOpts(TXDisconnectedBufferOptions bufferOptions);

    /**
     * 连接 MQTT
     *
     * @param  options 链接配置 {@link TXMqttConnectOptions}
     * @param  userContextId 上下文请求 ID
     * @return 接口调用状态
     */
    String connect(TXMqttConnectOptions options, long userContextId);

    /**
     * 重新连接
     *
     * @return 接口调用状态
     */
    String reconnect();

    /**
     * MQTT 断连
     *
     * @param timeout 等待时间（必须>0）。单位：毫秒
     * @param userContextId 用户上下文
     * @return 接口调用状态
     */
    String disConnect(long timeout, long userContextId);

    /**
     * 订阅主题
     *
     * @param topic 主题
     * @param qos 消息 qos
     * @param userContextId 上下文请求 ID
     * @return 接口调用状态
     */
    String subscribe(String topic, int qos, long userContextId);

    /**
     * 取消订阅主题
     *
     * @param topic 主题
     * @param userContextId 上下文请求 ID
     * @return 接口调用状态
     */
    String unSubscribe(String topic, long userContextId);

    /**
     * 发布主题
     *
     * @param topic 主题
     * @param message 消息 {@link TXMqttMessage}
     * @param userContextId 上下文请求 ID
     * @return 接口调用状态
     */
    String publish(String topic, TXMqttMessage message, long userContextId);

    /**
     * 获取连接状态
     *
     * @return 连接状态
     */
    String getConnectStatus();

    /**
     * 获取设备影子
     *
     * @param userContextId 上下文请求 ID
     * @return 设备影子
     */
    String getShadow(long userContextId);

    /**
     * 更新设备影子
     *
     * @param devicePropertyList 设备属性
     * @param userContextId 上下文请求 ID
     * @return 接口调用状态
     */
    String updateShadow(List<DeviceProperty> devicePropertyList, long userContextId);

    /**
     * 注册设备属性
     *
     * @param deviceProperty 设备属性 {@link DeviceProperty}
     */
    void registerDeviceProperty(DeviceProperty deviceProperty);

    /**
     * 取消注册设备属性
     *
     * @param deviceProperty 设备属性 {@link DeviceProperty}
     */
    void unRegisterDeviceProperty(DeviceProperty deviceProperty);

    /**
     * 更新 delta 信息后，上报空的 desired 信息，通知服务器不再发送 delta 消息
     *
     * @param reportJsonDoc 用户上报的 JSON 内容
     * @return 接口调用状态
     */
    String reportNullDesiredInfo(String reportJsonDoc);

    /**
     * 初始化OTA功能。
     *
     * @param storagePath OTA升级包存储路径(调用者必须确保路径已存在，并且具有写权限)
     * @param listener OTA事件回调
     */
    void initOTA(String storagePath, ITXOTAListener listener);

    /**
     * 上报设备当前版本信息到后台服务器。
     *
     * @param currentFirmwareVersion 设备当前版本信息
     * @return 发送成功时返回字符串"OK"；其它返回值表示发送失败
     */
    String reportCurrentFirmwareVersion(String currentFirmwareVersion);

     /**
     * 上报设备升级状态到后台服务器。
     *
     * @param state 状态
     * @param resultCode 结果代码。0：表示成功；其它：表示失败；常见错误码：-1: 下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5：更新固件失败
     * @param resultMsg 结果描述
     * @param version 版本号
     * @return 发送成功时返回字符串"OK"；其它返回值表示发送失败
     */
    String reportOTAState(String state, int resultCode, String resultMsg, String version);
}
