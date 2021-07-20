package com.tencent.iot.hub.device.android.core.shadow;

import android.content.Context;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.shadow.TXShadowActionCallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * 影子连接类
 */
public class TXShadowConnection {

    /**
     * 类标记
     */
    private static final String TAG = TXShadowConnection.class.getName();

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
     * 是否已经成功订阅 OPERATION_RESULT_TOPIC
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
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXShadowActionCallBack}
     */
    public TXShadowConnection(Context context, String productID, String deviceName, String secretKey, TXShadowActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXShadowActionCallBack}
     */
    public TXShadowConnection(Context context, String productID, String deviceName, String secretKey,
                              DisconnectedBufferOptions bufferOpts, TXShadowActionCallBack callBack) {
        this(context, productID, deviceName, secretKey, bufferOpts, null, callBack);
    }

    /**
     * 构造函数
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXShadowActionCallBack}
     */
    public TXShadowConnection(Context context, String productID, String deviceName, String secretKey,
                              DisconnectedBufferOptions bufferOpts, MqttClientPersistence clientPersistence, TXShadowActionCallBack callBack) {
        this(context, null, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack);
    }

    /**
     * Shadow 连接器构造器
     *
     * @param context 用户上下文（这个参数在回调函数时透传给用户）
     * @param serverURI 服务器 URI
     * @param productID 产品 ID
     * @param deviceName 设备名，唯一
     * @param secretKey 设备密钥
     * @param bufferOpts 发布消息缓存 buffer，当发布消息时 MQTT 连接非连接状态时使用 {@link DisconnectedBufferOptions}
     * @param clientPersistence 消息永久存储 {@link MqttClientPersistence}
     * @param callBack 连接、消息发布、消息订阅回调接口 {@link TXShadowActionCallBack}
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
     * 获取 TXMqttConnection 句柄
     *
     * @return {@link TXMqttConnection}
     */
    public TXMqttConnection getMqttConnection() {
        return (TXMqttConnection) mTXShadowConn.getMqttConnection();
    }


    /**
     * 设置断连状态 buffer 缓冲区
     *
     * @param bufferOpts 缓冲参数 {@link DisconnectedBufferOptions}
     */
    public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
        mTXShadowConn.setBufferOpts(bufferOpts);
    }

    /**
     * 与云端建立连接，结果通过回调函数通知
     *
     * @param options 连接参数 {@link MqttConnectOptions}
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        return mTXShadowConn.connect(options, userContext);
    }

    /**
     * 断开连接请求，结果通过回调函数通知
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status disConnect(Object userContext) {
        return mTXShadowConn.disConnect(userContext);
    }

    /**
     * 获取连接状态
     *
     * @return 连接状态 {@link TXMqttConstants.ConnectStatus}
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
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
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
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
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
     * 实现普通 topic 的发布
     *
     * @param topicName 主题
     * @param message 发布的消息
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
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
     * 更新设备属性信息，结果通过回调函数通知
     *
     * @param devicePropertyList 需要更新的设备属性集
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status update(List<DeviceProperty> devicePropertyList, Object userContext) {
        return mTXShadowConn.update(devicePropertyList, userContext);
    }

    /**
     * 上报空的 reported 信息，清空服务器中 reported 信息
     *
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status reportNullReportedInfo() {
        return mTXShadowConn.reportNullReportedInfo();
    }

    /**
     * 更新 delta 信息后，上报空的 desired 信息，通知服务器不再发送 delta 消息
     *
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status reportNullDesiredInfo() {
        return mTXShadowConn.reportNullDesiredInfo();
    }

    /**
     * 更新 delta 信息后，上报空的 desired 信息，通知服务器不再发送 delta 消息
     *
     * @param reportJsonDoc 用户上报的 JSON 内容
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status reportNullDesiredInfo(String reportJsonDoc) {
        return mTXShadowConn.reportNullDesiredInfo(reportJsonDoc);
    }

    /**
     * 获取设备影子文档，结果通过回调函数通知
     *
     * @param userContext 用户上下文（这个参数在回调函数时透传给用户）
     * @return 发送请求成功时返回 Status.OK；其它返回值表示发送请求失败
     */
    public Status get(Object userContext) {
        return mTXShadowConn.get(userContext);
    }

    /**
     * 注册当前设备的设备属性
     *
     * @param property 设备属性 {@link DeviceProperty}
     */
    public void registerProperty(DeviceProperty property) {
        mTXShadowConn.registerProperty(property);
    }

    /**
     * 取消注册当前设备的指定属性
     *
     * @param property 设备属性 {@link DeviceProperty}
     */
    public void unRegisterProperty(DeviceProperty property) {
        mTXShadowConn.unRegisterProperty(property);
    }

    /**
     * 检查 mqtt 状态
     *
     * @return 当前状态 {@link Status}
     */
    private Status checkMqttStatus() {
        if (null == mMqttConnection || mMqttConnection.getConnectStatus() != TXMqttConstants.ConnectStatus.kConnected) {
            TXLog.e(TAG, "mqtt is disconnected!");
            return Status.MQTT_NO_CONN;
        }

        return Status.OK;
    }
}
