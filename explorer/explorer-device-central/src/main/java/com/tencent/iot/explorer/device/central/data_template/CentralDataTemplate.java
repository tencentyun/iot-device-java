package com.tencent.iot.explorer.device.central.data_template;

import android.content.Context;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.mqtt.TXMqttConnection;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.central.callback.OnGetDeviceListListener;
import com.tencent.iot.explorer.device.central.consts.Common;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_DOWN_PREFIX;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TOPIC_SERVICE_UP_PREFIX;

public class CentralDataTemplate extends TXDataTemplate {

    public static final String METHOD_WS_ACTIVE_PUSH = "ws_active_push";

    public static final String METHOD_WS_ACTIVE_PUSH_REPLY = "ws_active_push_reply";

    private TXMqttConnection mConnection;

    private String mServiceDownStreamTopic;

    private String mServiceUptreamTopic;

    private Context mContext;

    private String mProductID;

    private String mDevName;

    private OnGetDeviceListListener mGetDeviceListListener;

    /**
     * @param context            用户上下文（这个参数在回调函数时透传给用户）
     * @param connection
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public CentralDataTemplate(Context context, TXMqttConnection connection, String productId, String deviceName, String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack, OnGetDeviceListListener onGetDeviceListListener) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mConnection = connection;
        this.mServiceDownStreamTopic = TOPIC_SERVICE_DOWN_PREFIX + productId + "/" + deviceName;
        this.mServiceUptreamTopic = TOPIC_SERVICE_UP_PREFIX + productId + "/" + deviceName;
        this.mContext = context;
        this.mProductID = productId;
        this.mDevName = deviceName;
        this.mGetDeviceListListener = onGetDeviceListListener;
    }

    @Override
    public void onMessageArrived(String topic, MqttMessage message) throws Exception {
        super.onMessageArrived(topic, message);
        TXLog.d(TAG, message.toString());
        if (topic.equals(mServiceDownStreamTopic)) {
            onServiceMessageReceived(message);
        }
    }

    private void onServiceMessageReceived(final MqttMessage message) {
        try {
            JSONObject jsonObj = new JSONObject(new String(message.getPayload()));
            String method = jsonObj.getString("method");
            if (METHOD_WS_ACTIVE_PUSH_REPLY.equals(method)) {
                int code = jsonObj.getInt("code");
                if (code == 0) {
                    JSONObject response = jsonObj.getJSONObject("data");
                    JSONArray deviceIds = response.getJSONArray("deviceIds");
                    ArrayList<String> deviceList = new ArrayList<>();
                    for (int i=0;i<deviceIds.length();i++){
                        deviceList.add(deviceIds.getString(i));
                    }
                    mGetDeviceListListener.onGetDeviceList(deviceList);
                } else {
                    TXLog.e(TAG, String.format("request device list error, code=%d", code));
                }
            }
        } catch (Exception e) {
            TXLog.e(TAG, "onServiceMessageArrivedCallBack: invalid message: " + message);
        }
    }

    public Status requestDeviceList(String accessToken) {
        //构造发布信息
        JSONObject object = new JSONObject();
        JSONObject params = new JSONObject();
        String clientToken = UUID.randomUUID().toString();
        try {
            object.put("method", METHOD_WS_ACTIVE_PUSH);
            object.put("clientToken", clientToken);
            params.put("AccessToken", accessToken);
            object.put("params", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(object.toString().getBytes());
        return mConnection.publish(mServiceUptreamTopic, message, null);
    }

    public Status refreshToken(String accessToken) {
        //构造发布信息
        JSONObject object = new JSONObject();
        JSONObject params = new JSONObject();
        String clientToken = UUID.randomUUID().toString();
        try {
            object.put("method", Common.REFRESH_HTTP_ACCESS_TOKEN);
            object.put("clientToken", clientToken);
            params.put("AccessToken", accessToken);
            object.put("params", params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(object.toString().getBytes());
        return mConnection.publish(mServiceUptreamTopic, message, null);
    }
}
