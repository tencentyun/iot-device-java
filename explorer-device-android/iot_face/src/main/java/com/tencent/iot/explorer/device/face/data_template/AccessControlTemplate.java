package com.tencent.iot.explorer.device.face.data_template;

import android.content.Context;
import android.os.Environment;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.face.resource.TXResourceCallBack;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
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

public class AccessControlTemplate {
    private static final String TAG = "TXAccessControlTemplate";
    // Default Value, should be changed in testing
    private String mBrokerURL = "ssl://iotcloud-mqtt.gz.tencentdevices.com:8883";
    private String mProductID = "PRODUCT_ID";
    private String mDevName = "DEVICE_NAME";
    private String mDevPSK = "DEVICE_SECRET";
    private String mDevCertName = "DEVICE_CERT_NAME ";
    private String mDevKeyName  = "DEVICE_KEY_NAME ";
    private String mJsonFileName = "JSON_FILE_NAME";
    private Context mContext;

    private String mDeviceType = "DEVICE_TYPE";

    private TXMqttActionCallBack mMqttActionCallBack;
    /**
     * MQTT连接实例
     */
    private TXFaceKitTemplateClient mMqttConnection;
    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    TXDataTemplateDownStreamCallBack mDownStreamCallBack = null;

    TXAuthCallBack mAuthCallBack = null;

    public AccessControlTemplate(Context context) {
        mContext = context;
    }

    public AccessControlTemplate(Context context, String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack,
                                 final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXAuthCallBack authCallBack) {
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
        mAuthCallBack = authCallBack;
    }

    public AccessControlTemplate(Context context, String brokerURL, String productId, String devName, String devPSK, TXMqttActionCallBack mqttActionCallBack,
                                 final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, TXAuthCallBack authCallBack) {
        mContext = context;
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mDownStreamCallBack = downStreamCallBack;
        mAuthCallBack = authCallBack;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXFaceKitTemplateClient( mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mMqttActionCallBack,
                mJsonFileName, mDownStreamCallBack, mAuthCallBack);

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
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
    }

    /**
     * 取消订阅主题
     *
     */
    public void unSubscribeTopic() {
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
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

    public Status eventSinglePost(String eventId, String type, JSONObject params){
        return  mMqttConnection.eventSinglePost(eventId, type, params);
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

    public void checkResource() {

        mMqttConnection.initResource(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXResourceCallBack() {

            @Override
            public void onReportResourceVersion(int resultCode, JSONArray resourceList, String resultMsg) {
                TXLog.e(TAG, "onReportResourceVersion:" + resultCode + ", resourceList:" + resourceList + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestResourceReady(String url, String md5, String version) {
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

        JSONArray resourceList = new JSONArray();
        mMqttConnection.reportCurrentResourceVersion(resourceList);
    }
}
