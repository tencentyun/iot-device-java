package com.tencent.iot.hub.device.android.app.mqtt;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.tencent.iot.hub.device.android.core.gateway.TXGatewayConnection;

import com.tencent.iot.hub.device.android.core.log.TXMqttLogCallBack;

import com.tencent.iot.hub.device.android.core.util.AsymcSslUtils;

import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MQTTSample {

    private static final String TAG = "TXMQTT";
    // Default Value, should be changed in testing
    private String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";

    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";

    private String mSubProductID = "SUBDEV_PRODUCT-ID";
    private String mSubDevName = "SUBDEV_DEV-NAME";
    private String mSubDevPsk = "SUBDEV_DEVICE-SECRET";
    private String mTestTopic = "TEST_TOPIC_WITH_SUB_PUB";
    private String mDevCert;
    private String mDevPriv;

    private boolean mMqttLogFlag;
    private TXMqttLogCallBack mMqttLogCallBack;
	
    private Context mContext;
    private String path2Store = "";

    private TXMqttActionCallBack mMqttActionCallBack;

    /**
     * MQTT连接实例
     */
    private TXGatewayConnection mMqttConnection;

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    public MQTTSample(Context context, TXMqttLogCallBack logCallBack, TXMqttActionCallBack callBack) {
        mContext = context;
        mMqttActionCallBack = callBack;
    }

    public MQTTSample(Context context, TXMqttActionCallBack callBack, String brokerURL, String productId,
                      String devName, String devPSK, String subProductID, String subDevName, String testTopic, String devCertName, String devKeyName,
                      Boolean mqttLogFlag, TXMqttLogCallBack logCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mSubProductID = subProductID;
        mSubDevName = subDevName;
        mTestTopic = testTopic;
        mDevCertName = devCertName;
        mDevKeyName = devKeyName;

        mMqttLogFlag = mqttLogFlag;
        mMqttLogCallBack = logCallBack;

        mContext = context;
        mMqttActionCallBack = callBack;
   }

    public MQTTSample(Context context, TXMqttActionCallBack callBack, String brokerURL, String productId,
                      String devName, String devPsk, String devCert, String devPriv, String subProductID, String subDevName, String testTopic, String devCertName, String devKeyName,
                      Boolean mqttLogFlag, TXMqttLogCallBack logCallBack) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPsk;
        mDevCert = devCert;
        mDevPriv = devPriv;
        mSubProductID = subProductID;
        mSubDevName = subDevName;
        mTestTopic = testTopic;
        mDevCertName = devCertName;
        mDevKeyName = devKeyName;

        mMqttLogFlag = mqttLogFlag;
        mMqttLogCallBack = logCallBack;

        mContext = context;
        mMqttActionCallBack = callBack;
        path2Store = mContext.getCacheDir().getAbsolutePath();
    }

    public MQTTSample(Context context, TXMqttActionCallBack callBack, String brokerURL, String productId,
                      String devName, String devPsk, String devCert, String devPriv, String subProductID, String subDevName, String subDevPsk, String testTopic, String devCertName, String devKeyName,
                      Boolean mqttLogFlag, TXMqttLogCallBack logCallBack) {
        this(context, callBack, brokerURL, productId, devName, devPsk, devCert, devPriv, subProductID, subDevName, testTopic, devCertName, devKeyName, mqttLogFlag, logCallBack);
        mSubDevPsk = subDevPsk;
    }

    public void setSubDevPsk(String val) {
        mSubDevPsk = val;
    }


    public MQTTSample(Context context, TXMqttActionCallBack callBack, String brokerURL, String productId,
                      String devName, String devPSK, String subProductID, String subDevName, String testTopic) {
        mBrokerURL = brokerURL;
        mProductID = productId;
        mDevName = devName;
        mDevPSK = devPSK;
        mSubProductID = subProductID;
        mSubDevName = subDevName;
        mTestTopic = testTopic;

        mContext = context;
        mMqttActionCallBack = callBack;
    }

    private TXOTACallBack oTACallBack = new TXOTACallBack() {

        @Override
        public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {

        }

        @Override
        public boolean onLastestFirmwareReady(String url, String md5, String version) {
            System.out.println("onLastestFirmwareReady url=" + url + " version " + version);
            mMqttConnection.gatewayDownSubdevApp(url, path2Store + "/" + md5, md5, version);
            return true; // false 自动触发下载升级文件  true 需要手动触发下载升级文件
        }

        @Override
        public void onDownloadProgress(int percent, String version) {
            mMqttConnection.gatewaySubdevReportProgress(percent, version);
        }

        @Override
        public void onDownloadCompleted(String outputFile, String version) {
            mMqttConnection.gatewaySubdevReportStart(version);
            mMqttConnection.gatewaySubdevReportSuccess(version);
        }

        @Override
        public void onDownloadFailure(int errCode, String version) {
            mMqttConnection.gatewaySubdevReportFail(errCode, "", version);
        }
    };

     /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXGatewayConnection(mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null ,mMqttLogFlag, mMqttLogCallBack, mMqttActionCallBack);
        mMqttConnection.setSubDevName(mSubDevName);
        mMqttConnection.setSubDevProductKey(mSubDevPsk);
        mMqttConnection.setSubProductID(mSubProductID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPriv != null && mDevCert != null && mDevPriv.length() != 0 && mDevCert.length() != 0) {
            TXLog.i(TAG, "Using cert stream " + mDevPriv + "  " + mDevCert);
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));
        } else if (mDevPSK != null && mDevPSK.length() != 0){
            TXLog.i(TAG, "Using PSK");
            options.setSocketFactory(AsymcSslUtils.getDefaultSLLContext().getSocketFactory());
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

    public void setSubdevOnline() {
        // set subdev online
        mMqttConnection.gatewaySubdevOnline(mSubProductID, mSubDevName);
    }

    public void setSubDevOffline() {
        mMqttConnection.gatewaySubdevOffline(mSubProductID, mSubDevName);
    }

    public void setSubDevBinded() {
        mMqttConnection.gatewayBindSubdev(mSubProductID, mSubDevName, mSubDevPsk);
    }

    public void setSubDevUnbinded() {
        mMqttConnection.gatewayUnbindSubdev(mSubProductID, mSubDevName);
    }

    public void checkSubdevRelation() {
        mMqttConnection.getGatewaySubdevRealtion();
    }

    public void getRemoteConfig() {
        mMqttConnection.getRemoteConfig();
    }

    public void concernRemoteConfig() {
        mMqttConnection.concernConfig();
    }

    public void reportSubDevVersion(String version) {
        mMqttConnection.gatewaySubdevReportVer(version);
    }

    public void initOTA() {
        TXLog.e(TAG, "path2Store " + path2Store);
        mMqttConnection.initOTA(path2Store, oTACallBack);
    }

    /**
     * 订阅广播主题
     */
    public void subscribeBroadCastTopic() {
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
        // 订阅广播主题
        mMqttConnection.subscribeBroadcastTopic(TXMqttConstants.QOS1, mqttRequest);
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

    /**
     * 订阅RRPC主题
     *
     */
    public void subscribeRRPCTopic() {
        // 用户上下文（请求实例）
        MQTTRequest mqttRequest = new MQTTRequest("subscribeTopic", requestID.getAndIncrement());
        // 订阅主题
        mMqttConnection.subscribeRRPCTopic(TXMqttConstants.QOS0, mqttRequest);

    }

    /**
     * 生成一条日志
     * @param logLevel 日志级别：
     *                 错误：TXMqttLogConstants.LEVEL_ERROR
     *                 警告：TXMqttLogConstants.LEVEL_WARN
     *                 通知：TXMqttLogConstants.LEVEL_INFO
     *                 调试：TXMqttLogConstants.LEVEL_DEBUG
     * @param tag
     * @param format
     * @param obj
     */
    public void mLog(int logLevel, final String tag,final String format, final Object... obj) {
        if (mMqttLogFlag)
            mMqttConnection.mLog(logLevel, tag, format, obj);
    }

    /**
     * 发起一次日志上传
     */
    public void uploadLog() {
        mMqttConnection.uploadLog();
    }

    public void checkFirmware() {

        mMqttConnection.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
            @Override
            public void onReportFirmwareVersion(int resultCode, String version, String resultMsg) {
                TXLog.e(TAG, "onReportFirmwareVersion:" + resultCode + ", version:" + version + ", resultMsg:" + resultMsg);
            }

            @Override
            public boolean onLastestFirmwareReady(String url, String md5, String version) {
                TXLog.e(TAG, "MQTTSample onLastestFirmwareReady");
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
