package com.tencent.iot.hub.device.java.service.interfaces;

import com.tencent.iot.hub.device.java.service.TXMqttMessage;
import com.tencent.iot.hub.device.java.service.TXMqttToken;

/**
 * MQTT 连接接口
 */
public interface ITXMqttActionListener {

    /**
     * MQTT Connect 完成回调
     *
     * @param status OK: 连接成功；ERROR: 连接失败
     * @param reconnect true: 重新连接；false: 首次连接
     * @param msg 连接信息
     * @param cause 连接失败原因，当Status.OK时，为null。
     */
    void onConnectCompleted(String status, boolean reconnect, long userContextId, String msg, Throwable cause);

    /**
     * MQTT连接断开回调
     *
     * @param cause 连接断开原因
     */
    void onConnectionLost(Throwable cause);

    /**
     * MQTT Disconnect 完成回调
     *
     * @param status OK: 断连成功；ERROR: 断连失败
     * @param msg 详细信息
     * @param cause 断连失败原因，当Status.OK时，为null。
     */
    void onDisconnectCompleted(String status, long userContextId, String msg, Throwable cause);

    /**
     * 发布消息完成回调
     *
     * @param status OK: 发布消息成功；ERROR: 发布消息失败
     * @param token 消息token
     * @param errMsg 详细信息
     * @param cause 发布消息失败原因，当Status.OK时，为null。
     */
    void onPublishCompleted(String status,  TXMqttToken token, long userContextId, String errMsg, Throwable cause);

    /**
     * 订阅主题完成回调
     *
     * @param status OK: 订阅成功；ERROR: 订阅失败
     * @param token 消息token
     * @param errMsg 详细信息
     * @param cause 订阅主题失败原因，当Status.OK时，为null。
     */
    void onSubscribeCompleted(String status,  TXMqttToken token, long userContextId, String errMsg, Throwable cause);

    /**
     * 取消订阅主题完成回调
     *
     * @param status OK: 取消订阅成功；ERROR: 取消订阅失败
     * @param token 消息token
     * @param errMsg 详细信息
     * @param cause 取消订阅主题失败原因，当Status.OK时，为null。
     */
    void onUnSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg, Throwable cause);

    /**
     * 收到订阅主题的消息Push
     *
     * @param topic 主题名称
     * @param message 消息内容
     */
    void onMessageReceived(String topic,  TXMqttMessage message);

    /**
     * 远程服务已启动回调接口
     */
    void onServiceStartedCallback();

    /**
     * 远程服务销毁回调接口
     */
    void onServiceDestroyCallback();

}
