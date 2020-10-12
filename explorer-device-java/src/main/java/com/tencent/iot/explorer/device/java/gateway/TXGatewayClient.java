package com.tencent.iot.explorer.device.java.gateway;



import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXAlarmPingSender;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import java.util.HashMap;

public class TXGatewayClient extends TXDataTemplateClient {
    private HashMap<String, TXGatewaySubdev> mSubdevs = new HashMap<String, TXGatewaySubdev>();
    private static final String GW_OPERATION_RES_PREFIX = "$gateway/operation/result/";
    private static final String GW_OPERATION_PREFIX = "$gateway/operation/";

    private static final Logger LOG = LoggerFactory.getLogger(TXGatewayClient.class);


    public TXGatewayClient( String serverURI, String productID, String deviceName, String secretKey, DisconnectedBufferOptions bufferOpts,
                           MqttClientPersistence clientPersistence, TXMqttActionCallBack callBack,
                           final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super( serverURI, productID, deviceName, secretKey, bufferOpts, clientPersistence, callBack, jsonFileName,downStreamCallBack);
        
    }

    /**
     *
     * @param productId
     * @param devName
     * @return null if not existed otherwise the subdev
     */
    public TXGatewaySubdev findSubdev(String productId, String devName) {
        LOG.debug("input product id is " + productId + ", input device name is " + devName);
        LOG.debug("The hashed information is " + mSubdevs);
        return mSubdevs.get(productId + devName);
    }

    /**
     * remove the subdev
     * @param productId
     * @param devName
     * @return
     */
    public synchronized TXGatewaySubdev removeSubdev(String productId, String devName) {
        if(null != findSubdev(productId, devName)) {
            return mSubdevs.remove(productId + devName);
        }
        return  null;
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
//            LOG.debug(TAG,"Sub dev already exits!");
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
    /**
     * set the status of the subdev
     * @param productId
     * @param devName
     * @param stat
     * @return the status of operation
     */
    public Status setSubdevStatus(String productId, String devName, Status stat) {
        TXGatewaySubdev subdev = findSubdev(productId, devName);
        if (subdev == null) {
            return Status.SUBDEV_STAT_NOT_EXIST;
        }
        subdev.setSubdevStatus(stat);
        return Status.OK;
    }

    /**
     * publish the offline message for the subdev
     * @param subProductID
     * @param subDeviceName
     * @return the result of operation
     */
    public Status subdevOffline(String subProductID, String subDeviceName) {
        LOG.debug("Try to find " + subProductID + " & " + subDeviceName);
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            LOG.debug("Cant find the subdev");
            return Status.SUBDEV_STAT_NOT_EXIST;
        } else if (subdev.getSubdevStatus() == Status.SUBDEV_STAT_OFFLINE) {
            LOG.debug("subdev has already offline!");
            return  Status.SUBDEV_STAT_OFFLINE;
        }

        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
        LOG.debug("set " + subProductID + " & " + subDeviceName + " to offline");

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "offline");
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
        LOG.debug("publish message " + message);
        return super.publish(topic, message, null);
}

    /**
     * publish the online message for the subdev
     * @param subProductID
     * @param subDeviceName
     * @return the result of operation
     */
    public Status subdevOnline(String subProductID, String subDeviceName) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if (subdev == null) {
            LOG.error("Cant find the subdev");
            return Status.SUBDEV_STAT_NOT_EXIST;
        } else if(subdev.getSubdevStatus() == Status.SUBDEV_STAT_ONLINE) {
            LOG.error("subdev has already online!");
            return  Status.SUBDEV_STAT_ONLINE;
        }
        String topic = GW_OPERATION_PREFIX + mProductId + "/" + mDeviceName;
        LOG.debug("set " + subProductID + " & " + subDeviceName + " to Online");

        // format the payload
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "online");
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
        LOG.debug("publish message " + message);
        return super.publish(topic, message, null);
    }

    /**
     * 订阅数据模板相关主题
     * @param topicId 主题ID
     * @param qos QOS等级
     * @return 发送请求成功时返回Status.OK;
     */
    public Status subscribeSubDevTopic(String subProductID, String subDeviceName, TXDataTemplateConstants.TemplateSubTopic topicId, final int qos) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if(null != subdev){
            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
                return  subdev.subscribeTemplateTopic(topicId, qos);
            } else {
                return  Status.SUBDEV_STAT_OFFLINE;
            }
        }
        return Status.SUBDEV_STAT_NOT_EXIST;
    }

    /**
     * 取消订阅数据模板相关主题
     * @param topicId 主题ID
     * @return 发送请求成功时返回Status.OK;
     */
    public Status unSubscribeSubDevTopic(String subProductID, String subDeviceName,TXDataTemplateConstants.TemplateSubTopic topicId) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if(null != subdev){
            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
                return  subdev.unSubscribeTemplateTopic(topicId);
            } else {
                return  Status.SUBDEV_STAT_OFFLINE;
            }
        }
        return Status.SUBDEV_STAT_NOT_EXIST;
    }

    /**
     * 属性上报
     * @param property 属性的json
     * @param metadata 属性的metadata，目前只包含各个属性对应的时间戳
     * @return 结果
     */
    public Status subDevPropertyReport(String subProductID, String subDeviceName,JSONObject property, JSONObject metadata) {
        TXGatewaySubdev subdev = findSubdev(subProductID, subDeviceName);
        if(null != subdev){
            if(Status.SUBDEV_STAT_ONLINE == subdev.getSubdevStatus()) {
                return  subdev.propertyReport(property, metadata);
            } else {
                return  Status.SUBDEV_STAT_OFFLINE;
            }
        }
        return Status.SUBDEV_STAT_NOT_EXIST;
    }
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
        LOG.debug("got gate operation messga " + topic + message);

        try {
            byte[] payload = message.getPayload();
            JSONObject jsonObject = new JSONObject(new String(payload));
            String type = jsonObject.getString("type");

            JSONObject payload_json = jsonObject.getJSONObject("payload");
            JSONArray devices = payload_json.getJSONArray("devices");

            for(int i=0;i < devices.length();i++) {
                JSONObject jsonNode = devices.getJSONObject(i);
                TXGatewaySubdev subdev = findSubdev(jsonNode.getString("product_id"),jsonNode.getString("device_name"));
                System.out.println("++++++++++"+jsonNode.getString("product_id")+jsonNode.getString("device_name")+"+++++++++++");
                if(null == subdev) {
                    return  false;
                }
                if (type.equalsIgnoreCase("online")) {
                    System.out.println("++++++++++"+jsonNode);

                    System.out.println("++++++++++"+jsonNode.getInt("result"));
                    int res = jsonNode.getInt("result");
                    if (res==0) {
                        subdev.setSubdevStatus(Status.SUBDEV_STAT_ONLINE);
                    }
                } else if (type.equalsIgnoreCase("offline")) {
                    int res = jsonNode.getInt("result");
                    if (res==0) {
                        subdev.setSubdevStatus(Status.SUBDEV_STAT_OFFLINE);
                    }
                }
            }
        }catch (JSONException e) {
        }
        return true;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        LOG.debug("message received " + topic);
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
                    LOG.error("Sub dev should be added! Product id:" + productId + ", Device Name:" + devName);
                }
            }
        }
    }

    @Override
    public synchronized Status connect(MqttConnectOptions options, Object userContext) {
        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnecting)) {
            LOG.info("The client is connecting. Connect return directly.");
            return Status.MQTT_CONNECT_IN_PROGRESS;
        }

        if (mConnectStatus.equals(TXMqttConstants.ConnectStatus.kConnected)) {
            LOG.info("The client is already connected. Connect return directly.");
            return Status.OK;
        }

        this.mConnOptions = options;
        if (mConnOptions == null) {
            LOG.error("Connect options == null, will not connect.");
            return Status.PARAMETER_INVALID;
        }

        Long timestamp = System.currentTimeMillis()/1000 + 600;
        String userNameStr = mUserName + ";" + getConnectId() + ";" + timestamp;

        mConnOptions.setUserName(userNameStr);

        if (mSecretKey != null && mSecretKey.length() != 0) {
            try {
                String passWordStr = HmacSha256.getSignature(userNameStr.getBytes(), Base64.decode(mSecretKey, Base64.DEFAULT)) + ";hmacsha256";
                mConnOptions.setPassword(passWordStr.toCharArray());
            }
            catch (IllegalArgumentException e) {
                LOG.debug("Failed to set password");
            }
        }

        mConnOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                LOG.info("onSuccess!");
                setConnectingState(TXMqttConstants.ConnectStatus.kConnected);
                mActionCallBack.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + mServerURI);
                // If the connection is established, subscribe the gateway operation topic
                String gwTopic = GW_OPERATION_RES_PREFIX + mProductId + "/" + mDeviceName;
                int qos = TXMqttConstants.QOS1;
                subscribe(gwTopic, qos, "Subscribe GATEWAY result topic");
                LOG.debug("Connected, then subscribe the gateway result topic");
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                LOG.error("{}", "onFailure!", exception);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                mActionCallBack.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString());
            }
        };

        if (mMqttClient == null) {
            try {
                mPingSender = new TXAlarmPingSender();
                mMqttClient = new MqttAsyncClient(mServerURI, mClientId, mMqttPersist, mPingSender);
                mMqttClient.setCallback(this);
                mMqttClient.setBufferOpts(super.bufferOpts);
                mMqttClient.setManualAcks(false);
            } catch (Exception e) {
                LOG.error("{}", "new MqttClient failed", e);
                setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
                return Status.ERROR;
            }
        }

        try {
            LOG.info("Start connecting to {}", mServerURI);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnecting);
            mMqttClient.connect(mConnOptions, userContext, mActionListener);
        } catch (Exception e) {
            LOG.error("{}", "MqttClient connect failed", e);
            setConnectingState(TXMqttConstants.ConnectStatus.kConnectFailed);
            return Status.ERROR;
        }
        return Status.OK;
    }

}
