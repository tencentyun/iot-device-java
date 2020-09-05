package com.tencent.iot.hub.device.java.core.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface TXWebSocketActionCallback {

    // 上线回调
    void onConnected();

    void onMessageArrived(String topic, MqttMessage message);

    // 掉线回调
    void onConnectionLost(Throwable cause);

    void onDisconnected();
}
