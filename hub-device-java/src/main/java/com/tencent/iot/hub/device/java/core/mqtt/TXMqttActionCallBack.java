package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public abstract class TXMqttActionCallBack {

    /**
     * MQTT Connect完成回调
     *
     * @param status        Status.OK: 连接成功； Status.ERROR: 连接失败
     * @param reconnect     true: 重新连接      false: 首次连接
     * @param userContext   用户上下文
     * @param msg           连接信息
     */
    public abstract void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg);

    /**
     * MQTT连接断开回调
     *
     * @param cause       连接断开原因
     */
    public abstract void onConnectionLost(Throwable cause);

    /**
     * MQTT Disconnect完成回调
     *
     * @param status      Status.OK: 断连成功； Status.ERROR: 断连失败
     * @param userContext 用户上下文
     * @param msg         详细信息
     */
    public abstract void onDisconnectCompleted(Status status, Object userContext, String msg);

    /**
     * 发布消息完成回调
     *
     * @param status      Status.OK: 发布消息成功； Status.ERROR: 发布消息失败
     * @param token       消息token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg         详细信息
     */
    public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String msg) {

    }

    /**
     * 订阅主题完成回调
     *
     * @param status      Status.OK: 订阅成功； Status.ERROR: 订阅失败
     * @param token       消息token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg        详细信息
     */
    public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext,  String msg) {

    }

    /**
     * 取消订阅主题完成回调
     *
     * @param status      Status.OK: 取消订阅成功； Status.ERROR: 取消订阅失败
     * @param token       消息token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg         详细信息
     */
    public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {

    }

    /**
     * 收到订阅主题的消息Push
     *
     * @param topic        主题名称
     * @param message      消息内容
     */
    public void onMessageReceived(String topic, MqttMessage message) {

    }
}
