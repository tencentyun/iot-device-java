package com.tencent.iot.explorer.device.java.core.data_template;



import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;
import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.utils.Loggor;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.*;


public class DataTemplateSample {
    private static final String TAG = DataTemplateSample.class.getSimpleName();
    // Default Value, should be changed in testing
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";
    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";
    private String mJsonFileName = "JSON_FILE_NAME";
    private String mJsonFilePath = "";

    private String workDir = System.getProperty("user.dir") + "/explorer/explorer-device-java/src/test/resources/";

    private static final Logger LOG = LoggerFactory.getLogger(DataTemplateSample.class);

    static {
        Loggor.setLogger(LOG);
    }

    private TXMqttActionCallBack mMqttActionCallBack;

    private TXDataTemplateClient mMqttConnection;

    private static AtomicInteger requestID = new AtomicInteger(0);

    TXDataTemplateDownStreamCallBack mDownStreamCallBack = null;

    public DataTemplateSample(String brokerURL, String productId, String devName, String devPSK, String devCertName, String devKeyName, TXMqttActionCallBack mqttActionCallBack,
                              final String jsonFileName, final String jsonFilePath, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mDevCertName = devCertName;
        mDevKeyName = devKeyName;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mJsonFilePath = jsonFilePath;
        mDownStreamCallBack = downStreamCallBack;
    }

    public DataTemplateSample( String brokerURL, String productId, String devName, String devPSK,TXMqttActionCallBack mqttActionCallBack,
                              final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mMqttActionCallBack = mqttActionCallBack;
        mJsonFileName = jsonFileName;
        mDownStreamCallBack = downStreamCallBack;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXDataTemplateClient(mBrokerURL, mProductID, mDevName, mDevPSK,
                null,null, mMqttActionCallBack, mJsonFileName, mJsonFilePath, mDownStreamCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            Loggor.info(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-java sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            Loggor.info(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByFile(workDir + mDevCertName, workDir + mDevKeyName));
        }
        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);
    }

    public TXMqttConstants.ConnectStatus getConnectStatus() {
        return mMqttConnection.getConnectStatus();
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
           Loggor.debug(TAG, "subscribeTopic: subscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)){
            Loggor.debug(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)){
            Loggor.debug(TAG, "subscribeTopic: subscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)){
            Loggor.debug(TAG, "subscribeTopic: subscribe service down stream topic failed!");
        }
    }

    /**
     * 取消订阅主题
     *
     */
    public void unSubscribeTopic() {
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)){
            Loggor.debug(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)){
            Loggor.debug(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)){
            Loggor.debug(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)){
            Loggor.debug(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
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

        mMqttConnection.initOTA(workDir, new TXOTACallBack() {
            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                Loggor.error(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                return false;
            }

            @Override
            public void onDownloadProgress(int percent, String version) {
                Loggor.error(TAG, "onDownloadProgress:" + percent);
            }

            @Override
            public void onDownloadCompleted(String outputFile, String version) {
                Loggor.error(TAG, "onDownloadCompleted:" + outputFile + ", version:" + version);

                mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                Loggor.error(TAG, "onDownloadFailure:" + errCode);

                mMqttConnection.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mMqttConnection.reportCurrentFirmwareVersion("0.0.1");
    }

}
