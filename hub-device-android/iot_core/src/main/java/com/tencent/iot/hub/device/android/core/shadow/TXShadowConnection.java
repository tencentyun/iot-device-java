package com.tencent.iot.hub.device.android.core.shadow;

import android.content.Context;

import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
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

import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.MQTT_SERVER_PORT_TLS;
import static com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants.QCLOUD_IOT_MQTT_DIRECT_DOMAIN;


public class TXShadowConnection {

    public static final String TAG = TXShadowConnection.class.getName();

    /**
     * clientToken formatter
     */
    private static final String CLIENT_TOKEN = "%s-%d";

    private Context mContext = null;

    private com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection mTXShadowConn = null;

    /**
     * shadow action回调接口
     */
    private TXShadowActionCallBack mShadowActionCallback = null;

    /**
     * mqtt 连接实例
     */
    private TXMqttConnection mMqttConnection = null;


    private int mQos = TXMqttConstants.QOS0;


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
        this(context, null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
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
        this.mContext = context;
        this.mShadowActionCallback = callBack;

        mTXShadowConn = new com.tencent.iot.hub.device.java.core.shadow.TXShadowConnection(serverURI, productID, deviceName, secretKey,
                bufferOpts, clientPersistence, callBack);
        mMqttConnection = new TXMqttConnection(context, serverURI, productID, deviceName, secretKey,
                bufferOpts, clientPersistence, mTXShadowConn.mShadowUponMqttCallBack);

        OPERATION_TOPIC = "$shadow/operation/" + productID + "/" + mMqttConnection.mDeviceName;
        OPERATION_RESULT_TOPIC = "$shadow/operation/result/" + productID + "/" + mMqttConnection.mDeviceName;

        mPublishMessageId = new Random().nextInt(MAX_MESSAGE_ID);

        mTXShadowConn.setMqttConnection(mMqttConnection);
    }

    /**
     * 获取TXMqttConnection句柄
     */
    public TXMqttConnection getMqttConnection() {
        return (TXMqttConnection) mTXShadowConn.getMqttConnection();
    }


    /**
     * 设置断连状态buffer缓冲区
     *
     * @param bufferOpts 缓冲参数
     */
    public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
        mTXShadowConn.setBufferOpts(bufferOpts);
    }

    /**
     * 与云端建立连接，结果通过回调函数通知。
     *
     * @param options     连接参数
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        return mTXShadowConn.connect(options, userContext);
    }

    /**
     * 断开连接请求，结果通过回调函数通知。
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status disConnect(Object userContext) {
        return mTXShadowConn.disConnect(userContext);
    }

    /**
     * 获取连接状态
     *
     * @return 连接状态
     */
    public TXMqttConstants.ConnectStatus getConnectStatus() {
        return mTXShadowConn.getConnectStatus();
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
     * 更新设备属性信息，结果通过回调函数通知。
     *
     * @param devicePropertyList 需要更新的设备属性集
     * @param userContext        用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status update(List<DeviceProperty> devicePropertyList, Object userContext) {
        return mTXShadowConn.update(devicePropertyList, userContext);
    }

    /**
     * 更新delta信息后，上报空的desired信息，通知服务器不再发送delta消息。
     *
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportNullDesiredInfo() {
        return mTXShadowConn.reportNullDesiredInfo();
    }

    /**
     * 更新delta信息后，上报空的desired信息，通知服务器不再发送delta消息。
     *
     * @param reportJsonDoc 用户上报的JSON内容
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status reportNullDesiredInfo(String reportJsonDoc) {
        return mTXShadowConn.reportNullDesiredInfo(reportJsonDoc);
    }

    /**
     * 获取设备影子文档，结果通过回调函数通知。
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回Status.OK; 其它返回值表示发送请求失败；
     */
    public Status get(Object userContext) {
        return mTXShadowConn.get(userContext);
    }

    /**
     * 注册当前设备的设备属性
     *
     * @param property 设备属性
     */
    public void registerProperty(DeviceProperty property) {
        mTXShadowConn.registerProperty(property);
    }

    /**
     * 取消注册当前设备的指定属性
     *
     * @param property
     */
    public void unRegisterProperty(DeviceProperty property) {
        mTXShadowConn.unRegisterProperty(property);
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
}
