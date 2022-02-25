package com.tencent.iot.hub.device.java.core.websocket;


import static com.tencent.iot.hub.device.java.core.websocket.WebsocketConstants.REMOTE_WS_SSH_PATH;
import static com.tencent.iot.hub.device.java.core.websocket.WebsocketConstants.SSH_PREFIX;
import static com.tencent.iot.hub.device.java.core.websocket.WebsocketConstants.SSH_WS_SERVER_URL;

import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

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

public class WebsocketClientManager {

    private static final String TAG = WebsocketClientManager.class.getName();

    private static WebsocketClientManager instance;

    // websocket client
    private WebSocketClient client = null;

    private String host = SSH_PREFIX + SSH_WS_SERVER_URL + REMOTE_WS_SSH_PATH;

    private static final String HMAC_ALGO = "HmacSHA1";

    /**
     * 产品 ID
     */
    public String mProductId;
    /**
     * 设备名
     */
    public String mDeviceName;
    /**
     * 密钥
     */
    private String mDevicePsk;
    /**
     * 随机数
     */
    private volatile Integer nonce;
    /**
     * 时间戳
     */
    private volatile Integer timestamp;

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

    private WebsocketSshCallback websocketSshCallback;

    /**
     * 获取单例
     *
     * @return 单例实体
     */
    public synchronized static WebsocketClientManager getInstance() {
        if (instance == null) {
            instance = new WebsocketClientManager();
        }
        return instance;
    }

    public void setWebsocketSshCallback(WebsocketSshCallback callback) {
        this.websocketSshCallback = callback;
    }

    public synchronized void createSocketClient(String productId, String deviceName, String devicePsk) {
        mProductId = productId;
        mDeviceName = deviceName;
        mDevicePsk = devicePsk;
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Sec-Websocket-Protocol", mProductId+"+"+mDeviceName);
        if (client == null) {
            client = new WebSocketClient(URI.create(host), new Draft_6455(), httpHeaders, 0) {
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
                    processWebsocketMessage(message);
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

    private void processWebsocketMessage(String message) {
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
        } else if (msgType == WebsocketMsgType.NEW_SESSION.getValue()) {
            websocketSshNewSession(token);
        } else if (msgType == WebsocketMsgType.RELEASE_SESSION.getValue()) {
            websocketSshReleaseSession(token);
        } else if (msgType == WebsocketMsgType.CMD_PONG.getValue()) {
            websocketSshPong();
        } else if (msgType == WebsocketMsgType.SSH_RAWDATA.getValue()) {
            websocketSshRawData(token, payload, payloadLen);
        } else if (msgType == WebsocketMsgType.VERIFY_DEVICE_RESP.getValue()) {
            websocketSshVerifyDeviceResult(token, 0);
        }
    }

    private String websocketSshMessageHeader(String token, WebsocketMsgType type, int payloadLen) {

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

        SecretKeySpec signKey = new SecretKeySpec(mDevicePsk.getBytes(), HMAC_ALGO);
        String signSourceStr = String.format("productId_%s_device_%s_timestamp_%d_rand_%s", mProductId, mDeviceName, timestamp, String.valueOf(nonce));

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
            payload.put("productId", mProductId);
            payload.put("deviceName", mDeviceName);
            payload.put("timestamp", timestamp);
            payload.put("rand", String.valueOf(nonce));
            payload.put("version", "1.0");
            payload.put("signMethod", "hmacsha1");
            payload.put("sign", hmacSign);

        } catch (JSONException e) {
            e.printStackTrace();
            return ;
        }

        String header = websocketSshMessageHeader(String.valueOf(timestamp), WebsocketMsgType.VERIFY_DEVICE, payload.toString().length());
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
        final String header = websocketSshMessageHeader(token, WebsocketMsgType.NEW_SESSION_RESP, payload.toString().length());
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
        if (websocketSshCallback != null) {
            websocketSshCallback.localSshCreate();
        }
        return 0;
    }
    /**
     * 销毁设备建立ssh通道
     * @param token 令牌
     */
    private void localSshDestory(String token) {

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
        String header = websocketSshMessageHeader(token, WebsocketMsgType.RELEASE_SESSION_RESP, payload.toString().length());
        String sendString = header+payload.toString();
        websocketSend(sendString);
        //Todo断开websocket连接
    }

    private void websocketSshPing() {
        reloadRandAndTimestamp();
        String headerString = websocketSshMessageHeader(String.valueOf(timestamp), WebsocketMsgType.CMD_PING, 0);
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
                Loggor.debug(TAG, "startWebsocketSshPingThread: ");
                websocketSshPing();
            }
        };
        connectionLostCheckerFuture = connectionLostCheckerService.scheduleAtFixedRate(connectionLostChecker, 5, 30, TimeUnit.SECONDS);

    }

    private void websocketSshPong() {
        //Todo已经收到Pong30s后发送下一个Ping
    }

    private void websocketSshRawData(String token, String payload, int payloadLen) {
        if (websocketSshCallback != null) {
            websocketSshCallback.websocketSshRawData(token, payload, payloadLen);
        }
        localSshSend(token, payload, payloadLen);
    }

    /**
     * 设备ssh透传数据
     * @param token 令牌
     * @param payload 控制台透传ssh数据
     * @param payloadLen payload长度
     */
    private void localSshSend(String token, String payload, int payloadLen) {
        reloadRandAndTimestamp();
        String header = websocketSshMessageHeader(token, WebsocketMsgType.SSH_RAWDATA, payload.length());
        String sendString = header+payload;
        websocketSend(sendString);
    }
}
