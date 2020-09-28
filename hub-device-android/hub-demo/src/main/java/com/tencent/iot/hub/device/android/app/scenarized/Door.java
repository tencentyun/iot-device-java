package com.tencent.iot.hub.device.android.app.scenarized;

import android.content.Context;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Door {

    public static final String TAG = "iot.scenarized.Door";

    /**
     * 产品ID
     */
    private static final String PRODUCT_ID = "YOUR_PRODUCT_ID";

    /**
     * 设备名称
     */
    private static final String DEVICE_NAME = "YOUR_DEVICE_NAME";

    /**
     * 密钥
     */
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";
	
    /**
     * 设备证书名
     */
    private static final String DEVICE_CERT_NAME = "YOUR_DEVICE_NAME_cert.crt";

    /**
     * 设备私钥文件名
     */
    private static final String DEVICE_KEY_NAME = "YOUR_DEVICE_NAME_private.key";

    private static final String COME_HOME_MESSAGE = "{\"action\": \"come_home\", \"targetDevice\": \"" + Airconditioner.DEVICE_NAME + "\"}";
    private static final String LEAVE_HOME_MESSAGE = "{\"action\": \"leave_home\", \"targetDevice\": \"" + Airconditioner.DEVICE_NAME + "\"}";

    private Context mContext;

    private TXMqttConnection mqttConnection;

    private MqttConnectOptions options;

    public Door(Context context) {
        this.mContext = context;
    }


    /**
     * 进门
     */
    public void enterRoom() {
        if (mqttConnection == null) {
            mqttConnection = new TXMqttConnection(mContext, PRODUCT_ID, DEVICE_NAME, SECRET_KEY, new DoorMqttActionCallBack());

            options = new MqttConnectOptions();
            options.setConnectionTimeout(8);
            options.setKeepAliveInterval(240);
            options.setAutomaticReconnect(true);
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, DEVICE_CERT_NAME, DEVICE_KEY_NAME));

            mqttConnection.connect(options, null);
            DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
            bufferOptions.setBufferEnabled(true);
            bufferOptions.setBufferSize(1024);
            bufferOptions.setDeleteOldestMessages(true);
            mqttConnection.setBufferOpts(bufferOptions);
        }

        if (mqttConnection.getConnectStatus().equals(TXMqttConstants.ConnectStatus.kConnected)) {
            MqttMessage message = new MqttMessage();
            message.setPayload(COME_HOME_MESSAGE.getBytes());
            String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "event");
            mqttConnection.publish(topic, message, null);
        } else {
            //mqttConnection.connect(options, null);
        }
    }

    /**
     * 出门
     */
    public void leaveRoom() {
        if (null == mqttConnection) {
            TXLog.e(TAG, "please enter room first!");
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(LEAVE_HOME_MESSAGE.getBytes());
        String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "event");
        mqttConnection.publish(topic, message, null);

        closeConnection();
    }

    public void closeConnection() {
        if (null != mqttConnection) {
            mqttConnection.disConnect(null);
            mqttConnection = null;
        }
    }

    private class DoorMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            TXLog.i(TAG, "onConnectCompleted：" + msg);

            if (status.equals(Status.OK)) {
                if (!reconnect) {
                    MqttMessage message = new MqttMessage();
                    message.setPayload(COME_HOME_MESSAGE.getBytes());
                    String topic = String.format("%s/%s/%s", PRODUCT_ID, DEVICE_NAME, "event");
                    mqttConnection.publish(topic, message, null);
                }
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String logInfo = String.format("onDisconnectCompleted, status[%s], msg[%s]", status.name(), msg);
            TXLog.i(TAG, logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg) {
            String logInfo = String.format("onSubscribeCompleted, status[%s], message[%s]", status.name(), msg);
            if (Status.ERROR == status) {
                TXLog.e(TAG, logInfo);
            } else {
                TXLog.i(TAG, logInfo);
            }
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String msg) {
            super.onPublishCompleted(status, token, userContext, msg);
        }
    }
}
