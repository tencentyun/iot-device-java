package com.tencent.iot.hub.device.java.core.shadow;

import  com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;

/**
 * 设备影子Action回调接口
 */

public abstract class TXShadowActionCallBack {

    /**
     * 文档请求响应的回调接口
     *
     * @param type 文档操作方式, get/update/delete
     * @param result 请求响应结果, 0: 成功；非0：失败
     * @param jsonDocument   云端返回的json文档
     */
    public void onRequestCallback(String type, int result, String jsonDocument) {
    }

    /**
     * 设备属性更新回调接口
     *
     * @param propertyJSONDocument 从云端收到的原始设备属性json文档
     * @param propertyList   更新后的设备属性集
     */
    public void onDevicePropertyCallback(String propertyJSONDocument, List<? extends DeviceProperty> propertyList) {
    }


    /**
     * 收到来自云端的消息
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    public void onMessageReceived(String topic, MqttMessage message) {
    }


    /**
     * 发布消息完成回调
     *
     * @param status        Status.OK: 发布消息成功； Status.ERROR: 发布消息失败
     * @param token         消息token，包含消息内容结构体
     * @param userContext   用户上下文
     * @param msg           详细信息
     */
    public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String msg) {
    }

    /**
     * 订阅主题完成回调
     *
     * @param status           Status.OK: 订阅成功； Status.ERROR: 订阅失败
     * @param token            消息token，包含消息内容结构体
     * @param userContext      用户上下文
     * @param msg              详细信息
     */
    public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
    }

    /**
     * 取消订阅主题完成回调
     *
     * @param status           Status.OK: 取消订阅成功； Status.ERROR: 取消订阅失败
     * @param token            消息token，包含消息内容结构体
     * @param userContext      用户上下文
     * @param msg              详细信息
     */
    public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
    }
    
    public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {

	}

	public void onConnectionLost(Throwable cause) {
	}
}
