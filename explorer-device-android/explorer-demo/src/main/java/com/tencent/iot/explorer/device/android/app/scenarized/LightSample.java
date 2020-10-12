package com.tencent.iot.explorer.device.android.app.scenarized;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTACallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXOTAConstansts;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;

public class LightSample {
    private static final String TAG = "TXLightSample";

    // device info
    private Context mContext;
    private String mDevPSK = "DEVICE-SECRET";
    private String mDevCertName = "DEVICE_CERT-NAME ";
    private String mDevKeyName  = "DEVICE_KEY-NAME ";
    public String mVersion = "0.0.1";

    /**上报周期*/
    private int reportPeriod = 10 * 1000;

    private TXDataTemplateClient mDataTemplateClient;

    private static AtomicInteger requestID = new AtomicInteger(0);

    private reportPropertyPeriodically mReportThread = null;

    //light property
    public ConcurrentHashMap<String, Object> mProperty = null;

    public LightSample(Context context, String brokerURL, String productId, String devName, String devCertName, String devKeyName, final String jsonFileName) {
        this.mContext = context;
        this.mDevCertName = devCertName;
        this.mDevKeyName = devKeyName;
        //初始化模板数据
        initTemplateData();
        mDataTemplateClient = new TXDataTemplateClient( context, brokerURL, productId, devName, null,null,null, new LightSampleMqttActionCallBack(), jsonFileName, new LightSampleDownStreamCallBack());
    }

    public LightSample(Context context, String brokerURL, String productId, String devName, String devPSK, final String jsonFileName) {
        this.mContext = context;
        //初始化模板数据
        initTemplateData();
        mDataTemplateClient = new TXDataTemplateClient( context, brokerURL, productId, devName, devPSK,null,null, new LightSampleMqttActionCallBack(), jsonFileName, new LightSampleDownStreamCallBack());
    }

    /**
     * 初始化属性
     */
    private void initTemplateData(){
        //初始化属性
        mProperty = new ConcurrentHashMap<>();
        mProperty.put("power_switch",0);
        mProperty.put("color",0);
        mProperty.put("brightness",1);
        mProperty.put("name","light");
    }

    /**
     * 连接上线
     */
    public void online() {
        //初始化连接
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0) {
            TXLog.i(TAG, "Using PSK");
            options.setSocketFactory(AsymcSslUtils.getSocketFactory());
        } else {
            TXLog.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(mContext, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest = new TXMqttRequest("connect", requestID.getAndIncrement());
        mDataTemplateClient.connect(options, mqttRequest);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mDataTemplateClient.setBufferOpts(bufferOptions);
    }

    /**
     * 下线
     */
    public void offline() {
        if(mDataTemplateClient.isConnected()) {
            if(null != mReportThread) {
                mReportThread.interrupt();
                mReportThread = null;
            }
            if (Status.OK != mDataTemplateClient.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)) {
                TXLog.e(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
            }
            if (Status.OK != mDataTemplateClient.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)) {
                TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
            }
            if (Status.OK != mDataTemplateClient.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)) {
                TXLog.e(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
            }
            TXMqttRequest mqttRequest = new TXMqttRequest("disconnect", requestID.getAndIncrement());
            mDataTemplateClient.disConnect(mqttRequest);
        }
    }

    /**
     * 检查固件更新
     */
    public void checkFirmware() {

        mDataTemplateClient.initOTA(Environment.getExternalStorageDirectory().getAbsolutePath(), new TXOTACallBack() {
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
                mDataTemplateClient.reportOTAState(TXOTAConstansts.ReportState.DONE, 0, "OK", version);
                mVersion = version;
            }

            @Override
            public void onDownloadFailure(int errCode, String version) {
                TXLog.e(TAG, "onDownloadFailure:" + errCode);

                mDataTemplateClient.reportOTAState(TXOTAConstansts.ReportState.FAIL, errCode, "FAIL", version);
            }
        });
        mDataTemplateClient.reportCurrentFirmwareVersion(mVersion);
    }

    /**
     * 设备是否在线
     * @return 在线状态
     */
    public boolean isOnline(){
        return mDataTemplateClient.isConnected();
    }

    /**
     * 根据接收的json文件设置属性值,开关改变时触发事件
     * @param params 属性描述文件
     */
    private void setPropertyBaseOnJson(JSONObject params) {
        Iterator<String> it = params.keys();
        while(it.hasNext()){
            String key = it.next();
            if(mProperty.containsKey(key)) {
                try {
                    if(key.equals("power_switch") &&  mProperty.get(key) != params.get(key)) {
                        TXLog.e(TAG,mProperty.get(key).toString() + params.get(key).toString());
                        LightSwitchEventPost(params.getInt(key));
                    }
                    mProperty.put(key, params.get(key));
                } catch (JSONException e) {
                    TXLog.e(TAG, "setPropertyBaseOnJson: failed! Invalid json!");
                    return;
                }
            }
        }
    }

    /**
     * 获取最后上报的信息以及未处理的control
     */
    private void LightStatusGet() {
        if(Status.OK != mDataTemplateClient.propertyGetStatus("report", false)) {
            TXLog.e(TAG, "property get statuts failed!");
        }

        if(Status.OK != mDataTemplateClient.propertyGetStatus("control", false)) {
            TXLog.e(TAG, "property get statuts failed!");
        }
    }

    /**
     * 上报设备的基本信息
     */
    private void LightInfoReport() {
        JSONObject params = new JSONObject();
        try {
            JSONObject label = new JSONObject();  //device label
            label.put("version", "v1.0.0");
            label.put("company", "tencent");

            params.put("module_hardinfo", "v1.0.0");
            params.put("module_softinfo", "v1.0.0");
            params.put("fw_ver", "v1.0.0");
            params.put("imei", "0");
            params.put("mac", "00:00:00:00");
            params.put("device_label", label);
        } catch (JSONException e) {
            TXLog.e(TAG, "Construct light info failed!");
            return;
        }
        if(Status.OK != mDataTemplateClient.propertyReportInfo(params)) {
            TXLog.e(TAG, "light info report failed!");
        }
    }

    /**
     * 根据LightStatusGet中发送control的回复的参数判断需不需要处理control,如果处理完毕，则清除control
     */
    private void LightControlClear() {
        if (Status.OK != mDataTemplateClient.propertyClearControl()) {
            TXLog.e(TAG, "clear control failed!");
        }
    }

    /**
     * 当开关变化时，发送事件
     */
    private void LightSwitchEventPost(int isSwitchOpen) {
        JSONArray events = new JSONArray();

        //event:status_report
        try {
            JSONObject event = new JSONObject();
            event.put("eventId","status_report");
            event.put("type", "info");
            event.put("timestamp", System.currentTimeMillis());

            JSONObject params = new JSONObject();
            params.put("status",0);

            if(1 == isSwitchOpen)
                params.put("message","switch open!");
            else
                params.put("message","switch close!");

            event.put("params", params);

            events.put(event);
        } catch (JSONException e) {
            TXLog.e(TAG, "Construct event params failed!");
            return;
        }

        if(Status.OK != mDataTemplateClient.eventsPost(events)){
            TXLog.e(TAG, "events post failed!");
        }
    }

    /**
     * 周期性上报属性
     */
    private class reportPropertyPeriodically extends Thread {
        public void run() {
            while (!isInterrupted()) {
                JSONObject property = new JSONObject();
                for(Map.Entry<String, Object> entry: mProperty.entrySet())
                {
                    try {
                        property.put(entry.getKey(),entry.getValue());
                    } catch (JSONException e) {
                        TXLog.e(TAG, "construct property failed!");
                    }
                }

                if (Status.OK != mDataTemplateClient.propertyReport(property, null)) {
                    TXLog.e(TAG, "report property failed!");
                    break;
                }

                try {
                    Thread.sleep(reportPeriod);
                } catch (InterruptedException e) {
                    TXLog.e(TAG, "The thread has been interrupted");
                    break;
                }
            }
        }
    }

    /**
     * 实现下行消息处理的回调接口
     */
    private class LightSampleDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //Just print
            Log.d(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //根据控制和状态信息设置属性
            try {
                Iterator<String> it = data.keys();
                if(it.hasNext()) {
                    String key = it.next();
                    if (key.equals("reported")) { //report
                        JSONObject params = data.getJSONObject("reported");
                        setPropertyBaseOnJson(params);
                    } else { //control
                        JSONObject params = data.getJSONObject("control");
                        setPropertyBaseOnJson(params);
                        LightControlClear();
                    }
                }
            } catch (JSONException e) {
                TXLog.e(TAG,"get status reply  params invalid!");
            }
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            //解析控制信息，此处根据参数设置相应的属性
            //set property
            setPropertyBaseOnJson(msg);

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            TXLog.d(TAG, "action [%s] received, input:" + params, actionId);
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    int period;
                    period = params.getInt("period");

                    //实现闪烁
                    mProperty.put("color", 1);
                    TimeUnit.MILLISECONDS.sleep(period*1000);
                    mProperty.put("color", 0);
                    TimeUnit.MILLISECONDS.sleep(period*1000);
                    mProperty.put("color", 1);

                    //construct result
                    JSONObject result = new JSONObject();
                    result.put("code",0);
                    result.put("status", "some message when error");
                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);
                    result.put("response", response);
                    return result;
                } catch (Exception e0) {
                    try {
                        //construct error result
                        JSONObject result = new JSONObject();
                        result.put("code", 1);
                        result.put("status", "action execute failed!");
                        // response based on output
                        JSONObject response = new JSONObject();
                        response.put("result", 1);
                        result.put("response", response);
                        return result;
                    } catch (Exception e1) {
                        return  null;
                    }
                }
            }
            return null;
        }
    }

    /**
     * 实现TXMqttActionCallBack回调接口
     */
    private class LightSampleMqttActionCallBack extends TXMqttActionCallBack {
        /**初次连接成功则订阅相关主题*/
        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            if(Status.OK == status && !reconnect) { //初次连接订阅主题,重连后会自动订阅主题
                if (Status.OK != mDataTemplateClient.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)) {
                    TXLog.e(TAG, "subscribeTopic: subscribe property down stream topic failed!");
                }
                if (Status.OK != mDataTemplateClient.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)) {
                    TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
                }
                if (Status.OK != mDataTemplateClient.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)) {
                    TXLog.e(TAG, "subscribeTopic: subscribe event down stream topic failed!");
                }
            } else {
                String userContextInfo = "";
                if (userContext instanceof TXMqttRequest) {
                    userContextInfo = userContext.toString();
                }
                String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                        status.name(), reconnect, userContextInfo, msg);
                TXLog.d(TAG,logInfo);
            }
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            TXLog.e(TAG,logInfo);
            mReportThread.interrupt();
            mReportThread = null;
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            TXLog.d(TAG,logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            TXLog.d(TAG,logInfo);
        }

        /**订阅属性下行主题成功则获取状态和上报信息，启动周期性上报属性线程*/
        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                TXLog.e(TAG,logInfo);
            } else {
                String topic = Arrays.toString(asyncActionToken.getTopics());
                if(topic.substring(1,topic.length()-1).equals(mDataTemplateClient.mPropertyDownStreamTopic)) {
                    LightInfoReport(); //发送基本信息
                    LightStatusGet();  //获取最后的状态以及未处理的control信息
                    if(null == mReportThread) {
                        mReportThread = new LightSample.reportPropertyPeriodically(); //周期性上报
                        mReportThread.start();
                    }
                }
                TXLog.d(TAG,logInfo);
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            TXLog.d(TAG,logInfo);
        }

        /**消息会在LightSampleDownStreamCallBack中被处理*/
        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            //do nothing, message will be process in LightSampleDownStreamCallBack
        }
    }
}
