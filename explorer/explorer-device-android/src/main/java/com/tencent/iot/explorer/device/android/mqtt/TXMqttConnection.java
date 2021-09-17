package com.tencent.iot.explorer.device.android.mqtt;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_APP_BIND_TOKEN;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

import android.content.Context;
import android.util.Base64;

import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class TXMqttConnection extends com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection implements MqttCallbackExtended {

    public static final String TAG = "TXMQTT_" + MQTT_SDK_VER;
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private final String mWechatScanQRCodeContentUrl ="https://iot.cloud.tencent.com/iotexplorer/device";

    /**
     * tcp://localhost:port
     * ssl://localhost:port
     */
    protected Context mContext;

    protected TXAlarmPingSender mPingSender = null;


    /**
     * @param context    用户上下文（这个参数在回调函数时透传给用户）
     * @param productID  产品名
     * @param deviceName 设备名，唯一
	 * @param secretKey  密钥
     * @param callBack   连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, null, callBack);
    }

    /**
     * @param context    用户上下文（这个参数在回调函数时透传给用户）
     * @param productID  产品名
     * @param deviceName 设备名，唯一
	 * @param secretKey  密钥
     * @param bufferOpts 发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param callBack   连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * 使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
	 * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        this(context, null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI         服务器URI
     * @param productID         产品名
     * @param deviceName        设备名，唯一
     * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXMqttConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack) {
        super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mContext = context;
    }


    /**
     * 生成绑定设备的二维码字符串
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generateDeviceQRCodeContent() {
        // 格式为  ${product_id};${device_name};${random};${timestamp};hmacsha256;sign

        int randNum = (int) (Math.random() * 999999);
        long timestamp = System.currentTimeMillis() / 1000;
        String text2Sgin = mProductId + mDeviceName + ";" + randNum + ";" + timestamp;
        String signature = sign(text2Sgin, mSecretKey);
        String content = mProductId + ";" + mDeviceName + ";" + randNum + ";" + timestamp + ";hmacsha256;" + signature;
        return content;
    }


    /**
     * 生成支持微信扫一扫跳转连连小程序的绑定设备的二维码字符串
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generateDeviceWechatScanQRCodeContent() {
        // https://iot.cloud.tencent.com/iotexplorer/device?page=adddevice&productId=XXXXXXXX&device_sign=xxxxxxx
        String deviceSign = "";
        try {
            deviceSign = URLEncoder.encode(generateDeviceQRCodeContent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            TXLog.e(TAG, "toURLEncoded error:"+deviceSign+e.toString());
            return "";
        }
        String content = mWechatScanQRCodeContentUrl + "?page=adddevice&productId=" + mProductId + "&device_sign=" + deviceSign;
        return content;
    }

    private String sign(String src, String psk) {
        Mac mac;

        try {
            mac = Mac.getInstance(HMAC_SHA_256);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        String hmacSign;
        SecretKeySpec signKey = new SecretKeySpec(Base64.decode(psk, Base64.DEFAULT), HMAC_SHA_256);

        try {
            mac.init(signKey);
            byte[] rawHmac = mac.doFinal(src.getBytes());
            hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
            return hmacSign;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 连接MQTT服务器，结果通过回调函数通知。
     *
     * @param options     连接参数
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            TXLog.i(TAG, "The client is connecting. Connect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
            TXLog.i(TAG, "The client is already connected. Connect return directly.");
            return Status.OK;
        }

        this.mConnOptions = options;
        if (mConnOptions == null) {
            TXLog.e(TAG, "Connect options == null, will not connect.");
            return Status.PARAMETER_INVALID;
        }

        Long timestamp;
        if (options.isAutomaticReconnect()) {
            timestamp = (long) Integer.MAX_VALUE;
        } else {
            timestamp = System.currentTimeMillis()/1000 + 600;
        }
        String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;

        mConnOptions.setUserName(userNameStr);

        if (mSecretKey != null && mSecretKey.length() != 0) {
            try {
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(), Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
                mConnOptions.setPassword(passWordStr.toCharArray());
            }
            catch (IllegalArgumentException e) {
                TXLog.d(TAG, "Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                TXLog.i(TAG, "onSuccess!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + mServerURI, null);
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                TXLog.e(TAG, exception, "onFailure!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString(), exception);
            }
        };

        if (mMqttClient == null) {
            try {
                mPingSender = new TXAlarmPingSender(mContext);
                mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
                mMqttClient.setCallback(this);
                mMqttClient.setBufferOpts(this.bufferOpts);
                mMqttClient.setManualAcks(false);
            } catch (Exception e) {
                TXLog.e(TAG, "new MqttClient failed", e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        try {
            TXLog.i(TAG, "Start connecting to %s", mServerURI);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
            mMqttClient.connect(mConnOptions, userContext, mActionListener);
        } catch (Exception e) {
            TXLog.e(TAG, "MqttClient connect failed", e);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
            return Status.ERROR;
        }

        return Status.OK;
    }

    /**
     * 设备绑定App下发的token
     * @return 生成的绑定设备的二维码字符串;
     */
    public Status appBindToken(String token) {

        //构造发布信息
        JSONObject object = new JSONObject();
        JSONObject params = new JSONObject();
        String clientToken = mProductId + mDeviceName + UUID.randomUUID().toString();
        try {
            object.put("method", METHOD_APP_BIND_TOKEN);
            object.put("clientToken", clientToken);
            object.put("timestamp", System.currentTimeMillis());
            params.put("token", token);
            object.put("params", params);
        } catch (Exception e) {
            TXLog.e(TAG, "appBindToken: failed!");
            return Status.ERR_JSON_CONSTRUCT;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(1); //qos 1
        message.setPayload(object.toString().getBytes());
        return publish(TOPIC_SERVICE_UP_PREFIX + mProductId + "/" + mDeviceName, message, null);
    }

}
