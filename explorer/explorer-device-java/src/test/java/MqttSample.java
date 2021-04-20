import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import samples.data_template.DataTemplateSample;
import com.tencent.iot.explorer.device.java.utils.ReadFile;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynreg;
import com.tencent.iot.hub.device.java.core.dynreg.TXMqttDynregCallback;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttSample {
    private static final Logger LOG = LoggerFactory.getLogger(MqttSample.class);

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = "PRODUCT_ID";
    private static String mDevName = "DEVICE_NAME";
    private static String mDevPSK  = "DEVICE_PSK"; //若使用证书验证，设为null

    private static String mProductKey = "PRODUCT_KEY";             // Used for dynamic register
    private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mJsonFileName = "struct.json";

    private static DataTemplateSample mDataTemplateSample;

    private static void readDeviceInfoJson() {
        File file = new File(System.getProperty("user.dir") + "/explorer/explorer-device-java/src/test/resources/device_info.json");
        System.out.println(file.getAbsolutePath());
        if (file.exists()) {
            try {
                String s = ReadFile.readJsonFile(file.getAbsolutePath());
                JSONObject json = new JSONObject(s);
                mProductID = json.getString("PRODUCT_ID");
                mDevName = json.getString("DEVICE_NAME");
                mDevPSK = json.getString("DEVICE_PSK").length() == 0 ? null : json.getString("DEVICE_PSK");
                mDevCert = json.getString("DEVICE_CERT_FILE_NAME");
                mDevPriv = json.getString("DEVICE_PRIVATE_KEY_FILE_NAME");
                mProductKey = json.getString("PRODUCT_KEY");
                mJsonFileName = json.getString("TEMPLATE_JSON_FILE_NAME").length() == 0 ? "struct.json" : json.getString("TEMPLATE_JSON_FILE_NAME");
            } catch (JSONException t) {
                LOG.error("device_info.json file format is invalid!." + t);
            }
        } else{
            LOG.error("Cannot open device_info.json File.");
        }
    }

    private static void dynReg() {
        try {
            Thread.sleep(2000);
            LOG.debug("Test Dynamic");
            TXMqttDynreg dynreg = new TXMqttDynreg(mProductID, mProductKey, mDevName, new SelfMqttDynregCallback());
            if (dynreg.doDynamicRegister()) {
                LOG.debug("Dynamic Register OK!");
            } else {
                LOG.error("Dynamic Register failed!");
            }
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
            structJson.put("parBool", 0);      // 布尔型参数
            structJson.put("parInt", 0);      // 整数型参数
            structJson.put("parString", "string");  // 字符串参数
            structJson.put("patFloat", 0.0001); // 浮点型参数
            structJson.put("enum_param", 0);      // 枚举型参数
            structJson.put("timestamp_param", 1577871650);        // 时间型参数
            property.put("structPar", structJson);
            property.put("struct_param", structJson);   // 自定义结构体属性
            property.put("power_switch",0);  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("color",0);  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("brightness",0);  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("name","test");  // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性

            JSONArray arrInt = new JSONArray(); // 整数数组
            arrInt.put(1);
            arrInt.put(3);
            arrInt.put(5);
            arrInt.put(7);
            property.put("arrInt", arrInt);

            JSONArray arrStr = new JSONArray(); // 字符串数组
            arrStr.put("aaa");
            arrStr.put("bbb");
            arrStr.put("ccc");
            arrStr.put("");
            property.put("arrString", arrStr);

            JSONArray arrFloat = new JSONArray();  // 浮点数组
            arrFloat.put(5.001);
            arrFloat.put(0.003);
            arrFloat.put(0.004);
            arrFloat.put(0.007);
            property.put("arrFloat", arrFloat);

            JSONArray arrStruct = new JSONArray();   // 结构体数组
            for (int i = 0; i < 7; i++) {
                JSONObject structEleJson = new JSONObject();
                structEleJson.put("boolM", 0);      // 布尔型参数
                structEleJson.put("intM", 0);      // 整数型参数
                structEleJson.put("stringM", "string");  // 字符串参数
                structEleJson.put("floatM", 0.1); // 浮点型参数
                structEleJson.put("enumM", 0);      // 枚举型参数
                structEleJson.put("timeM", 1577871650);        // 时间型参数
                arrStruct.put(structEleJson);
            }

            property.put("arrStruct", arrStruct);

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

    private static void checkFirmware() {
        try {
            Thread.sleep(2000);
            mDataTemplateSample.checkFirmware();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        readDeviceInfoJson();

        dynReg();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // init connection
        mDataTemplateSample = new DataTemplateSample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, new SelfMqttActionCallBack(),
                mJsonFileName, new SelfDownStreamCallBack());
        mDataTemplateSample.connect();

        subscribeTopic();

        unSubscribeTopic();

        propertyReport();

        propertyGetStatus();

        propertyReportInfo();

        propertyClearControl();

        eventSinglePost();

        eventsPost();

        checkFirmware();

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
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
            LOG.debug(logInfo);
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

    /**
     * Callback for dynamic register
     */
    private static class SelfMqttDynregCallback extends TXMqttDynregCallback {

        @Override
        public void onGetDevicePSK(String devicePsk) {
            mDevPSK = devicePsk;
            String logInfo = String.format("Dynamic register OK! onGetDevicePSK, devicePSK[%s]", devicePsk);
            LOG.info(logInfo);
        }

        @Override
        public void onGetDeviceCert(String deviceCert, String devicePriv) {
//            mDevCert = deviceCert;   //这里获取的是证书内容字符串 创建对应ssl认证时可使用options.setSocketFactory(AsymcSslUtils.getSocketFactoryByStream(new ByteArrayInputStream(mDevCert.getBytes()), new ByteArrayInputStream(mDevPriv.getBytes())));方式，示例中使用的是读取本地文件路径的方式。
//            mDevPriv = devicePriv;   //这里获取的是秘钥内容字符串
            String logInfo = String.format("Dynamic register OK!onGetDeviceCert, deviceCert[%s] devicePriv[%s]", deviceCert, devicePriv);
            LOG.info(logInfo);
        }

        @Override
        public void onFailedDynreg(Throwable cause, String errMsg) {
            String logInfo = String.format("Dynamic register failed! onFailedDynreg, ErrMsg[%s]", cause.toString() + errMsg);
            LOG.error(logInfo);
        }

        @Override
        public void onFailedDynreg(Throwable cause) {
            String logInfo = String.format("Dynamic register failed! onFailedDynreg, ErrMsg[%s]", cause.toString());
            LOG.error(logInfo);
        }
    }
}