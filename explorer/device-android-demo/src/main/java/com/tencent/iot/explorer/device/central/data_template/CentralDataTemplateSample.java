package com.tencent.iot.explorer.device.central.data_template;


import android.content.Context;
import android.os.Environment;

import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.central.callback.OnGetDeviceListListener;
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




public class CentralDataTemplateSample {

    private static final String TAG = CentralDataTemplateSample.class.getSimpleName();
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
    private CentralTemplateClient mClient;
    private TXDataTemplateDownStreamCallBack mDownStreamCallBack;
    private OnGetDeviceListListener mGetDeviceListListener;

    private Context mContext;


    public CentralDataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack,
                                     final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, OnGetDeviceListListener onGetDeviceListListener) {
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
        mGetDeviceListListener = onGetDeviceListListener;
    }

    public CentralDataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPSK, TXMqttActionCallBack mqttActionCallBack,
                                 final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, OnGetDeviceListListener onGetDeviceListListener) {
        mContext = context;
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mDownStreamCallBack = downStreamCallBack;
        mGetDeviceListListener = onGetDeviceListListener;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mClient = new CentralTemplateClient(mContext, mBrokerURL, mProductID, mDevName, mDevPSK, null, null, mMqttActionCallBack,
                mJsonFileName, mDownStreamCallBack, mGetDeviceListListener);

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
        mClient.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mClient.setBufferOpts(bufferOptions);
    }

    /**
     * 是否已经连接物联网开发平台
     */
    public boolean isConnected() {
        return mClient.isConnected();
    }

    /**
     * 断开MQTT连接
     */
    public void disconnect() {
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", REQUEST_ID.getAndIncrement());
        mClient.disConnect(mqttRequest);
    }

    /**
     * 订阅主题
     */
    public void subscribeTopic() {
        if (Status.OK != mClient.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
        }
        if (Status.OK != mClient.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
        if (Status.OK != mClient.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe action down stream topic failed!");
        }
        if (Status.OK != mClient.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)) {
            TXLog.e(TAG, "subscribeTopic: subscribe service down stream topic failed!");
        }
    }

    /**
     * 取消订阅主题
     */
    public void unSubscribeTopic() {
        if (Status.OK != mClient.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if (Status.OK != mClient.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if (Status.OK != mClient.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if (Status.OK != mClient.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)) {
            TXLog.e(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
        }
    }

    public Status propertyReport(JSONObject property, JSONObject metadata) {
        return mClient.propertyReport(property, metadata);
    }

    public Status propertyGetStatus(String type, boolean showmeta) {
        return mClient.propertyGetStatus(type, showmeta);
    }

    public Status propertyReportInfo(JSONObject params) {
        return mClient.propertyReportInfo(params);
    }

    public Status propertyClearControl() {
        return mClient.propertyClearControl();
    }

    public Status eventsPost(JSONArray events) {
        return mClient.eventsPost(events);
    }

    public Status eventSinglePost(String eventId, String type, JSONObject params) {
        return mClient.eventSinglePost(eventId, type, params);
    }

    public void checkFirmware() {
        mClient.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
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
                mClient.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                TXLog.e(TAG, "onDownloadFailure:" + errCode);
                mClient.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mClient.reportCurrentFirmwareVersion("0.0.1");
    }

    /**
     * 生成绑定设备的二维码字符串
     * @return 生成的绑定设备的二维码字符串;
     */
    public String generateDeviceQRCodeContent() {
        return mClient.generateDeviceQRCodeContent();
    }

    public Status requestDeviceList(String accessToken) {
        return mClient.requestDeviceList(accessToken);
    }

}
