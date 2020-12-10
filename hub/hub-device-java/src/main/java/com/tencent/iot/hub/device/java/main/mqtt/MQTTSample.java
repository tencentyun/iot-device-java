package com.tencent.iot.hub.device.java.main.mqtt;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.gateway.TXGatewayConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;

public class MQTTSample {

    private static final String TAG = "TXMQTT";
    private static final Logger LOG = LoggerFactory.getLogger(MQTTSample.class);
    
    // Default Value, should be changed in testing
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";
    private String mSubProductID = "SUBDEV_PRODUCT-ID";
    private String mSubDevName = "SUBDEV_DEV-NAME";
    private String mTestTopic = "XLQ2S443OT/firefly";

    private TXMqttActionCallBack mMqttActionCallBack;

    /**
     * MQTT连接实例
     */
    private TXMqttConnection mMqttConnection;

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    public MQTTSample(TXMqttActionCallBack callBack) {
        mMqttActionCallBack = callBack;
        mMqttConnection = new TXMqttConnection(mProductID, mDevName, callBack);
    }

    public MQTTSample( TXMqttActionCallBack callBack, String brokerURL, String productId,
                      String devName, String devPSK, String subProductID, String subDevName, String testTopic) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mSubProductID = subProductID;
        mSubDevName = subDevName;
        mTestTopic = testTopic;
        
        mMqttActionCallBack = callBack;
        mMqttConnection = new TXMqttConnection(mProductID, mDevName, mDevPSK, mMqttActionCallBack);
    }

    /**
     * 获取主题
     *
     * @param topicName
     * @return
     */
    private String getTopic(String topicName) {
        return mTestTopic;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXMqttConnection(mProductID, mDevName, mDevPSK, mMqttActionCallBack);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

 //       options.setSocketFactory(AsymcSslUtils.getSocketFactory());
       

        MQTTRequest mqttRequest = new MQTTRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);
    }

    /**
     * 断开MQTT连接
     */
    public void disconnect() {
        MQTTRequest mqttRequest = new MQTTRequest("disconnect", requestID.getAndIncrement());
        mMqttConnection.disConnect(mqttRequest);
    }

    public void setSubdevOnline() {
        // set subdev online
       
    }

    public void setSubDevOffline() {
       
    }

    /**
     * 订阅主题
     *
     * @param topicName 主题名
     */
    public void subscribeTopic(String topicName) {
        // 主题
        String topic = getTopic(topicName);
        // QOS等级
        int qos = TXMqttConstants.QOS1;
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());

        LOG.debug("{}", "sub topic is " + topic);

        // 订阅主题
        mMqttConnection.subscribe(topic, qos, mqttRequest);

    }

    /**
     * 取消订阅主题
     *
     * @param topicName 主题名
     */
    public void unSubscribeTopic(String topicName) {
        // 主题
        String topic = getTopic(topicName);
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
        LOG.debug("{}", "Start to unSubscribe" + topic);
        // 取消订阅主题
        mMqttConnection.unSubscribe(topic, mqttRequest);
    }

    /**
     * 发布主题
     */
    public void publishTopic(String topicName,  JSONObject jsonObject) {

        // 主题
        String topic = getTopic(topicName);
        System.out.print("aaaaaaaaaaaaa " +topic);
        // MQTT消息
        MqttMessage message = new MqttMessage();

        message.setQos(TXMqttConstants.QOS1);
        message.setPayload(jsonObject.toString().getBytes());

        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("publishTopic", requestID.getAndIncrement());

        LOG.debug("{}", "pub topic " + topic + message);
        // 发布主题
        mMqttConnection.publish(topic, message, mqttRequest);

    }
}
