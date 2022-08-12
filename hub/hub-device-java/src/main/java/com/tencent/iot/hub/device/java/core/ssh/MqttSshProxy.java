package com.tencent.iot.hub.device.java.core.ssh;


import static com.tencent.iot.hub.device.java.core.ssh.WebsocketSshConstants.REMOTE_WS_SSH_PATH;
import static com.tencent.iot.hub.device.java.core.ssh.WebsocketSshConstants.SSH_PREFIX;
import static com.tencent.iot.hub.device.java.core.ssh.WebsocketSshConstants.SSH_WS_SERVER_URL;

import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MqttSshProxy {

    private static final String TAG = MqttSshProxy.class.getName();
    private static final String HMAC_ALGO = "HmacSHA1";

    private TXMqttConnection mMqttConnection;

    // websocket client
    private WebSocketClient client = null;
    // ssh local socket
    private Socket socket = null;
    // websocket服务地址，自建服务器可设置
    private String websocketUrl = SSH_PREFIX + SSH_WS_SERVER_URL + REMOTE_WS_SSH_PATH;

    //随机数
    private volatile Integer nonce;
    //时间戳
    private volatile Integer timestamp;
    //会话token
    private volatile String token = "";

    /**
     * Attribute for a service that triggers lost connection checking
     *
     * @since 1.4.1
     */
    private ScheduledExecutorService connectionLostCheckerService;

    /**
     * Attribute for a task that checks for lost connections
     *
     * @since 1.4.1
     */
    private ScheduledFuture<?> connectionLostCheckerFuture;

    private static Thread listeningSshMsgThread = null;

    /**
     * 构造函数
     *
     * @param mqttConnection {@link TXMqttConnection}
     */
    public MqttSshProxy(TXMqttConnection mqttConnection, String sshHost, int sshPort) {
        this.mMqttConnection = mqttConnection;
        createSocketClient();
    }

    private void createSocketClient() {
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Sec-Websocket-Protocol", mMqttConnection.mProductId+"+"+mMqttConnection.mDeviceName);
        if (client == null) {
            client = new WebSocketClient(URI.create(websocketUrl), new Draft_6455(), httpHeaders, 0) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Loggor.debug(TAG, "WebsocketClientManager onOpen ");
                    websocketSshVerifyDevice();
                    startWebsocketSshPingThread();
                }

                @Override
                public void onMessage(String message) {
                    Loggor.debug(TAG, "WebsocketClientManager onMessage message: " + message);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    super.onMessage(bytes);
                    String message = new String(bytes.array(), Charset.forName("UTF-8"));
                    Loggor.debug(TAG, "WebsocketClientManager onMessage bytes: " + message);
                    processWebsocketMessage(message, bytes);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Loggor.error(TAG, "WebsocketClientManager onClose: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Loggor.debug(TAG, "WebsocketClientManager onError: " + ex.toString());
                }
            };
        }
        if (!client.isOpen()) {
            client.connect();
        } else {
            websocketSshVerifyDevice();
        }
    }

    private void processWebsocketMessage(String message, ByteBuffer bytes) {
        String[] splitMessage = message.split("\r\n");
        String messageHeader = "";
        String payload = "";
        if (splitMessage.length > 0) {
            messageHeader = splitMessage[0];
            payload = splitMessage[splitMessage.length - 1];
        }
        int msgType = -1;
        String token = "";
        int payloadLen = -1;
        if (messageHeader.length() > 0) {
            JSONObject header = new JSONObject(messageHeader);
            msgType = header.getInt("msgType");
            token = header.getString("token");
            payloadLen = header.getInt("payloadLen");
        }
        if (msgType == -1) {
            return;
        } else if (msgType == WebsocketSshMsgType.NEW_SESSION.getValue()) {
            websocketSshNewSession(token);
            this.token = token;
        } else if (msgType == WebsocketSshMsgType.RELEASE_SESSION.getValue()) {
            websocketSshReleaseSession(token);
        } else if (msgType == WebsocketSshMsgType.CMD_PONG.getValue()) {
            //心跳回复 pong
        } else if (msgType == WebsocketSshMsgType.SSH_RAWDATA.getValue()) {
            String header = messageHeader+"\r\n";
            byte[] b = header.getBytes();
            int l = b.length;
            byte[] buffer = new byte[bytes.array().length - l];
            System.arraycopy(bytes.array(), l, buffer, 0, bytes.array().length - l);
            this.token = token;
            localSshSend(buffer);
        } else if (msgType == WebsocketSshMsgType.VERIFY_DEVICE_RESP.getValue()) {
            websocketSshVerifyDeviceResult(token, 0);
        }
    }

    private String websocketSshMessageHeader(String token, WebsocketSshMsgType type, int payloadLen) {

        final JSONObject header = new JSONObject();
        try {
            header.put("requestId", "ws-" + nonce);
            header.put("msgType", type.getValue());
            header.put("payloadLen", payloadLen);
            header.put("serviceType", 0);
            header.put("timestmap", timestamp);
            header.put("token", token);

        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

        return header.toString()+"\r\n";
    }

    private void reloadRandAndTimestamp() {

        int randNum = (int)(Math.random() * ((1 << 31) - 1));
        this.nonce = randNum;
        int timestamp = (int)(System.currentTimeMillis() / 1000);
        this.timestamp = timestamp;
    }

    private void websocketSend(String sendString) {
        Loggor.debug(TAG, "client.send sendString: " + sendString);
        client.send(sendString.getBytes());
    }

    /**
     * 1 {"deviceName":"test","productId":"test","timestamp":123456,"rand":"12f2","version":"1.0","signMethod":"hmacsha256","sign":"xxxx"}
     * 2 签名的字符串 str := fmt.Sprintf("productId_%s_device_%s_timestamp_%d_rand_%s",productId,deviceName,timestamp,rand)
     * 3 密钥方式 signMethod=hmacsha256 key是设备密钥进行hmacsha256
     * 4 证书方式 signMethod=rsasha256 使用设备私钥 进行rsa签名
     * 5 算出sign后进行base64得到最后的签名字符串
     */
    private void websocketSshVerifyDevice() {

        reloadRandAndTimestamp();

        SecretKeySpec signKey = new SecretKeySpec(mMqttConnection.mSecretKey.getBytes(), HMAC_ALGO);
        String signSourceStr = String.format("productId_%s_device_%s_timestamp_%d_rand_%s", mMqttConnection.mProductId, mMqttConnection.mDeviceName, timestamp, String.valueOf(nonce));

        String hmacSign="";
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            if (mac != null) {
                mac.init(signKey);
                byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
                hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("productId", mMqttConnection.mProductId);
            payload.put("deviceName", mMqttConnection.mDeviceName);
            payload.put("timestamp", timestamp);
            payload.put("rand", String.valueOf(nonce));
            payload.put("version", "1.0");
            payload.put("signMethod", "hmacsha1");
            payload.put("sign", hmacSign);

        } catch (JSONException e) {
            e.printStackTrace();
            return ;
        }

        String header = websocketSshMessageHeader(String.valueOf(timestamp), WebsocketSshMsgType.VERIFY_DEVICE, payload.toString().length());
        String sendString = header+payload.toString();

        websocketSend(sendString);
    }

    private void websocketSshVerifyDeviceResult(String token, int ret) {
        if (ret == 0) {
            //校验成功
        } else {
            //校验失败Todo关闭websocket
        }
    }

    private void websocketSshNewSessionResp(String token, int ret) {
        final JSONObject payload = new JSONObject();
        if (ret == 0) {
            payload.put("code", 0);
            payload.put("message", "success");
        } else {
            payload.put("code", ret);
            payload.put("message", "fail");
        }
        final String header = websocketSshMessageHeader(token, WebsocketSshMsgType.NEW_SESSION_RESP, payload.toString().length());
        String sendString = header+payload.toString();
        websocketSend(sendString);
    }

    private void websocketSshNewSession(String token) {
        if (localSshCreate(token) == 0) {
            websocketSshNewSessionResp(token, 0);
        } else {
            websocketSshNewSessionResp(token, 1);
        }
    }

    /**
     * 设备建立ssh通道
     * @param token 令牌
     * @return 设备建立ssh通道结果
     */
    private int localSshCreate(String token) {

        //建立tcp连接
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            socket = new Socket(mMqttConnection.sshHost, mMqttConnection.sshPort);//创建Socket类对象
            startListeningSshLocalMessage(token);
            return 0;
        } catch (IOException e) {
            Loggor.debug(TAG, "socket init error message: " + e.toString());
            e.printStackTrace();
            return 1;
        }
    }
    /**
     * 销毁设备建立ssh通道
     * @param token 令牌
     */
    private void localSshDestory(String token) {
        try {
            if (listeningSshMsgThread != null) {
                listeningSshMsgThread.interrupt();
                listeningSshMsgThread = null;
            }
            if (socket != null) {
                socket.shutdownInput();
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void websocketSshReleaseSession(String token) {
        localSshDestory(token);
        websocketSshReleaseSessionResp(token, 0);
    }

    private void websocketSshReleaseSessionResp(String token, int ret) {
        JSONObject payload = new JSONObject();
        if (ret == 0) {
            payload.put("code", 0);
            payload.put("message", "success");
        } else {
            payload.put("code", ret);
            payload.put("message", "fail");
        }
        String header = websocketSshMessageHeader(token, WebsocketSshMsgType.RELEASE_SESSION_RESP, payload.toString().length());
        String sendString = header+payload.toString();
        websocketSend(sendString);
        //Todo断开websocket连接
    }

    private void websocketSshPing() {
        reloadRandAndTimestamp();
        String headerString = websocketSshMessageHeader(String.valueOf(timestamp), WebsocketSshMsgType.CMD_PING, 0);
        websocketSend(headerString);
    }

    private void startWebsocketSshPingThread() {
        connectionLostCheckerService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                final String threadName = String.format("tencent-pool-websocket-ssh-thread-%d", counter.incrementAndGet());
                return new Thread(r, threadName);
            }
        });
        Runnable connectionLostChecker = new Runnable() {

            @Override
            public void run() {
                websocketSshPing();
            }
        };
        connectionLostCheckerFuture = connectionLostCheckerService.scheduleAtFixedRate(connectionLostChecker, 5, 30, TimeUnit.SECONDS);
    }

    public void stopWebsocketSshPing() {
        if (connectionLostCheckerFuture != null) {
            connectionLostCheckerService.shutdown();
            connectionLostCheckerFuture.cancel(true);
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private void startListeningSshLocalMessage(final String token) {
        listeningSshMsgThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!listeningSshMsgThread.isInterrupted() && socket != null && socket.isConnected()) {
                        InputStream in = socket.getInputStream();
                        ByteArrayOutputStream result = new ByteArrayOutputStream();
                        byte[] buffer = new byte[2048];
                        int length;
                        length = in.read(buffer);
                        result.write(buffer, 0, length);
                        if (result.size()>0) {
                            websocketSshRawData(result.toString("UTF-8"), length, result.toByteArray());
                        }
                    }
                } catch (IOException e) {
                    Loggor.debug(TAG, "startListeningSshLocalMessage error message: " + e.toString());
                    socket = null;
                    e.printStackTrace();
                }
            }
        });
        listeningSshMsgThread.start();
    }

    /**
     * 设备ssh透传数据
     * @param payload 控制台透传ssh数据
     * @param payloadLen payload长度
     */
    private void websocketSshRawData(String payload, int payloadLen, byte[] buffer) {
        if (this.token != null && !this.token.equals("")) {
            reloadRandAndTimestamp();
            String header = websocketSshMessageHeader(token, WebsocketSshMsgType.SSH_RAWDATA, payloadLen);
            byte [] headerByte = header.getBytes();
            byte [] totalByte = new byte[headerByte.length + payloadLen];
            System.arraycopy(headerByte, 0, totalByte, 0, headerByte.length);
            System.arraycopy(buffer, 0, totalByte, headerByte.length, payloadLen);
            String sendString = header + payload;
            Loggor.debug(TAG, "client.send sendString: " + sendString + "payload" + payload);
            client.send(totalByte);
        }
    }

    /**
     * 设备ssh透传数据
     */
    private void localSshSend(byte[] buffer) {
        if (socket != null) {
            try {
                OutputStream out = socket.getOutputStream();
                out.write(buffer);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
