package com.tencent.iot.explorer.device.tme.data_template;

import android.content.Context;
import android.os.Environment;

import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.SERVICE_DOWN_STREAM_TOPIC;

public class TmeDataTemplateSample {

    private static final String TAG = TmeDataTemplateSample.class.getSimpleName();
    private static final AtomicInteger REQUEST_ID = new AtomicInteger(0);

    //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mBrokerURL = null;
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";
    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName = "DEVICE_KEY-NAME ";
    private String mJsonFileName = "JSON_FILE_NAME";

    private TXMqttActionCallBack mMqttActionCallBack;
    private TmeTemplateClient mMqttConnection;
    private TXDataTemplateDownStreamCallBack mDownStreamCallBack;

    private Context mContext;


    public TmeDataTemplateSample(Context context) {
        mContext = context;
    }

    public TmeDataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack,
                                 final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mDevCertName = devCertName;
        mDevKeyName = devKeyName;
        mContext = context;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mDownStreamCallBack = downStreamCallBack;
    }

    public TmeDataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK, TXMqttActionCallBack mqttActionCallBack,
                                 final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        mContext = context;
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mDownStreamCallBack = downStreamCallBack;
    }

    /**
     * 生成绑定设备的二维码字符串
     *
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generateDeviceQRCodeContent() {
        return mMqttConnection.generateDeviceQRCodeContent();
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TmeTemplateClient(mContext, mBrokerURL, mProductID, mDevName, mDevPSK, null, null, mMqttActionCallBack,
                mJsonFileName, mDownStreamCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0) {
            TXLog.i(TAG, "Using PSK");
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", REQUEST_ID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return mMqttConnection.isConnected();
    }

    /**
     * 断开MQTT连接
     */
    public void disconnect() {
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", REQUEST_ID.getAndIncrement());
        mMqttConnection.disConnect(mqttRequest);
    }

    /**
     * 订阅主题
     */
    public void subscribeTopic() {
        if (Status.OK != mMqttConnection.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
        }
        if (Status.OK != mMqttConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
        if (Status.OK != mMqttConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe action down stream topic failed!");
        }
        if (Status.OK != mMqttConnection.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe service down stream topic failed!");
        }
    }

    /**
     * 取消订阅主题
     */
    public void unSubscribeTopic() {
        if (Status.OK != mMqttConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if (Status.OK != mMqttConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if (Status.OK != mMqttConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if (Status.OK != mMqttConnection.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
        }
    }

    public Status propertyReport(JSONObject property, JSONObject metadata) {
        return mMqttConnection.propertyReport(property, metadata);
    }

    public Status propertyGetStatus(String type, boolean showmeta) {
        return mMqttConnection.propertyGetStatus(type, showmeta);
    }

    public Status propertyReportInfo(JSONObject params) {
        return mMqttConnection.propertyReportInfo(params);
    }

    public Status propertyClearControl() {
        return mMqttConnection.propertyClearControl();
    }

    public Status eventsPost(JSONArray events) {
        return mMqttConnection.eventsPost(events);
    }

    public Status eventSinglePost(String eventId, String type, JSONObject params) {
        return mMqttConnection.eventSinglePost(eventId, type, params);
    }

    public void checkFirmware() {
        mMqttConnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                TXLog.e(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                return false;
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
