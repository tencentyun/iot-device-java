package com.tencent.iot.explorer.device.java.test;

import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.gateway.TXGatewayClient;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.iot.explorer.device.java.server.samples.data_template.DataTemplateSample;
import com.tencent.iot.explorer.device.java.server.samples.gateway.ProductAirconditioner;
import com.tencent.iot.explorer.device.java.server.samples.gateway.ProductLight;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class IoTDataTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(IoTDataTemplate.class);
    
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = "YOUR_PRODUCT_ID";
    private static String mDevName = "YOUR_DEVICE_NAME";
    private static String mDevPSK  = "null"; //若使用证书验证，设为null
    private static String mSubDev1ProductId = "YOUR_LIGHT_PRODUCT_ID";
    private static String mSubDev1DeviceName = "YOUR_LIGHT_DEVICE_NAME";
    private static TXGatewayClient mConnection;
    private String mSubDev2ProductId = "YOUR_AIRCONDITIONER_PRODUCT_ID";
    private String mSubDev2DeviceName = "YOUR_AIRCONDITIONER_DEVICE_NAME";

    private String mDevCert = "";           // Cert String
    private String mDevPriv = "";           // Priv String
    private static AtomicInteger requestID = new AtomicInteger(0);
    private ProductLight mSubDev1 = null;
    private ProductAirconditioner mSubDev2 = null;
    private final static String mJsonFileName = "网关.json";

    private static JSONObject jsonObject = new JSONObject();
    private static int pubCount = 0;
    private static final int testCnt = 100;
    private static DataTemplateSample mDataTemplateSample;
    private static MqttConnectOptions options;


    /**
     * MQTT连接实例
     */
    private static void dbgPrint(String s) {
        System.out.println(s);
    }
    public static void main(String[] args) {
        try {
            jsonObject.put("color", "0");
            jsonObject.put("brightness", "100");
            jsonObject.put("name", "test");

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        mDataTemplateSample = new DataTemplateSample(mBrokerURL, mProductID, mDevName, mDevPSK,null,null, new callBack(),
                mJsonFileName, new SelfDownStreamCallBack());
        mDataTemplateSample.connect();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MqttMessage message = new MqttMessage();
        // 这里添加获取到的数据
        message.setPayload(jsonObject.toString().getBytes());
        JSONObject property = new JSONObject();
            property.put("power_switch",1);
            property.put("color",1);
            property.put("brightness",100);
            property.put("name","test2");

        mDataTemplateSample.propertyReport(property,null);



    }

    public static class callBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            // TODO Auto-generated method stub
            String topic = String.format("%s/%s/%s", mProductID, mDevName,"data");
            System.out.println("MQTT Connect完成回调" + status.toString());

        }

        @Override
        public void onConnectionLost(Throwable cause) {
            // TODO Auto-generated method stub
            LOG.info("MQTT连接断开回调连接断开原因"+cause);

        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            // TODO Auto-generated method stub
            LOG.info("MQTT DISCONNECT –断开连接");
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],   errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()),  errMsg);
            dbgPrint(logInfo);
            LOG.info("onDisconnectCompleted111111111111111");
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";

            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            dbgPrint(logInfo);

            MqttMessage message = new MqttMessage();
            // 这里添加获取到的数据
            message.setPayload(jsonObject.toString().getBytes());
            message.setQos(0);

            String topic = String.format("%s/%s/%s", mProductID, mDevName,"data");


        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
            dbgPrint(logInfo);
            MqttMessage msg = new MqttMessage();
            pubCount += 1;

            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (pubCount > testCnt) {
                return;
            }

            // 这里添加获取到的数据
            msg.setPayload(jsonObject.toString().getBytes());
            msg.setQos(0);

        }
    }

    /**
     * 实现下行消息处理的回调接口
     */
    private static class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            LOG.debug("reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //可根据自己需求进行处理状态和控制信息的获取结果
            LOG.debug("event down stream message received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            LOG.debug("control down stream message received : " + msg);
            //do something

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                dbgPrint("Construct params failed!");
                return null;
            }
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            LOG.debug("action [{}] received, input:" + params, actionId);
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    Iterator<String> it = params.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        LOG.debug("Input parameter[{}]:" + params.get(key), key);
                    }
                    //construct result
                    JSONObject result = new JSONObject();
                    result.put("code",0);
                    result.put("status", "some message wher errorsome message when error");

                    // response based on output
                    JSONObject response = new JSONObject();
                    response.put("result", 0);

                    result.put("response", response);
                    return result;
                } catch (JSONException e) {
                    return null;
                }
            } else if (actionId.equals("YOUR ACTION")) {
                //do your action
            }
            return null;
        }
    }

}
