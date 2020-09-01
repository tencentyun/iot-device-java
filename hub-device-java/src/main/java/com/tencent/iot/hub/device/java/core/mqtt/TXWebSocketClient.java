package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;


public class TXWebSocketClient extends MqttClient {

    // 开始连接和未连接两种状态(主动触发改变的状态)
    private volatile boolean isConnected = false;
    private volatile TXWebSocketActionCallback connectListener;
    private boolean automicReconnect = true;
    private String clientId;
    private String secretKey = null;
    private MqttConnectOptions conOptions;

    public TXWebSocketClient(String serverURI, String clientId, String path) throws MqttException {
        super(serverURI, clientId, new MemoryPersistence());//new MqttDefaultFilePersistence(path));
        this.clientId = clientId;

        this.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("connectionLost");
                if (connectListener != null) {
                    connectListener.onConnectionLost(cause);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("messageArrived");
                if (connectListener != null) {
                    connectListener.onMessageArrived(topic, message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("deliveryComplete");
            }
        });
    }

    // 同步绑定接口
    public boolean connectWithResult() {
        try {
            if (isConnected) {
                System.out.println("already connect");
                return true;
            }
            IMqttToken token = this.connectWithResult(conOptions);

            return isConnected = token.isComplete();
        } catch (MqttException e) {
            e.printStackTrace();
            System.out.println("MqttException");
        }
        return false;
    }

    // 发布消息的接口
    @Override
    public void publish(String topic, MqttMessage message) throws MqttException {
        super.publish(topic, message);
    }

    // 异步连接接口，根据实际情况放开
    public void connect() {
        System.out.println("conected " + isConnected());

        try {
            this.connect(conOptions);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;

        // 设置密钥之后可以进行 mqtt 连接
        conOptions = new MqttConnectOptions();
        String userName = generateUsername();
        conOptions.setUserName(userName);
        System.out.println("userName " + userName);
        conOptions.setPassword(generatePwd(userName).toCharArray());
        System.out.println("pwd " + generatePwd(userName));
        System.out.println("clientId " + clientId);
        conOptions.setCleanSession(true);
        conOptions.setSocketFactory(AsymcSslUtils.getSocketFactory());
        conOptions.setAutomaticReconnect(true);
        conOptions.setKeepAliveInterval(60);
        conOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setTXWebSocketActionCallback(TXWebSocketActionCallback connectListener) {
        this.connectListener = connectListener;
    }

    public TXWebSocketActionCallback getTXWebSocketActionCallback() {
        return this.connectListener;
    }

    // 主动断开连接
    public synchronized void destroy() {
        isConnected = false;
        try {
            this.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 获取连接状态 true:上线 false:掉线
    public boolean isConnected() {
        return isConnected;
    }

    private String generatePwd(String userName) {
        System.out.println("secretKey=" + secretKey);
        if (secretKey != null) {
            try {
                String passWordStr = HmacSha256.getSignature(userName.getBytes(),
                        Base64.decode(secretKey, Base64.DEFAULT)) + ";hmacsha256";
                return passWordStr;
            } catch (IllegalArgumentException e) {
                System.out.println("Failed to set password");
            }
        }
        return null;
    }

    private String generateUsername() {

        Long timestamp;
        if (automicReconnect) {
            timestamp = (long) Integer.MAX_VALUE;
        } else {
            timestamp = System.currentTimeMillis() / 1000 + 600;
        }

        String userNameStr = clientId + ";" + TXMqttConstants.APPID + ";" + getConnectId() + ";" + timestamp;
        return userNameStr;
    }

    protected String getConnectId() {
        StringBuffer connectId = new StringBuffer();
        for (int i = 0; i < TXMqttConstants.MAX_CONN_ID_LEN; i++) {
            int flag = (int) (Math.random() * Integer.MAX_VALUE) % 3;
            int randNum = (int) (Math.random() * Integer.MAX_VALUE);
            switch (flag) {
                case 0:
                    connectId.append((char) (randNum % 26 + 'a'));
                    break;
                case 1:
                    connectId.append((char) (randNum % 26 + 'A'));
                    break;
                case 2:
                    connectId.append((char) (randNum % 10 + '0'));
                    break;
            }
        }

        return connectId.toString();
    }
}
