package com.tencent.iot.explorer.device.java.core.mqtt;

import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import samples.data_template.DataTemplateSample;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.unit.test.BuildConfig;

import static org.junit.Assert.assertSame;

public class TestMqttSample {
    private static final Logger LOG = LoggerFactory.getLogger(TestMqttSample.class);

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTMQTTSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTMQTTSAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTMQTTSAMPLE_DEVICE_PSK; //若使用证书验证，设为null

    private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mJsonFileName = "struct.json";

    private static DataTemplateSample mDataTemplateSample;

    private static void connect() {
        try {
            Thread.sleep(2000);
            // init connection
            mDataTemplateSample = new DataTemplateSample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, new SelfMqttActionCallBack(),
                    mJsonFileName, new SelfDownStreamCallBack());
            mDataTemplateSample.connect();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void disconnect() {
        try {
            Thread.sleep(2000);
            mDataTemplateSample.disconnect();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void subscribeTopic() {
        try {
            Thread.sleep(2000);
            mDataTemplateSample.subscribeTopic();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void unSubscribeTopic() {
        try {
            Thread.sleep(2000);
            mDataTemplateSample.unSubscribeTopic();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void propertyReport() {
        try {
            Thread.sleep(2000);
            JSONObject property = new JSONObject();
            JSONObject structJson = new JSONObject();
            structJson.put("bool_param", 0);      // 布尔型参数
            structJson.put("int_param", 0);      // 整数型参数
            structJson.put("string_param", "string");  // 字符串参数
            structJson.put("float_param", 0.0001); // 浮点型参数
            structJson.put("enum_param", 0);      // 枚举型参数
            structJson.put("timestamp_param", 1577871650);        // 时间型参数
            property.put("struct_param", structJson);   // 自定义结构体属性
            property.put("power_switch",0);  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("color",0);  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("brightness",0);  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("name","test");  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            if(Status.OK != mDataTemplateSample.propertyReport(property, null)) {
                LOG.error("property report failed!");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void propertyGetStatus() {
        try {
            Thread.sleep(2000);

            //get status
            if(Status.OK != mDataTemplateSample.propertyGetStatus("report", false)) {
                LOG.error("property get report status failed!");
            }

            if(Status.OK != mDataTemplateSample.propertyGetStatus("control", false)) {
                LOG.error("property get control status failed!");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void propertyReportInfo() {
        try {
            Thread.sleep(2000);
            //report info
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
                LOG.error("Construct params failed!");
                return;
            }
            if(Status.OK != mDataTemplateSample.propertyReportInfo(params)) {
                LOG.error("property report failed!");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void propertyClearControl() {
        try {
            Thread.sleep(2000);
            //clear control
            if(Status.OK !=  mDataTemplateSample.propertyClearControl()){
                LOG.error("clear control failed!");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void eventSinglePost() {
        try {
            Thread.sleep(2000);
            String eventId = "status_report";
            String type = "info";
            JSONObject params = new JSONObject();
            try {
                params.put("status",0);
                params.put("message","");
            } catch (JSONException e) {
                LOG.error("Construct params failed!");
            }
            if(Status.OK != mDataTemplateSample.eventSinglePost(eventId, type, params)){
                LOG.error("single event post failed!");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void eventsPost() {
        try {
            Thread.sleep(2000);
            JSONArray events = new JSONArray();

            //event:status_report
            try {
                JSONObject event = new JSONObject();
                event.put("eventId","status_report");
                event.put("type", "info");
                event.put("timestamp", System.currentTimeMillis());

                JSONObject params = new JSONObject();
                params.put("status",0);
                params.put("message","");

                event.put("params", params);

                events.put(event);
            } catch (JSONException e) {
                LOG.error("Construct params failed!");
                return;
            }

            //event:low_voltage
            try {
                JSONObject event = new JSONObject();
                event.put("eventId","low_voltage");
                event.put("type", "alert");
                event.put("timestamp", System.currentTimeMillis());

                JSONObject params = new JSONObject();
                params.put("voltage",1.000000f);

                event.put("params", params);

                events.put(event);
            } catch (JSONException e) {
                LOG.error("Construct params failed!");
                return;
            }

            //event:hardware_fault
            try {
                JSONObject event = new JSONObject();
                event.put("eventId","hardware_fault");
                event.put("type", "fault");
                event.put("timestamp", System.currentTimeMillis());

                JSONObject params = new JSONObject();
                params.put("name","");
                params.put("error_code",1);

                event.put("params", params);

                events.put(event);
            } catch (JSONException e) {
                LOG.error("Construct params failed!");
                return;
            }

            if(Status.OK != mDataTemplateSample.eventsPost(events)){
                LOG.error("events post failed!");
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        connect();

        subscribeTopic();

        unSubscribeTopic();

        propertyReport();

        propertyGetStatus();

        propertyReportInfo();

        propertyClearControl();

        eventSinglePost();

        eventsPost();

        disconnect();
    }

    public static class SelfMqttActionCallBack extends TXMqttActionCallBack {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            LOG.info(logInfo);
            unlock();
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            LOG.info(logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            LOG.info(logInfo);
            unlock();
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            LOG.debug(logInfo);
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            if (Status.ERROR == status) {
                LOG.error(logInfo);
            } else {
                LOG.debug(logInfo);
            }
            if (Arrays.toString(asyncActionToken.getTopics()).contains("thing/down/property") && userContextInfo.contains("subscribeTopic")) {
                unlock();
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
            LOG.debug(logInfo);
            if (Arrays.toString(asyncActionToken.getTopics()).contains("thing/down/property") && userContextInfo.contains("subscribeTopic")) {
                unlock();
            }
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
            LOG.debug(logInfo);
            if (message.toString().contains("report_reply") || message.toString().contains("get_status_reply") || message.toString().contains("report_info_reply") || message.toString().contains("clear_control_reply") || message.toString().contains("event_reply") || message.toString().contains("events_reply")) {
                unlock();
            }
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
                LOG.error("Construct params failed!");
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

    /** ============================================================================== Unit Test ============================================================================== **/

    private static Object mLock = new Object(); // 同步锁
    private static int mCount = 0; // 加解锁条件
    private static boolean mUnitTest = false;

    private static void lock() {
        synchronized (mLock) {
            mCount = 1;  // 设置锁条件
            while (mCount > 0) {
                try {
                    mLock.wait(); // 等待唤醒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void unlock() {
        if (mUnitTest) {
            synchronized (mLock) {
                mCount = 0;
                mLock.notifyAll(); // 回调执行完毕，唤醒主线程
            }
        }
    }

    @Test
    public void testMqttConnect() {
        mUnitTest = true;
        LogManager.resetConfiguration();
        LOG.isDebugEnabled();
        PropertyConfigurator.configure(TestMqttSample.class.getResource("/log4j.properties"));

        connect();
        LOG.debug("after connect");

        subscribeTopic();
        lock();
        LOG.debug("after subscribe");

//        propertyReport();
//        lock();
//        LOG.debug("after propertyReport");

        propertyGetStatus();
        lock();
        LOG.debug("after propertyGetStatus");

        propertyReportInfo();
        lock();
        LOG.debug("after propertyReportInfo");

        propertyClearControl();
        lock();
        LOG.debug("after propertyClearControl");

//        eventSinglePost();
//        lock();
//        LOG.debug("after eventSinglePost");
//
//        eventsPost();
//        lock();
//        LOG.debug("after eventsPost");

        unSubscribeTopic();
        lock();
        LOG.debug("after unSubscribe");

        disconnect();
        lock();
        LOG.debug("after disconnect");
        assertSame(mDataTemplateSample.getConnectStatus(), TXMqttConstants.ConnectStatus.kDisconnected);
    }
}