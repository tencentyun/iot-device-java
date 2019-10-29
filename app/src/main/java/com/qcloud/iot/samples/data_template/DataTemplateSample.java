package com.qcloud.iot.samples.data_template;

import android.content.Context;
import android.os.Environment;

import com.qcloud.iot_explorer.common.Status;
import com.qcloud.iot_explorer.data_template.TXDataTemplateClient;
import com.qcloud.iot_explorer.data_template.TXDataTemplateDownCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttActionCallBack;
import com.qcloud.iot_explorer.mqtt.TXMqttRequest;
import com.qcloud.iot_explorer.mqtt.TXOTACallBack;
import com.qcloud.iot_explorer.mqtt.TXOTAConstansts;
import com.qcloud.iot_explorer.utils.AsymcSslUtils;
import com.qcloud.iot_explorer.utils.TXLog;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.concurrent.atomic.AtomicInteger;

import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_TOPIC;
import static com.qcloud.iot_explorer.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_TOPIC;

public class DataTemplateSample {
    private static final String TAG = "TXMQTT";
    // Default Value, should be changed in testing
    private String mBrokerURL = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";
    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";
    private Context mContext;

    private TXMqttActionCallBack mMqttActionCallBack;

    /**
     * MQTT连接实例
     */
    private TXDataTemplateClient mMqttConnection;

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    public DataTemplateSample(Context context) {
        mContext = context;
    }

    public DataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mDevCertName = devCertName;
        mDevKeyName = devKeyName;
        mContext = context;
        mMqttActionCallBack = mqttActionCallBack;
    }

    public DataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK,TXMqttActionCallBack mqttActionCallBack) {
        mContext = context;
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mMqttActionCallBack = mqttActionCallBack;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXDataTemplateClient(mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mMqttActionCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            TXLog.i(TAG, "Using PSK");
            options.setSocketFactory(AsymcSslUtils.getSocketFactory());
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
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
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mMqttConnection.disConnect(mqttRequest);
    }

    /**
     * 订阅主题
     *
     */
    public void subscribeTopic() {
        mMqttConnection.subscribeTemplateTopic(PROPERTY_DOWN_TOPIC, 0);
        mMqttConnection.subscribeTemplateTopic(EVENT_DOWN_TOPIC, 0);
        mMqttConnection.subscribeTemplateTopic(ACTION_DOWN_TOPIC, 1);
    }

    /**
     * 取消订阅主题
     *
     */
    public void unSubscribeTopic() {
        mMqttConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_TOPIC);
        mMqttConnection.unSubscribeTemplateTopic(EVENT_DOWN_TOPIC);
        mMqttConnection.unSubscribeTemplateTopic(ACTION_DOWN_TOPIC);
    }

    public Status propertyReport(String params, String metadata, TXDataTemplateDownCallBack reportReplyCallBack) {
        return mMqttConnection.propertyReport(params, metadata, reportReplyCallBack);
    }

    public Status eventsPost(String events, TXDataTemplateDownCallBack eventReplyCallBack) {
       return mMqttConnection.eventsPost(events, eventReplyCallBack);
    }

    public Status eventSinglePost(String eventId, String type, String params, TXDataTemplateDownCallBack eventReplyCallBack){
        return  mMqttConnection.eventSinglePost(eventId, type, params, eventReplyCallBack);
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
