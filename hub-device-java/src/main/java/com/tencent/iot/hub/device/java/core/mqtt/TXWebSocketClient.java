package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;


public class TXWebSocketClient extends MqttAsyncClient implements MqttCallbackExtended {

    private volatile TXWebSocketActionCallback connectListener;
    private boolean automicReconnect = true;
    private String clientId;
    private String secretKey = null;
    private MqttConnectOptions conOptions;
    // 状态机
    private AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);

    public TXWebSocketClient(String serverURI, String clientId) throws MqttException {
        super(serverURI, clientId, new MemoryPersistence());
        this.clientId = clientId;
        setCallback(this);
    }

    // 连接接口
    public IMqttToken connect() throws MqttException {
        if (state.get() == ConnectionState.CONNECTED) { // 已经连接过
            System.out.println("already connect");
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
        }
        IMqttToken ret = super.connect(conOptions);
        state.set(ConnectionState.CONNECTING);
        return ret;
    }

    // 重连接口
    public void reconnect() throws MqttException {
        super.reconnect();
    }

    public void setSecretKey(String secretKey, SocketFactory socketFactory) {
        this.secretKey = secretKey;

        // 设置密钥之后可以进行 mqtt 连接
        conOptions = new MqttConnectOptions();
        String userName = generateUsername();
        conOptions.setUserName(userName);
        conOptions.setPassword(generatePwd(userName).toCharArray());
        conOptions.setCleanSession(true);
        conOptions.setSocketFactory(socketFactory);
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
    public synchronized IMqttToken disconnect() throws MqttException {
        if (state.get() == ConnectionState.DISCONNECTED || state.get() == ConnectionState.DISCONNECTING) {      // 已经处于断开连接状态
            throw new MqttException(MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED);
        }

        IMqttToken ret = this.disconnect(null, mActionListener);
        state.set(ConnectionState.DISCONNECTING);   // 接口调用成功后重新设置状态
        return ret;
    }

    private void onDisconnected() {
        state.set(ConnectionState.DISCONNECTED);
        if (connectListener != null) {
            connectListener.onDisconnected();
        }
    }

    IMqttActionListener mActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            System.out.println("disconnect onSuccess");
            onDisconnected();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
            System.out.println("disconnect onFailure");
            onDisconnected();
        }
    };

    // 获取连接状态 true:上线 false:掉线
    public ConnectionState getConnectionState() {
        return state.get();
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

        return clientId + ";" + TXMqttConstants.APPID + ";" + getConnectId() + ";" + timestamp;
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

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        state.set(ConnectionState.CONNECTED);
        System.out.println("connectComplete");
        if (connectListener != null) {
            connectListener.onConnected();
        }

        // 根据实际情况注释
//        testPublish();
    }

    // 测试使用的自动发布消息
    private void testPublish() {
        MqttMessage msg = new MqttMessage();
        msg.setPayload("str".getBytes());
        msg.setQos(0);  // 最多发送一次，不做必达性保证
        System.out.println("start send");
        try {
            this.publish("/", msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("connectionLost");
        state.set(ConnectionState.CONNECTION_LOST);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("messageArrived");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("deliveryComplete");
    }
}
