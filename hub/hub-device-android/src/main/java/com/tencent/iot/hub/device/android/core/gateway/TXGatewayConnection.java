package com.tencent.iot.hub.device.android.core.gateway;

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SDK_VER;

import android.content.Context;
import android.util.Base64;

import com.tencent.iot.hub.device.android.core.mqtt.TXAlarmPingSender;
import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.gateway.TXGatewaySubdev;
import com.tencent.iot.hub.device.java.core.log.TXMqttLogCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 网关连接类
 */
public class TXGatewayConnection  extends TXMqttConnection {
    /**
     * 类标记
     */
    private static final String TAG = "TXMQTT" + MQTT_SDK_VER;
    private static final String HMAC_SHA_256 = "HmacSHA256";

    private HashMap<String, TXGatewaySubdev> mSubdevs = new HashMap<String, TXGatewaySubdev>();
    private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";
    private static final String GW_OPERATION_PREFIX = "$gateway/operation/";
    private static final String PRODUCT_CONFIG_PREFIX = "$config/operation/result/";

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param serverURI 服务器 URI
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param mqttLogFlag 是否开启日志功能
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXGatewayConnection(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                               MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, mqttLogFlag,logCallBack,callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param serverURI 服务器 URI
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param mqttLogFlag 是否开启日志功能
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 url
     */
    public TXGatewayConnection(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                               MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, mqttLogFlag,logCallBack,callBack,logUrl);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param serverURI 服务器 URI
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param mqttLogFlag 是否开启日志功能
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     * @param logUrl 日志上报 url
     * @param sshHost ssh 要访问的IP
     * @param sshPort ssh 端口号
     */
    public TXGatewayConnection(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                               MqttClientPersistence clientPersistence, Boolean mqttLogFlag, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack, String logUrl, String sshHost, int sshPort) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, mqttLogFlag,logCallBack,callBack,logUrl, sshHost, sshPort);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）{@link Context}
     * @param serverURI 服务器 URI
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param logCallBack 日志回调 {@link TXMqttLogCallBack}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXGatewayConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,
                            DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXMqttLogCallBack logCallBack,TXMqttActionCallBack callBack) {
        this(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence,true, logCallBack, callBack);
    }

    /**
     * 构造函数，使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXGatewayConnection(Context context, String productID, String deviceName, String secretKey,
                               DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence,
                               TXMqttActionCallBack callBack) {
        this(context, null, productID, deviceName, secretKey, bufferOpts, clientPersistence, false,null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXGatewayConnection(Context context, String productID, String deviceName, String secretKey,
                               DisconnectedBufferOptions bufferOpts, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param srvURL 服务器 URI
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXGatewayConnection(Context context, String srvURL, String productID, String deviceName,
                               String secretKey, TXMqttActionCallBack callBack) {
        this(context, srvURL, productID, deviceName, secretKey, null, null, false, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户） {@link Context}
     * @param productID 网关产品 ID
     * @param deviceName 网关设备名，唯一
     * @param secretKey 网关设备密钥
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXMqttActionCallBack}
     */
    public TXGatewayConnection(Context context, String productID, String deviceName,
                               String secretKey, TXMqttActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, null, null, callBack);
    }



    /**
     * 查找网关子设备
     *
     * @param productId 子产品 ID
     * @param devName 子设备名
     * @return 网关子设备 {@link TXGatewaySubdev}
     */
    private TXGatewaySubdev findSubdev(String productId, String devName) {
        TXLog.d(TAG, "The hashed information is " + mSubdevs);
        return mSubdevs.get(productId + devName);
    }

    /**
     * 移除网关子设备
     *
     * @param subdev 子设备 {@link TXGatewaySubdev}
     * @return 被移除的网关子设备 {@link TXGatewaySubdev}
     */
    private synchronized TXGatewaySubdev removeSubdev(TXGatewaySubdev subdev) {
        return mSubdevs.remove(subdev.mProductId + subdev.mDevName);
    }

    /**
     * 移除网关子设备
     *
     * @param productId 子产品 ID
     * @param devName 子设备名
     * @return 被移除的网关子设备 {@link TXGatewaySubdev}
     */
    private synchronized TXGatewaySubdev removeSubdev(String productId, String devName) {
        return mSubdevs.remove(productId + devName);
    }

    /**
     * 添加新的子设备
     *
     * @param dev 子设备 {@link TXGatewaySubdev}
     */
    private synchronized void addSubdev(TXGatewaySubdev dev) {
        mSubdevs.put(dev.mProductId + dev.mDevName, dev);
    }

    /**
     * 获取子设备状态
     *
     * @param productId 子产品 ID
     * @param devName 子设备名
     * @return 状态 {@link Status}
     */
    public Status getSubdevStatus(String productId, String devName) {
        TXGatewaySubdev subdev = findSubdev(productId, devName);
        if (subdev == null) {
            return Status.SUBDEV_STAT_NOT_EXIST;
        }
        return subdev.getSubdevStatus();
    }

    /**
     * 设置子设备状态
     *
     * @param productId 子产品 ID
     * @param devName 子设备名
     * @param stat 状态 {@link Status}
     * @return 操作结果 {@link Status}
     */
    public Status setSubdevStatus(String productId, String devName, Status stat) {
        TXGatewaySubdev subdev = findSubdev(productId, devName);
        if (subdev == null) {
            return Status.SUBDEV_STAT_NOT_EXIST;
        }
        subdev.setSubdevStatus(stat);
        return Status.OK;
    }

    /**
     * 网关子设备离线
     *
     * @param subProductID 子产品 ID
     * @param subDeviceName 子设备名
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevOffline(String subProductID, String subDeviceName) {
        TXLog.d(TAG, "Try to find " + subProductID + " & " + subDeviceName);
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            TXLog.d(TAG, "Cant find the subdev");
            subdev = new TXGatewaySubdev(subProductID, subDeviceName);
        }
        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

        TXLog.d(TAG, "set " + subProductID + " & " + subDeviceName + " to offline");

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "offline");
            JSONObject plObj = new JSONObject();
            String strDev = "[{'product_id':'" + subProductID +"','device_name':'" + subDeviceName + "'}]";
            JSONArray devs = new JSONArray(strDev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }
        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(obj.toString().getBytes());
        TXLog.d(TAG, "publish message " + message);

        return super.publish(topic, message, null);
    }

    private static String sign(String src, String psk) {
        Mac mac;

        try {
            mac = Mac.getInstance(HMAC_SHA_256);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        String hmacSign;
        SecretKeySpec signKey = new SecretKeySpec(psk.getBytes(), HMAC_SHA_256);

        try {
            mac.init(signKey);
            byte[] rawHmac = mac.doFinal(src.getBytes());
            hmacSign = com.tencent.iot.hub.device.java.core.util.Base64.encodeToString(rawHmac, com.tencent.iot.hub.device.java.core.util.Base64.NO_WRAP);
            return hmacSign;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 网关绑定子设备
     *
     * @param subProductID 子产品 ID
     * @param subDeviceName 子设备名
     * @param psk 子设备密钥
     * @return 操作结果 {@link Status}
     */
    public Status gatewayBindSubdev(String subProductID, String subDeviceName, String psk) {

        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "bind");
            JSONObject plObj = new JSONObject();
            JSONObject dev = new JSONObject();
            dev.put("product_id", subProductID);
            dev.put("device_name", subDeviceName);
            int randNum = (int) (Math.random() * 999999);
            dev.put("random", randNum);
            long timestamp = System.currentTimeMillis() / 1000;
            dev.put("timestamp", timestamp);
            dev.put("signmethod", "hmacsha256");
            dev.put("authtype", "psk");
            String text2Sgin = subProductID + subDeviceName + ";" + randNum + ";" + timestamp;
            dev.put("signature", sign(text2Sgin, psk));
            JSONArray devs = new JSONArray();
            devs.put(dev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(obj.toString().getBytes());
        TXLog.d(TAG, "publish message " + message);

        return super.publish(topic, message, null);
    }

    /**
     * 解绑网关子设备
     *
     * @param subProductID 子产品 ID
     * @param subDeviceName 子设备名
     * @return 操作结果 {@link Status}
     */
    public Status gatewayUnbindSubdev(String subProductID, String subDeviceName) {

        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "unbind");
            JSONObject plObj = new JSONObject();
            JSONObject dev = new JSONObject();
            dev.put("product_id", subProductID);
            dev.put("device_name", subDeviceName);
            JSONArray devs = new JSONArray();
            devs.put(dev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(obj.toString().getBytes());
        TXLog.d(TAG, "publish message " + message);

        return super.publish(topic, message, null);
    }

    /**
     * 查询网关子设备拓扑关系
     *
     * @return 操作结果 {@link Status}
     */
    public Status getGatewaySubdevRealtion() {
        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;

        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "describe_sub_devices");
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(obj.toString().getBytes());
        TXLog.d(TAG, "publish message " + message);

        return super.publish(topic, message, null);
    }

    /**
     * 获取远程配置
     *
     * @return 操作结果 {@link Status}
     */
    public Status getRemoteConfig() {
        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "get");
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        // 这里添加获取到的数据
        message.setPayload(obj.toString().getBytes());
        message.setQos(1);
        String topic = String.format("$config/report/%s/%s", mProductId, mDeviceName);
        return super.publish(topic, message, null);
    }

    /**
     * 关注远程配置变化
     *
     * @return 操作结果 {@link Status}
     */
    public Status concernConfig() {
        String subscribeConfigTopic = PRODUCT_CONFIG_PREFIX + mProductId + "/" + mDeviceName;
        return this.subscribe(subscribeConfigTopic, 1, "subscribe config topic");
    }

    /**
     * 网关子设备上线
     *
     * @param subProductID 子产品 ID
     * @param subDeviceName 子设备名
     * @return 操作结果 {@link Status}
     */
    public Status gatewaySubdevOnline(String subProductID, String subDeviceName) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            TXLog.d(TAG, "Cant find the subdev");
            subdev = new TXGatewaySubdev(subProductID, subDeviceName);
        }
        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
        TXLog.d(TAG, "set " + subProductID + " & " + subDeviceName + " to Online");

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "online");
            JSONObject plObj = new JSONObject();
            String strDev = "[{'product_id':'" + subProductID +"','device_name':'" + subDeviceName + "'}]";
            JSONArray devs = new JSONArray(strDev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }
        addSubdev(subdev);

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(obj.toString().getBytes());
        TXLog.d(TAG, "publish message " + message);

        return super.publish(topic, message, null);
    }

    private boolean consumeGwOperationMsg(String topic, MqttMessage message) {
        if (!topic.startsWith(GW_OPERATION_RES_PREFIX)) {
            return false;
        }
        TXLog.d(TAG, "got gate operation messga " + topic + message);
        String productInfo = topic.substring(GW_OPERATION_RES_PREFIX.length());
        int splitIdx = productInfo.indexOf('/');
        String productId = productInfo.substring(0, splitIdx);
        String devName = productInfo.substring(splitIdx + 1);

        TXGatewaySubdev subdev = findSubdev(productId, devName);

        // this subdev is not managed by me
        if (subdev == null) {
            return false;
        }

        try {
            byte[] payload = message.getPayload();
            JSONObject jsonObject = new JSONObject(new String(payload));

            String type = jsonObject.getString("type");
            if (type.equalsIgnoreCase("online")) {
                String res = jsonObject.getString("result");

                if (res.equals("0")) {
                    subdev.setSubdevStatus(Status.SUBDEV_STAT_ONLINE);
                }

            } else if (type.equalsIgnoreCase("offline")) {
                String res = jsonObject.getString("result");

                if (res.equals("0")) {
                    removeSubdev(subdev);
                }
            }

        }catch (JSONException e) {

        }

        return true;
    }

    /**
     * 收到消息
     *
     * @param topic 消息主题
     * @param message 消息内容结构体 {@link MqttMessage}
     * @throws Exception 抛出异常 {@link Exception}
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        TXLog.d(TAG, "message received " + topic);
        if (!consumeGwOperationMsg(topic, message)) {
            super.messageArrived(topic, message);
        }
    }

    /**
     * 连接 MQTT 服务，无需设置 username 和 password，内部会自动填充
     *
     * @param options 连接参数 {@link MqttConnectOptions}
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 操作结果 {@link Status}
     */
    @Override
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
            timestamp = System.currentTimeMillis() / 1000 + 600;
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
                // If the connection is established, subscribe the gateway operation topic
                String gwTopic = GW_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
                int qos = TXMqttConstants.QOS1;

                subscribe(gwTopic, qos, "Subscribe GATEWAY result topic");
                TXLog.d(TAG, "Connected, then subscribe the gateway result topic");

                if (mMqttLogFlag) {
                    initMqttLog(TAG);
                }
                if (sshHost != null && !sshHost.equals("")) {// 用户上下文（请求实例）
                    subscribeNTPTopic(TXMqttConstants.QOS1, null);
                }
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
                mMqttClient.setBufferOpts(super.bufferOpts);
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
}
