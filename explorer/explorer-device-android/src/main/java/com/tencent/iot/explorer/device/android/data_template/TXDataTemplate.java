package com.tencent.iot.explorer.device.android.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.CustomLog;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.DataTemplate;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.explorer.device.java.utils.ILog;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_ACTION;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_ACTION_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_EVENTS_POST;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_EVENTS_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_EVENT_POST;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_EVENT_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CLEAR_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CLEAR_CONTROL_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_CONTROL_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_GET_STATUS_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_INFO;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_INFO_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.METHOD_PROPERTY_REPORT_REPLY;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_ACTION_UP_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_EVENT_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_EVENT_UP_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_PROPERTY_UP_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.ACTION_UP_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.EVENT_UP_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplatePubTopic.PROPERTY_UP_STREAM_TOPIC;

public class TXDataTemplate extends DataTemplate {

    public static final String TAG = "TXDATATEMPLATE";
    private static final CustomLog CUSTOM_LOG = new CustomLog(TAG);

    /**
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productId         产品名
     * @param deviceName        设备名，唯一
     * @param jsonFileName      数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName,
                          final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(connection, productId, deviceName, new TXDataTemplateJson(context, jsonFileName), downStreamCallBack, CUSTOM_LOG);
    }
}
