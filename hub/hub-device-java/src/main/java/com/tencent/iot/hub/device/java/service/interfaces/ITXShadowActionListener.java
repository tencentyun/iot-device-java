// ITXShadowListener.aidl
package com.tencent.iot.hub.device.java.service.interfaces;

import com.tencent.iot.hub.device.java.service.TXMqttMessage;
import com.tencent.iot.hub.device.java.service.TXMqttToken;

import java.util.List;

import com.tencent.iot.hub.device.java.core.shadow.DeviceProperty;

public interface ITXShadowActionListener {

    /**
     * 文档请求响应的回调接口
     *
     * @param type     文档操作方式, get/update/delete
     * @param result   请求响应结果, 0: 成功；非0：失败
     * @param document 云端返回的JSON文档
     */
    void onRequestCallback(String type, int result, String document);

    /**
     * 设备属性处理回调接口
     *
     * @param propertyJSONDocument 设备属性json文档
     * @param devicePropertyList   更新后的设备属性集
     */
    void onDevicePropertyCallback(String propertyJSONDocument,  List<DeviceProperty> devicePropertyList);

    /**
     * 发布消息完成回调
     *
     * @param status OK: 发布消息成功，ERROR: 发布消息失败
     * @param token  消息token
     * @param errMsg 详细信息
     */
    void onPublishCompleted(String status, TXMqttToken token, long userContextId, String errMsg);

    /**
     * 订阅主题完成回调
     *
     * @param status     OK: 订阅成功，ERROR: 订阅失败
     * @param token      消息token
     * @param errMsg     详细信息
     */
    void onSubscribeCompleted(String status, TXMqttToken token, long userContextId, String errMsg);

    /**
     * 取消订阅主题完成回调
     *
     * @param status    OK: 取消订阅成功，ERROR: 取消订阅失败
     * @param token     消息token，包含消息内容结构体
     * @param errMsg    详细信息
     */
    void onUnSubscribeCompleted(String status,TXMqttToken token, long userContextId, String errMsg);

    /**
     * 收到订阅主题的消息Push
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    void onMessageReceived(String topic, TXMqttMessage message);
}
