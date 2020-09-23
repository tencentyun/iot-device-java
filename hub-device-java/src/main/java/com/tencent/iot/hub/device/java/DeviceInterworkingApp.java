package com.tencent.iot.hub.device.java;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.main.scenarized.Airconditioner;
import com.tencent.iot.hub.device.java.main.scenarized.Door;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class DeviceInterworkingApp {

    // Demo
    public static Airconditioner mAir;
    public static Door mDoor;
    private static int pubCount = 0;
    private static final int testCnt = 1000;

    public static void main(String[] args) {
        mAir = new Airconditioner(new AirconditionerMqttActionCallBack());
        mDoor = new Door();

        try {
            while(pubCount < testCnt) {
                pubCount += 1;
                Thread.sleep(20000);
                if (pubCount > 1) {
                    if (pubCount % 2 == 0) {
                        mDoor.enterRoom();
                    }
                    if (pubCount % 2 == 1) {
                        mDoor.leaveRoom();
                    }
                }
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mAir.closeConnection();
    }

    private static class AirconditionerMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            System.out.println(msg);

            if (status.equals(Status.OK)) {
                mAir.subScribeTopic();
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            System.out.println(logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String logInfo = String.format("onDisconnectCompleted, status[%s], msg[%s]", status.name(), msg);
            System.out.println(logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
            String logInfo = String.format("onSubscribeCompleted, status[%s], message[%s]", status.name(), msg);
            if (Status.ERROR == status) {
                System.out.println(logInfo);
            } else {
                System.out.println(logInfo);
            }
        }

        @Override
        public void onMessageReceived(String topic, MqttMessage message) {
            String logInfo = String.format("Airconditioner onMessageReceived, topic[%s], message[%s]", topic, message.toString());
            System.out.println(logInfo);

            if (message.toString().contains("come_home")) {
                logInfo = "receive command: open airconditioner ";
            } else {
                logInfo = "receive command: close airconditioner ";
            }

            System.out.println(logInfo);
        }
    }
}
