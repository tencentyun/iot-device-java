package com.qcloud.iot.samples.mqtt;

import android.content.Context;
import android.os.Environment;
import android.util.Log;


import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttConnection;
import com.qcloud.iot_explorer.mqtt.TXMqttConstants;
import com.qcloud.iot_explorer.mqtt.TXOTACallBack;
import com.qcloud.iot_explorer.mqtt.TXOTAConstansts;
import com.qcloud.iot_explorer.utils.AsymcSslUtils;
import com.qcloud.iot_explorer.utils.TXLog;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;

public class MQTTSample {

    private static final String TAG = "TXMQTT";
    // Default Value, should be changed in testing
    private String mBrokerURL = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";

    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";

    private String mSubProductID = "SUBDEV_PRODUCT-ID";
    private String mSubDevName = "SUBDEV_DEV-NAME";
    private String mTestTopic = "TEST_TOPIC_WITH_SUB_PUB";
    private String mDevCert;
    private String mDevPriv;

    private boolean mMqttLogFlag;
    private Context mContext;

    private TXMqttActionCallBack mMqttActionCallBack;

    /**
     * MQTT连接实例
     */
    private TXMqttConnection  mMqttConnection;

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    public MQTTSample(Context context) {
        mContext = context;
    }

    public MQTTSample(Context context, String brokerURL, String productId,
                      String devName, String devPSK, String testTopic, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mTestTopic = testTopic;
        mDevCertName = devCertName;
        mDevKeyName = devKeyName;
        mContext = context;
        mMqttActionCallBack = mqttActionCallBack;
   }

     public MQTTSample(Context context, String brokerURL, String productId,
                      String devName, String devPSK, String testTopic, TXMqttActionCallBack mqttActionCallBack) {
        mContext = context;
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mTestTopic = testTopic;
        mMqttActionCallBack = mqttActionCallBack;
    }

     /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXMqttConnection(mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mMqttActionCallBack);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0) {
            TXLog.i(TAG, "Using cert stream " + mDevPriv + "  " + mDevCert);
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        } else if (mDevPSK != null && mDevPSK.length() != 0){
            TXLog.i(TAG, "Using PSK");
            options.setSocketFactory(AsymcSslUtils.getSocketFactory());
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, mDevCertName, mDevKeyName));
        }

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

    /**
     * 订阅主题
     *
     */
    public void subscribeTopic() {
        // 主题
        String topic = mTestTopic;
        // QOS等级
        int qos = TXMqttConstants.QOS1;
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());

        Log.d(TAG, "sub topic is " + topic);

        // 订阅主题
        mMqttConnection.subscribe(topic, qos, mqttRequest);

    }

    /**
     * 取消订阅主题
     *
     */
    public void unSubscribeTopic() {
        // 主题
        String topic = mTestTopic;
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("unSubscribeTopic", requestID.getAndIncrement());
        Log.d(TAG, "Start to unSubscribe" + topic);
        // 取消订阅主题
        mMqttConnection.unSubscribe(topic, mqttRequest);
    }

    /**
     * 发布主题
     */
    public void publishTopic(String topicName, Map<String, String> data) {
        // 主题
        String topic = mTestTopic;
        // MQTT消息
        MqttMessage message = new MqttMessage();

        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
        } catch (JSONException e) {
            TXLog.e(TAG, e, "pack json data failed!");
        }
        message.setQos(TXMqttConstants.QOS1);
        message.setPayload(jsonObject.toString().getBytes());

        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("publishTopic", requestID.getAndIncrement());

        Log.d(TAG, "pub topic " + topic + message);
        // 发布主题
        mMqttConnection.publish(topic, message, mqttRequest);

    }

    public void checkFirmware() {

        mMqttConnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                TXLog.e(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }

            @Override
            public void onDownloadProgress(int percent, String version) {
                TXLog.e(TAG, "onDownloadProgress:" + percent);
            }

            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                TXLog.e(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

                mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                TXLog.e(TAG, "onDownloadFailure:" + errCode);

                mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mMqttConnection.reportCurrentFirmwareVersion("0.0.1");
    }
}
