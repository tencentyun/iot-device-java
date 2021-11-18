package com.tencent.iot.explorer.device.video.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.rtc.data_template.TXTRTCTemplateClient;
import com.tencent.iot.explorer.device.video.recorder.TXVideoTemplateClient;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.SERVICE_DOWN_STREAM_TOPIC;

public class VideoDataTemplateSample {
    private String TAG = VideoDataTemplateSample.class.getSimpleName();

    private String mBrokerURL = null;
    private String mProductID = "PRODUCT-ID";
    private String mDevName = "DEVICE-NAME";
    private String mDevPSK = "DEVICE-SECRET";
    private Context mContext;
    private TXMqttActionCallBack mMqttActionCallBack;

    /**
     * MQTT连接实例
     */
    private TXVideoTemplateClient mMqttConnection;

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    public VideoDataTemplateSample(Context context, String brokerURL, String productId, String devName, String devPsk, TXMqttActionCallBack mqttActionCallBack) {
        this.mContext = context;
        this.mBrokerURL = brokerURL;
        this.mProductID = productId;
        this.mDevName = devName;
        this.mDevPSK = devPsk;
        this.mMqttActionCallBack = mqttActionCallBack;
    }

    /**
     * 建立MQTT连接
     */
    public void connect() {
        mMqttConnection = new TXVideoTemplateClient(mContext, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mMqttActionCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            TXLog.i(TAG, "Using PSK");
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);
    }

    public String generateDeviceQRCodeContent() {
        return mMqttConnection.generateDeviceQRCodeContent();
    }

    public void disconnect() {
        TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mMqttConnection.disConnect(mqttRequest);
    }

    public Status reportCallStatusProperty(String p2pInfo) {
        return mMqttConnection.reportXp2pInfo(p2pInfo);
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
            TXLog.e(TAG, "subscribeTopic: subscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)){
            TXLog.e(TAG, "subscribeTopic: subscribe service down stream topic failed!");
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
            TXLog.e(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)){
            TXLog.e(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
        }
    }
}
