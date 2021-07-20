package com.tencent.iot.hub.device.java.core.log;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT 日志类
 */
public class TXMqttLog {
    private static final Logger logger = LoggerFactory.getLogger(TXMqttLog.class);

    /**
     * 类标记
     */
    public static final String TAG = TXMqttLog.class.getName();

    private TXMqttLogImpl mqttLogImp;

    private TXMqttConnection mMqttConnection;

    /**
     * 日志级别
     */
    public static final String level_str[] = new String[] {"ERR", "ERR", "WRN", "INF", "DBG"};
    private  int mLogLevel = TXMqttLogConstants.LEVEL_FATAL;

    private static final String LOG_OPERATION_PREFIX = "$log/operation/";
    private static final String LOG_OPERATION_RES_PREFIX = "$log/operation/result/";

    private static final String CLIENT_TOKEN = "%s-%d";
    private AtomicInteger mClientTokenNum = new AtomicInteger(0);

    static { Loggor.setLogger(logger); }

    /**
     * 构造函数
     *
     * @param mqttConnection {@link TXMqttConnection}
     */
    public TXMqttLog(TXMqttConnection mqttConnection) {
        this.mMqttConnection = mqttConnection;
        this.mqttLogImp = new TXMqttLogImpl(mqttConnection);
    }

    /**
     * 构造函数
     *
     * @param mqttConnection {@link TXMqttConnection}
     * @param logUrl 日志 URL
     */
    public TXMqttLog(TXMqttConnection mqttConnection, String logUrl) {
        this.mMqttConnection = mqttConnection;
        this.mqttLogImp = new TXMqttLogImpl(mqttConnection, logUrl);
    }

    /**
     * 日志上传初始化，订阅指令下行 topic，向数据上行 topic 发布消息
     *
     * @return 初始化成功时返回 Status.OK；其它返回值表示初始化失败；
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
     *
     * @param logLevel 日志等级
     */
    public void setMqttLogLevel(int logLevel) {
        this.mLogLevel = logLevel;
    }

    /**
     * 将一条日志保存到日志队列中
     * @param logLevel 日志级别
     * @param tag 日志标记
     * @param format 格式
     * @param obj 日志内容
     * @return 操作结果
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
        if (!(jsonDocument == null || jsonDocument.length() == 0)) {
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
            Loggor.error(TAG, e.getMessage() + "build report info failed");
            return "";
        }

        return documentJSONObj.toString();
    }

}

