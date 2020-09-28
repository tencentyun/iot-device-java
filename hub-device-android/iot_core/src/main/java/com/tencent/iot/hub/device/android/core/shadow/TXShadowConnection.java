package com.tencent.iot.hub.device.android.core.shadow;

import android.content.Context;
import android.os.SystemClock;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;



public class TXShadowConnection extends com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection{

    public static final String TAG = TXShadowConnection.class.getName();

    /**
     * clientToken formatter
     */
    private static final String CLIENT_TOKEN = "%s-%d";

    private Context mContext = null;

    /**
     * shadow action回调接口
     */
    private TXShadowActionCallBack mShadowActionCallback = null;

    /**
     * mqtt 连接实例
     */
    private TXMqttConnection mMqttConnection = null;


    private int mQos = TXMqttConstants.QOS0;

    private AtomicInteger mClientTokenNum = new AtomicInteger(0);


    /**
     * 保存用户注册的属性
     */
    private HashMap<String, DeviceProperty> mRegisterPropertyMap = new HashMap<>();

    /**
     * 是否已经成功订阅OPERATION_RESULT_TOPIC
     */
    private boolean mIsOperationResultSubscribeSuccess = false;

    /**
     * 文档版本号
     */
    private int mDocumentVersion = 0;

    private String OPERATION_TOPIC = null;
    private String OPERATION_RESULT_TOPIC = null;

    private static final int MAX_MESSAGE_ID = 65535;
    private int mPublishMessageId = 0;

    /**
     * @param context    用户上下文（这个参数在回调函数时透传给用户）
     * @param productID  产品名
     * @param deviceName 设备名，唯一
	 * @param secretKey  密钥
     * @param callBack   连接、消息发布、消息订阅回调接口
     */
    public TXShadowConnection(Context context, String productID, String deviceName, String secretKey, TXShadowActionCallBack callBack) {
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
    public TXShadowConnection(Context context, String productID, String deviceName, String secretKey,
                              DisconnectedBufferOptions bufferOpts, TXShadowActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productID         产品名
     * @param deviceName        设备名，唯一
	 * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXShadowConnection(Context context, String productID, String deviceName, String secretKey,
                              DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXShadowActionCallBack callBack) {
        this(context, TXMqttConstants.DEFAULT_SERVER_URI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * Shadow连接器构造器
     *
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI         服务器URI
     * @param productID         产品名
     * @param deviceName        设备名，唯一
	 * @param secretKey         密钥
     * @param bufferOpts        发布消息缓存buffer，当发布消息时MQTT连接非连接状态时使用
     * @param clientPersistence 消息永久存储
     * @param callBack          连接、消息发布、消息订阅回调接口
     */
    public TXShadowConnection(Context context, String serverURI, String productID, String deviceName, String secretKey,
                              DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXShadowActionCallBack callBack) {
        super(serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
        this.mContext = context;
        this.mShadowActionCallback = callBack;

        mMqttConnection = new TXMqttConnection(context, serverURI, productID, deviceName, secretKey,
                bufferOpts, clientPersistence, new ShadowUponMqttCallBack());

        OPERATION_TOPIC = "$shadow/operation/" + productID + "/" + mMqttConnection.mDeviceName;
        OPERATION_RESULT_TOPIC = "$shadow/operation/result/" + productID + "/" + mMqttConnection.mDeviceName;

        mPublishMessageId = new Random().nextInt(MAX_MESSAGE_ID);
    }

    /**
     * 获取TXMqttConnection句柄
     */
    public TXMqttConnection getMqttConnection() {
        return mMqttConnection;
    }

    /**
     * 与云端建立连接，结果通过回调函数通知。
     *
     * @param options     连接参数
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        Status status = mMqttConnection.connect(options, userContext);
        if (status != Status.OK) {
            return status;
        }

        long startTimeMills = SystemClock.uptimeMillis();
        while (!mIsOperationResultSubscribeSuccess) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }

            if (mMqttConnection.getConnectStatus() == TXMqttConstants.ConnectStatus.kConnectFailed) {
                return Status.ERROR;
            }

            if (SystemClock.uptimeMillis() - startTimeMills > 20000) {  //20 seconds
                TXLog.e(TAG, "Subscribe topic [%s] timeout!!!", OPERATION_RESULT_TOPIC);
                return Status.ERROR;
            }
        }

        return status;
    }

    /**
     * 订阅普通主题
     *
     * @param topicName 主题名
     * @param qos QOS等级
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status subscribe(String topicName, int qos, Object userContext) {
        Status status;
        status = checkMqttStatus();
        if (status != Status.OK) {
            return status;
        }
        TXLog.d(TAG, "sub topic is " + topicName);
        // 订阅主题
        return mMqttConnection.subscribe(topicName, qos, userContext);
    }

    /**
     * 取消订阅普通主题
     *
     * @param topicName 主题名
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status unSubscribe(String topicName, Object userContext) {
        Status status;
        status = checkMqttStatus();
        if (status != Status.OK) {
            return status;
        }
        TXLog.d(TAG, "Start to unSubscribe" + topicName);
        // 取消订阅主题
        return mMqttConnection.unSubscribe(topicName, userContext);
    }

    /**
     * 实现普通topic的发布
     *
     * @param topicName    topic
     * @param message     发布的消息
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status publish(String topicName, MqttMessage message, Object userContext) {
        //判断MQTT状态
        Status status;
        status = checkMqttStatus();
        if (status != Status.OK) {
            return status;
        }
        TXLog.d(TAG, "pub topic " + topicName + message);
        // 发布主题
        return mMqttConnection.publish(topicName, message, userContext);
    }

    /**
     * 检查mqtt状态
     *
     * @return 当前状态
     */
    private Status checkMqttStatus() {
        if (null == mMqttConnection || mMqttConnection.getConnectStatus() != TXMqttConstants.ConnectStatus.kConnected) {
            TXLog.e(TAG, "mqtt is disconnected!");
            return Status.MQTT_NO_CONN;
        }

        return Status.OK;
    }

    /**
     * 处理接收到的影子消息
     *
     * @param topic   来自哪个TOPIC的消息
     * @param message MQTT消息
     */
    private void processShadowMessageReceived(String topic, MqttMessage message) {
        if (null == message || null == message.getPayload()) {
            TXLog.e(TAG, "handle mqtt message failed, reason[%s]!", "message or payload is empty");
            return;
        }

        String jsonStr = new String(message.getPayload());
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(jsonStr);

            String type = jsonObj.getString(TXShadowConstants.TYPE);
            if (TXShadowConstants.DELTA.equals(type)) {
                processDeltaMessage(message);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 处理delta消息函数。
     * 当服务端发送delta消息到终端, 将会调用此方法
     *
     * @param message mqtt消息
     */
    private void processDeltaMessage(MqttMessage message) {
        JSONObject jsonObj = null;

        try {
            jsonObj = new JSONObject(new String(message.getPayload()));
            //edited by v_vweisun 2020/09/22 start
            jsonObj = jsonObj.getJSONObject(TXShadowConstants.PAYLOAD);//new JSONObject(jsonObj.getString(TXShadowConstants.PAYLOAD));
            //edited by v_vweisun 2020/09/22 end

            if (jsonObj.has(TXShadowConstants.VERSION)) {
                int versionNum = jsonObj.getInt(TXShadowConstants.VERSION);
                if (versionNum > mDocumentVersion) {
                    mDocumentVersion = versionNum;
                    TXLog.d(TAG, "New Version number : %d", mDocumentVersion);
                } else {
                    TXLog.w(TAG, "Old Delta Message received - Ignoring rx : %d local : %d", versionNum, mDocumentVersion);
                    return;
                }
            }
        } catch (JSONException e) {
            TXLog.e(TAG, e, "Received JSON is not valid!");
            return;
        }

        if (!jsonObj.has(TXShadowConstants.STATE)) {
            return;
        }

        List<DeviceProperty> propertyList = new ArrayList<>();
        String stateJsonStr = "";

        try {
            //edited by v_vweisun 2020/09/22 start
//			stateJsonStr = jsonObj.getString(TXShadowConstants.STATE);

            JSONObject stateObj = jsonObj.getJSONObject(TXShadowConstants.STATE);//new JSONObject(stateJsonStr);
            //edited by v_vweisun 2020/09/22 end

            Iterator it = mRegisterPropertyMap.keySet().iterator();
            while (it.hasNext()) {
                DeviceProperty property = mRegisterPropertyMap.get(it.next());

                if (stateObj.has(property.mKey)) {
                    // edited by v_vweisun 2020/09/22 start
                    if (TXShadowConstants.JSONDataType.INT == property.mDataType) {
                        property.data(stateObj.getInt(property.mKey));
                    } else if (TXShadowConstants.JSONDataType.LONG == property.mDataType) {
                        property.data(stateObj.getLong(property.mKey));
                    } else if (TXShadowConstants.JSONDataType.FLOAT == property.mDataType) {
                        property.data(stateObj.getDouble(property.mKey));
                    } else if (TXShadowConstants.JSONDataType.DOUBLE == property.mDataType) {
                        property.data(stateObj.getDouble(property.mKey));
                    } else if (TXShadowConstants.JSONDataType.BOOLEAN == property.mDataType) {
                        property.data(stateObj.getBoolean(property.mKey));
                    } else {
                        property.data(stateObj.getString(property.mKey));
                    }
                    // edited by v_vweisun 2020/09/22 end
                    propertyList.add(property);
                    TXLog.d(TAG, "******%s, %s", property.mKey, stateObj.getString(property.mKey));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null != mShadowActionCallback && !propertyList.isEmpty()) {
            mShadowActionCallback.onDevicePropertyCallback(stateJsonStr, propertyList);
        }
    }

    private class ShadowUponMqttCallBack extends TXMqttActionCallBack {
        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            TXLog.d(TAG, "onConnectCompleted, status[%s], reconnect[%b], msg[%s]", status, reconnect, msg);
            if (Status.OK == status) {
                TXLog.d(TAG, "******subscribe topic:" + OPERATION_RESULT_TOPIC);
                mMqttConnection.subscribe(OPERATION_RESULT_TOPIC, mQos, "subscribe context");
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            TXLog.e(TAG, cause, "mqtt connection lost!");
            mIsOperationResultSubscribeSuccess = false;
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            TXLog.d(TAG, "onDisconnectCompleted, status[%s], msg[%s]", status.name(), msg);

            mIsOperationResultSubscribeSuccess = false;
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            super.onPublishCompleted(status, token, userContext, errMsg);

            String[] topics = token.getTopics();
            TXLog.d(TAG, "onPublishCompleted, status[%s], errMsg[%s], topics[%s]", status.name(), errMsg, Arrays.toString(topics));
            for (String topic : topics) {
                if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
                } else {
                    mShadowActionCallback.onPublishCompleted(status, token, userContext, errMsg);
                }
            }
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            super.onSubscribeCompleted(status, token, userContext, errMsg);

            String[] topics = token.getTopics();
            TXLog.d(TAG, "onSubscribeCompleted, status[%s], errMsg[%s], topics[%s]", status.name(), errMsg, Arrays.toString(topics));
            for (String topic : topics) {
                if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
                    if (status == Status.OK) {
                        TXLog.d(TAG, "***subscribe topic:" + OPERATION_RESULT_TOPIC + " success!!!!");
                        mIsOperationResultSubscribeSuccess = true;
                    }
                } else {
                    mShadowActionCallback.onSubscribeCompleted(status, token, userContext, errMsg);
                }
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            super.onUnSubscribeCompleted(status, token, userContext, errMsg);

            String[] topics = token.getTopics();
            TXLog.d(TAG, "onUnSubscribeCompleted, status[%s], errMsg[%s], topics[%s]", status.name(), errMsg, Arrays.toString(topics));
            for (String topic : topics) {
                if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
                    if (status == Status.OK) {
                        mIsOperationResultSubscribeSuccess = false;
                    }
                } else {
                    mShadowActionCallback.onUnSubscribeCompleted(status, token, userContext, errMsg);
                }
            }
        }

        @Override
        public void onMessageReceived(String topic, MqttMessage message) {
            super.onMessageReceived(topic, message);

            TXLog.d(TAG, "onMessageReceived,  topics[%s]", topic);

            if (topic.startsWith("$" + TXShadowConstants.SHADOW)) {
                if (null == message || null == message.getPayload()) {
                    TXLog.e(TAG, "handle mqtt message failed, reason[%s]!", "message or payload is empty");
                    return;
                }

                String jsonStr = new String(message.getPayload());

                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    if (jsonObj.has(TXShadowConstants.TYPE)) {
                        String type = jsonObj.getString(TXShadowConstants.TYPE);

                        if (TXShadowConstants.GET.equals(type) || TXShadowConstants.UPDATE.equals(type) || TXShadowConstants.DELETE.equals(type)) {

                            if (jsonObj.has(TXShadowConstants.RESULT)) {
                                int result = jsonObj.getInt(TXShadowConstants.RESULT);
                                if (jsonObj.has(TXShadowConstants.PAYLOAD)) {
                                    String payloadStr = jsonObj.getJSONObject(TXShadowConstants.PAYLOAD).toString();

                                    mShadowActionCallback.onRequestCallback(type, result, payloadStr);

                                    JSONObject payloadJsonObj = new JSONObject(payloadStr);
                                    if (payloadJsonObj.has(TXShadowConstants.VERSION)) {
                                        mDocumentVersion = payloadJsonObj.getInt(TXShadowConstants.VERSION);
                                        TXLog.d(TAG, "******update local mDocumentVersion to " + mDocumentVersion);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                processShadowMessageReceived(topic, message);
            } else {
                mShadowActionCallback.onMessageReceived(topic, message);
            }
        }
    }
}
