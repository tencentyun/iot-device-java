package com.tencent.iot.hub.device.android.core.log;

import android.text.TextUtils;

import com.tencent.iot.hub.device.android.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.android.core.util.TXLog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class TXMqttLog {

    public static final String TAG = TXMqttLog.class.getName();

    private TXMqttLogImpl mqttLogImp;

    private  TXMqttConnection mMqttConnection;

    public static final String level_str[] = new String[] {"ERR", "ERR", "WRN", "INF", "DBG"};
    private  int mLogLevel = TXMqttLogConstants.LEVEL_FATAL;

    private static final String LOG_OPERATION_PREFIX = "$log/operation/";
    private static final String LOG_OPERATION_RES_PREFIX = "$log/operation/result/";

    private static final String CLIENT_TOKEN = "%s-%d";
    private AtomicInteger mClientTokenNum = new AtomicInteger(0);

    public TXMqttLog(TXMqttConnection mqttConnection) {
        this.mMqttConnection = mqttConnection;
        this.mqttLogImp = new TXMqttLogImpl(mqttConnection);
    }

    /**
     * 日志上传初始化，订阅指令下行topic，向数据上行topic发布消息
     * @return 初始化成功时返回Status.OK; 其它返回值表示初始化失败；
     */
    public Status initMqttLog() {
        //初始化之前处理上次的离线日志
        mqttLogImp.uploadOfflineLog();

        Status status;
        status = subscribeLogResultTopic(mMqttConnection);
        if (status != Status.OK) {
            return status;
        }
        return publishLogLevelGet(mMqttConnection);
    }

    /**
     * 设置日志级别，在接受消息回调函数中调用
     * @param logLevel 日志等级
     */
    public void setMqttLogLevel(int logLevel) {
        this.mLogLevel = logLevel;
    }

    /**
     * 将一条日志保存到日志队列中
     */
    public boolean saveMqttLog(final int logLevel, final String tag, final String format, final Object... obj) {
        //低于设置日志信息等级的信息不存储
        if(mMqttConnection != null && logLevel <= mLogLevel) {
            String log = buildMqttLog(logLevel, tag, format, obj);
            return mqttLogImp.appendToLogDeque(log);
        }
        return false;
    }

    /**
     * 触发一次日志上传
     */
    public void uploadMqttLog() {
        mqttLogImp.uploadMqttLog();
    }

    /**
     * 构造一条日志消息
     * 格式：[日志内容（每行日志格式为"LEVEL|DATETIME|TAG|CONTENT"，行与行之间采用"\n\f,"分隔）]”
     */
    private String buildMqttLog(final int logLevel,final String tag,final String format, final Object... obj) {
        long nowCurrentMillis = System.currentTimeMillis();
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String logTime = timeFormatter.format(nowCurrentMillis);
        String log = obj == null ? format : String.format(format, obj);
        if (log == null) {
            log = "";
        }

        return String.format("%s|%s|%s|%s\n\f", level_str[logLevel], logTime, tag, log);
    }

    /**
     * 订阅指令上行topic
     */
    private Status subscribeLogResultTopic(TXMqttConnection mqttConnection) {
        String topic = LOG_OPERATION_RES_PREFIX + mqttConnection.mProductId + "/" + mqttConnection.mDeviceName;
        int qos = TXMqttConstants.QOS0;
        return mqttConnection.subscribe(topic, qos, "Subscribe Log result topic");
    }

    /**
     * 发布消息给数据上行topic
     */
    private Status publishLogLevelGet(TXMqttConnection mqttConnection) {
        String topic = LOG_OPERATION_PREFIX + mqttConnection.mProductId + "/" + mqttConnection.mDeviceName;

        String clientToken = String.format(CLIENT_TOKEN, mqttConnection.mClientId, mClientTokenNum.getAndIncrement());
        String jsonDocument = buildGetJsonDocument(clientToken);

        MqttMessage mqttMessage = new MqttMessage();
        if (!TextUtils.isEmpty(jsonDocument)) {
            mqttMessage.setPayload(jsonDocument.getBytes());
        }
        mqttMessage.setQos(TXMqttConstants.QOS0);

        return  mqttConnection.publish(topic, mqttMessage, "Publish GET message");
    }

    private String buildGetJsonDocument(String clientToken) {
        JSONObject documentJSONObj = new JSONObject();

        try {
            documentJSONObj.put(TXMqttLogConstants.TYPE, TXMqttLogConstants.GET_LOG_LEVEL);
            documentJSONObj.put(TXMqttLogConstants.CLIENT_TOKEN, clientToken);
        } catch (JSONException e) {
            TXLog.e(TAG, e, "build report info failed");
            return "";
        }

        return documentJSONObj.toString();
    }

}

