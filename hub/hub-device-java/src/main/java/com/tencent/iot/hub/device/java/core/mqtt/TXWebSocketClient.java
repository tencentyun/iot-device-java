package com.tencent.iot.hub.device.java.core.mqtt;

import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.SocketFactory;

/**
 * websocket 连接类
 */
public class TXWebSocketClient extends MqttAsyncClient implements MqttCallbackExtended {

    private static final String TAG = TXWebSocketClient.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(TXWebSocketClient.class);
    static { Loggor.setLogger(logger); }

    private volatile TXWebSocketActionCallback connectListener;
    private boolean automicReconnect = true;
    private String clientId;
    private String secretKey = null;
    private MqttConnectOptions conOptions;
    // 状态机
    private AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);

    /**
     * 构造函数
     *
     * @param serverURI 服务器 URI
     * @param clientId 客户端 ID
     * @param secretKey 密钥认证设备该参数为 设备密钥  证书认证设备该参数为 设备私钥
     * @throws MqttException
     */
    public TXWebSocketClient(String serverURI, String clientId, String secretKey) throws MqttException {
        super(serverURI, clientId, new MemoryPersistence());
        this.secretKey = secretKey;
        this.clientId = clientId;
        setCallback(this);
    }

    /**
     * 连接
     *
     * @return mqtt token {@link IMqttToken}
     * @throws MqttException {@link MqttException}
     */
    public IMqttToken connect() throws MqttException {
        if (state.get() == ConnectionState.CONNECTED) { // 已经连接过
            Loggor.debug(TAG, "already connect");
            throw new MqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
        }

        IMqttToken ret = super.connect(conOptions);
        state.set(ConnectionState.CONNECTING);
        ret.waitForCompletion(-1);
        return ret;
    }

    /**
     * 重连
     *
     * @throws MqttException {@link MqttException}
     */
    public void reconnect() throws MqttException {
        super.reconnect();
    }

    /**
     * 设置连接配置，无需设置 username 和 password，内部会自动填充
     *
     * @param mqttConnectOptions 连接参数 {@link MqttConnectOptions}
     */
    public void setMqttConnectOptions(MqttConnectOptions mqttConnectOptions) {
        this.conOptions = mqttConnectOptions;

        // 设置密钥之后可以进行 mqtt 连接
        String userName = generateUsername();
        conOptions.setUserName(userName);
        if (getIsPskDevice()) {
            try {
                conOptions.setPassword(generatePwd(userName).toCharArray());
            } catch (IllegalArgumentException e) {
                Loggor.debug(TAG, "Failed to set password");
            }
        } else {
            String base64String = sha256WithRSA(userName, secretKey);
            String hexString = toHex(Base64.decode(base64String, Base64.DEFAULT));

            String password = hexString + ";rsa-sha256";
            conOptions.setPassword(password.toCharArray());
        }
        conOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    }

    private boolean getIsPskDevice() {
        if (secretKey != null && secretKey.length() != 0) {
            return !secretKey.contains("BEGIN PRIVATE KEY");
        }
        return false;
    }

    private String sha256WithRSA(String input, String secret) {
        String privateKey = secret.replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("\n", "");

        byte[] sign = new byte[0];
        byte[] decodedPrivKey = Base64.decode(privateKey, Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedPrivKey);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            Signature privateSignature = Signature.getInstance("SHA256withRSA");
            privateSignature.initSign(kf.generatePrivate(spec));
            privateSignature.update(input.getBytes(Charset.forName("UTF-8")));
            sign = privateSignature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        return Base64.encodeToString(sign, Base64.NO_WRAP);
    }

    private static final char[] DIGITS
            = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final String toHex(byte[] data) {
        final StringBuffer sb = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            sb.append(DIGITS[(data[i] >>> 4) & 0x0F]);
            sb.append(DIGITS[data[i] & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * 获取密钥
     *
     * @return 密钥认证设备该参数为 设备密钥  证书认证设备该参数为 设备私钥
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * 设置连接监听器
     *
     * @param connectListener {@link TXWebSocketActionCallback}
     */
    public void setTXWebSocketActionCallback(TXWebSocketActionCallback connectListener) {
        this.connectListener = connectListener;
    }

    /**
     * 获取连接监听器
     *
     * @return {@link TXWebSocketActionCallback}
     */
    public TXWebSocketActionCallback getTXWebSocketActionCallback() {
        return this.connectListener;
    }

    /**
     * 断开连接
     *
     * @return mqtt token {@link IMqttToken}
     * @throws MqttException {@link MqttException}
     */
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
            Loggor.debug(TAG, "connectListener.onDisconnected()");
            connectListener.onDisconnected();
        }
    }

    IMqttActionListener mActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Loggor.debug(TAG, "disconnect onSuccess");
            onDisconnected();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
            Loggor.error(TAG, "disconnect onFailure");
            onDisconnected();
        }
    };

    /**
     * 获取连接状态
     *
     * @return {@link ConnectionState}
     */
    public ConnectionState getConnectionState() {
        return state.get();
    }

    private String generatePwd(String userName) {
        if (secretKey != null) {
            try {
                String passWordStr = HmacSha256.getSignature(userName.getBytes(),
                        Base64.decode(secretKey, Base64.DEFAULT)) + ";hmacsha256";
                return passWordStr;
            } catch (IllegalArgumentException e) {
                Loggor.error(TAG, "Failed to set password");
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

    /**
     * 连接完成
     *
     * @param reconnect 是否重连
     * @param serverURI 服务器 URI
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        state.set(ConnectionState.CONNECTED);
        Loggor.debug(TAG, "connectComplete");
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
        Loggor.debug(TAG, "start send");
        try {
            this.publish("/", msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 掉线
     *
     * @param cause 掉线原因
     */
    @Override
    public void connectionLost(Throwable cause) {
        Loggor.error(TAG, "connectionLost");
        state.set(ConnectionState.CONNECTION_LOST);
    }

    /**
     * 收到消息
     *
     * @param topic 主题
     * @param message 消息 {@link MqttMessage}
     * @throws Exception
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Loggor.debug(TAG, "messageArrived");
    }

    /**
     * 发送消息完成
     *
     * @param token {@link IMqttDeliveryToken}
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Loggor.debug(TAG, "deliveryComplete");
    }
}
