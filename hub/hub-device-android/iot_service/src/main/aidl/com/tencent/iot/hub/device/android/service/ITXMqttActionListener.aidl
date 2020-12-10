// ITXMqttListener.aidl
package com.tencent.iot.hub.device.android.service;

import com.tencent.iot.hub.device.android.service.TXMqttMessage;
import com.tencent.iot.hub.device.android.service.TXMqttToken;

interface ITXMqttActionListener {

    /**
     * MQTT Connect完成回调
     *
     * @param status    OK: 连接成功        ERROR: 连接失败
     * @param reconnect true: 重新连接      false: 首次连接
     * @param msg       连接信息
     */
    void onConnectCompleted(String status, boolean reconnect, long userContextId, String msg);

    /**
     * MQTT连接断开回调
     *
     * @param cause 连接断开原因
     */
    void onConnectionLost(String cause);

    /**
     * MQTT Disconnect完成回调
     *
     * @param status OK: 断连成功，ERROR: 断连失败
     * @param msg    相信信息
     */
    void onDisconnectCompleted(String status, long userContextId, String msg);

    /**
     * 发布消息完成回调
     *
     * @param status OK: 发布消息成功，ERROR: 发布消息失败
     * @param token  消息token
     * @param errMsg 详细信息
     */
    void onPublishCompleted(String status, inout TXMqttToken token, long userContextId, String errMsg);

    /**
     * 订阅主题完成回调
     *
     * @param status  OK: 订阅成功，ERROR: 订阅失败
     * @param token   消息token
     * @param errMsg  详细信息
     */
    void onSubscribeCompleted(String status, inout TXMqttToken token, long userContextId, String errMsg);

    /**
     * 取消订阅主题完成回调
     *
     * @param status    OK: 取消订阅成功，ERROR: 取消订阅失败
     * @param token     消息token
     * @param errMsg    详细信息
     */
    void onUnSubscribeCompleted(String status, inout TXMqttToken token, long userContextId, String errMsg);

    /**
     * 收到订阅主题的消息Push
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    void onMessageReceived(String topic, inout TXMqttMessage message);

    /**
     * 远程服务已启动回调接口
     */
    void onServiceStartedCallback();

    /**
     * 远程服务销毁回调接口
     */
    void onServiceDestroyCallback();

}
