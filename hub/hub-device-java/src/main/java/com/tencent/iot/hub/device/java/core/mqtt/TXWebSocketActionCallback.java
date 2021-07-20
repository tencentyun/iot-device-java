package com.tencent.iot.hub.device.java.core.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * websocket 连接回调
 */
public interface TXWebSocketActionCallback {

    /**
     * 上线回调
     */
    void onConnected();

    /**
     * 收到消息
     * @param topic 主题
     * @param message 消息 {@link MqttMessage}
     */
    void onMessageArrived(String topic, MqttMessage message);

    /**
     * 掉线
     * @param cause 掉线原因
     */
    void onConnectionLost(Throwable cause);

    /**
     * 离线
     */
    void onDisconnected();
}
