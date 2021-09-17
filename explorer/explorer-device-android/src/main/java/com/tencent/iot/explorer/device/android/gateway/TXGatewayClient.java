package com.tencent.iot.explorer.device.android.gateway;

import android.content.Context;
import android.util.Base64;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.android.mqtt.TXAlarmPingSender;
import com.tencent.iot.explorer.device.android.utils.TXLog;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class TXGatewayClient extends TXDataTemplateClient {
    public static final String TAG = "TXGATEWAYCLIENT";
    private HashMap<String, TXGatewaySubdev> mSubdevs = new HashMap<String, TXGatewaySubdev>();
    private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";
    private static final String GW_OPERATION_PREFIX = "$gateway/operation/";
    private Context mContext;

    public TXGatewayClient(Context context, String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                           MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                           final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack, jsonFileName,downStreamCallBack);
        this.mContext = context;
    }

    /**
     *
     * @param productId
     * @param devName
     * @return null if not existed otherwise the subdev
     */
    public TXGatewaySubdev findSubdev(String productId, String devName) {
        TXLog.d(TAG, "input product id is " + productId + ", input device name is " + devName);
        TXLog.d(TAG, "The hashed information is " + mSubdevs);
        return mSubdevs.get(productId + devName);
    }

    /**
     * remove the subdev
     * @param productId
     * @param devName
     * @return
     */
    public synchronized TXGatewaySubdev removeSubdev(String productId, String devName) {
        TXGatewaySubdev dev2Remove = findSubdev(productId, devName);
        if (null != dev2Remove) {
            // 发现存在子设备，在移除前尝试销毁相关资源
            dev2Remove.destroy();
            return mSubdevs.remove(productId + devName);
        }
        return null;
    }

    public synchronized TXGatewaySubdev removeSubdev(TXGatewaySubdev subdev) {
        return mSubdevs.remove(subdev.mProductId + subdev.mDeviceName);
    }

//    /**
//     *  add a new subdev entry
//     * @param productId
//     * @param  deviceName
//     * @param jsonFileName
//     * @param downStreamCallBack
//     */
//    public synchronized void addSubdev(String productId, String deviceName, final String jsonFileName,
//                                       TXGatewaySubdevActionCallBack actionCallBack, TXDataTemplateDownStreamCallBack downStreamCallBack) {
//        if(null == findSubdev(productId, deviceName)) {
//            TXGatewaySubdev subdev = new TXGatewaySubdev(this, this.mContext, productId, deviceName,
//                                                            jsonFileName, actionCallBack, downStreamCallBack);
//            mSubdevs.put(productId + deviceName, subdev);
//        } else {
//            Log.d(TAG,"Sub dev already exits!");
//        }
//    }

    public synchronized void addSubdev(TXGatewaySubdev subdev) {
        mSubdevs.put(subdev.mProductId + subdev.mDeviceName, subdev);
    }

//    /**
//     *  Get the subdev status
//     * @param productId
//     * @param devName
//     * @return the status of subdev
//     */
//    public Status getSubdevStatus(String productId, String devName) {
//        TXGatewaySubdev subdev = findSubdev(productId, devName);
//        if (subdev == null) {
//            return Status.SUBDEV_STAT_NOT_EXIST;
//        }
//        return subdev.getSubdevStatus();
//    }
//
//    /**
//     * set the status of the subdev
//     * @param productId
//     * @param devName
//     * @param stat
//     * @return the status of operation
//     */
//    public Status setSubdevStatus(String productId, String devName, Status stat) {
//        TXGatewaySubdev subdev = findSubdev(productId, devName);
//        if (subdev == null) {
//            return Status.SUBDEV_STAT_NOT_EXIST;
//        }
//        subdev.setSubdevStatus(stat);
//        return Status.OK;
//    }

    /**
     * publish the offline message for the subdev
     * @param subProductID
     * @param subDeviceName
     * @return the result of operation
     */
    public Status subdevOffline(String subProductID, String subDeviceName) {
        return setSubdevStatus(subProductID, subDeviceName, "offline");
    }

    /**
     * publish the online message for the subdev
     * @param subProductID
     * @param subDeviceName
     * @return the result of operation
     */
    public Status subdevOnline(String subProductID, String subDeviceName) {
        return setSubdevStatus(subProductID, subDeviceName, "online");
    }

    private Status setSubdevStatus(String subProductID, String subDeviceName, String status) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            TXLog.e(TAG, "Cant find the subdev");
            return Status.SUBDEV_STAT_NOT_EXIST;
        } else  {
            if (status.equals("online")) {
                if(subdev.getSubdevStatus() == Status.SUBDEV_STAT_ONLINE) {
                    TXLog.e(TAG, "subdev has already online!");
                    return  Status.SUBDEV_STAT_ONLINE;
                }
            } else if (status.equals("offline")) {
                if (subdev.getSubdevStatus() == Status.SUBDEV_STAT_OFFLINE) {
                    TXLog.e(TAG, "subdev has already offline!");
                    return  Status.SUBDEV_STAT_OFFLINE;
                }
            }
        }

        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
        TXLog.d(TAG, "set " + subProductID + " & " + subDeviceName + " to " + status);

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", status);
            JSONObject plObj = new JSONObject();
            String strDev = "[{'product_id':'" + subProductID +"','device_name':'" + subDeviceName + "'}]";
            JSONArray devs = new JSONArray(strDev);
            plObj.put("devices", devs);
            obj.put("payload", plObj);
        } catch (JSONException e) {
            return Status.ERROR;
        }

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(obj.toString().getBytes());
        TXLog.d(TAG, "publish message " + message);
        return super.publish(topic, message, null);
    }

//    /**
//     * 订阅数据模板相关主题
//     * @param topicId 主题ID
//     * @param qos QOS等级
//     * @return 发送请求成功时返回Status.OK;
//     */
//    public Status subscribeSubDevTopic(String subProductID, String subDeviceName,TXDataTemplateConstants.TemplateSubTopic topicId, final int qos) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.subscribeTemplateTopic(topicId, qos);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 取消订阅数据模板相关主题
//     * @param topicId 主题ID
//     * @return 发送请求成功时返回Status.OK;
//     */
//    public Status unSubscribeSubDevTopic(String subProductID, String subDeviceName,TXDataTemplateConstants.TemplateSubTopic topicId) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.unSubscribeTemplateTopic(topicId);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 属性上报
//     * @param property 属性的json
//     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
//     * @return 结果
//     */
//    public Status subDevPropertyReport(String subProductID, String subDeviceName,JSONObject property, JSONObject metadata) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.propertyReport(property, metadata);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 获取状态
//     * @param type 类型
//     * @param showmeta 是否携带showmeta
//     * @return 结果
//     */
//    public Status subDevPropertyGetStatus(String subProductID, String subDeviceName,String type, boolean showmeta) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.propertyGetStatus(type, showmeta);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 设备基本信息上报
//     * @param params 参数
//     * @return 结果
//     */
//    public Status subDevPropertyReportInfo(String subProductID, String subDeviceName, JSONObject params) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.propertyReportInfo(params);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 清理控制信息
//     * @return 结果
//     */
//    public Status subDevPropertyClearControl(String subProductID, String subDeviceName) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.propertyClearControl();
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 单个事件上报
//     * @param eventId 事件ID
//     * @param type 事件类型
//     * @param params 参数
//     * @return 结果
//     */
//    public Status subDevEventSinglePost(String subProductID, String subDeviceName,String eventId, String type, JSONObject params) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.eventSinglePost(eventId, type, params);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }
//
//    /**
//     * 多个事件上报
//     * @param events 事件集合
//     * @return 结果
//     */
//    public Status subDevEventsPost(String subProductID, String subDeviceName, JSONArray events) {
//        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
//        if(null != subdev){
//            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
//                return  subdev.eventsPost(events);
//            } else {
//                return  Status.SUBDEV_STAT_OFFLINE;
//            }
//        }
//        return Status.SUBDEV_STAT_NOT_EXIST;
//    }

    private boolean consumeGwOperationMsg(String topic, MqttMessage message) {
        if (!topic.startsWith(GW_OPERATION_RES_PREFIX)) {
            return false;
        }
        TXLog.d(TAG, "got gate operation messga " + topic + message);

        try {
            byte[] payload = message.getPayload();
            JSONObject jsonObject = new JSONObject(new String(payload));
            String type = jsonObject.getString("type");

            JSONObject payload_json = jsonObject.getJSONObject("payload");
            JSONArray devices = payload_json.getJSONArray("devices");

            for(int i=0;i < devices.length();i++) {
                JSONObject jsonNode = devices.getJSONObject(i);
                TXGatewaySubdev subdev = findSubdev(jsonNode.getString("product_id"),jsonNode.getString("device_name"));
                if(null == subdev) {
                    return  false;
                }
                if (type.equalsIgnoreCase("online")) {
                    String res = jsonNode.getString("result");
                    if (res.equals("0")) {
                        subdev.setSubdevStatus(Status.SUBDEV_STAT_ONLINE);
                    }
                } else if (type.equalsIgnoreCase("offline")) {
                    String res = jsonNode.getString("result");
                    if (res.equals("0")) {
                        subdev.setSubdevStatus(Status.SUBDEV_STAT_OFFLINE);
                    }
                } else if (type.equalsIgnoreCase("bind")) {
                    int res = jsonNode.getInt("result");
                    subdev.onSubDevBind(res);
                } else if (type.equalsIgnoreCase("unbind")) {
                    int res = jsonNode.getInt("result");
                    subdev.onSubDevUnbind(res);
                }
            }
        }catch (JSONException e) {
        }
        return true;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        TXLog.d(TAG, "message received " + topic);
        if (!consumeGwOperationMsg(topic, message)) {
            String [] productInfo = topic.split("/");
            String productId = productInfo[3];
            String devName = productInfo[4];

            if(this.mProductId.equals(productId) && this.mDeviceName.equals(devName)) {
                super.messageArrived(topic, message);
            } else {
                TXGatewaySubdev subdev= findSubdev(productId, devName);
                if(null != subdev) {
                    subdev.onMessageArrived(topic, message);
                } else {
                    TXLog.e(TAG, "Sub dev should be added! Product id:" + productId + ", Device Name:" + devName);
                }
            }
        }
    }

    @Override
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            TXLog.i(TAG, "The client is connecting. Connect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
            TXLog.i(TAG, "The client is already connected. Connect return directly.");
            return Status.OK;
        }

        this.mConnOptions = options;
        if (mConnOptions == null) {
            TXLog.e(TAG, "Connect options == null, will not connect.");
            return Status.PARAMETER_INVALID;
        }

        Long timestamp;
        if (options.isAutomaticReconnect()) {
            timestamp = (long) Integer.MAX_VALUE;
        } else {
            timestamp = System.currentTimeMillis() / 1000 + 600;
        }
        String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;

        mConnOptions.setUserName(userNameStr);

        if (mSecretKey != null && mSecretKey.length() != 0) {
            try {
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(), Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
                mConnOptions.setPassword(passWordStr.toCharArray());
            }
            catch (IllegalArgumentException e) {
                TXLog.d(TAG, "Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                TXLog.i(TAG, "onSuccess!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + mServerURI, null);
                // If the connection is established, subscribe the gateway operation topic
                String gwTopic = GW_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
                int qos = TXMqttConstants.QOS1;
                subscribe(gwTopic, qos, "Subscribe GATEWAY result topic");
                TXLog.d(TAG, "Connected, then subscribe the gateway result topic");
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                TXLog.e(TAG, exception, "onFailure!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString(), exception);
            }
        };

        if (mMqttClient == null) {
            try {
                mPingSender = new TXAlarmPingSender(mContext);
                mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
                mMqttClient.setCallback(this);
                mMqttClient.setBufferOpts(super.bufferOpts);
                mMqttClient.setManualAcks(false);
            } catch (Exception e) {
                TXLog.e(TAG, "new MqttClient failed", e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        try {
            TXLog.i(TAG, "Start connecting to %s", mServerURI);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
            mMqttClient.connect(mConnOptions, userContext, mActionListener);
        } catch (Exception e) {
            TXLog.e(TAG, "MqttClient connect failed", e);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
            return Status.ERROR;
        }
        return Status.OK;
    }

}
