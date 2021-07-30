package com.tencent.iot.hub.device.java.core.shadow;

import  com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;

/**
 * 设备影子 Action 回调接口
 */
public abstract class TXShadowActionCallBack {

    /**
     * MQTT Connect 完成回调
     *
     * @param status Status.OK: 连接成功；Status.ERROR: 连接失败
     * @param reconnect true: 重新连接；false: 首次连接
     * @param userContext 用户上下文
     * @param msg 连接信息
     */
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
    }

    /**
     * MQTT 连接断开回调
     *
     * @param cause 连接断开原因
     */
    public void onConnectionLost(Throwable cause) {
    }

    /**
     * 文档请求响应的回调接口
     *
     * @param type 文档操作方式，get/update/delete
     * @param result 请求响应结果，0: 成功；非 0：失败
     * @param jsonDocument   云端返回的 json 文档
     */
    public void onRequestCallback(String type, int result, String jsonDocument) {
    }

    /**
     * 设备属性更新回调接口
     *
     * @param propertyJSONDocument 从云端收到的原始设备属性 json 文档
     * @param propertyList 更新后的设备属性集
     */
    public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends DeviceProperty> propertyList) {
    }


    /**
     * 收到来自云端的消息
     *
     * @param topic 主题名称
     * @param message 消息内容
     */
    public void onMessageReceived(String topic, MqttMessage message) {
    }


    /**
     * 发布消息完成回调
     *
     * @param status Status.OK: 发布消息成功；Status.ERROR: 发布消息失败
     * @param token 消息 token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg 详细信息
     */
    public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String msg) {
    }

    /**
     * 订阅主题完成回调
     *
     * @param status Status.OK: 订阅成功；Status.ERROR: 订阅失败
     * @param token 消息 token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg 详细信息
     */
    public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
    }

    /**
     * 取消订阅主题完成回调
     *
     * @param status Status.OK: 取消订阅成功；Status.ERROR: 取消订阅失败
     * @param token 消息 token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg 详细信息
     */
    public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
    }

}
