package com.tencent.iot.explorer.device.java.core.ota;

import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConstants;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import explorer.unit.test.BuildConfig;
import com.tencent.iot.explorer.device.java.core.data_template.DataTemplateSample;
import com.tencent.iot.hub.device.java.utils.Loggor;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Device OTA sample
 */
public class OTASampleTest {

    private static final Logger LOG = LoggerFactory.getLogger(OTASampleTest.class);
    private static final String TAG = OTASampleTest.class.getSimpleName();
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTOTASAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTOTASAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTOTASAMPLE_DEVICE_PSK; //若使用证书验证，设为null

    private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mJsonFileName = "struct.json";
    private static String mJsonFilePath = System.getProperty("user.dir") + "/src/test/resources/";

    private static DataTemplateSample mDataTemplateSample;

    static {
        Loggor.setLogger(LOG);
    }

    private static void connect() {
        // init connection
        mDataTemplateSample = new DataTemplateSample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, new SelfMqttActionCallBack(),
                mJsonFileName, mJsonFilePath, new SelfDownStreamCallBack());
        mDataTemplateSample.connect();
    }

    private static void checkFirmware() {
        mDataTemplateSample.checkFirmware();
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
            Loggor.info(TAG, logInfo);
            unlock();
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            Loggor.info(TAG, logInfo);
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo, msg);
            Loggor.info(TAG, logInfo);
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof TXMqttRequest) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onPublishCompleted, status[%s], topics[%s],  userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
            Loggor.debug(TAG, logInfo);
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
                Loggor.error(TAG, logInfo);
            } else {
                Loggor.debug(TAG, logInfo);
                if (Arrays.toString(asyncActionToken.getTopics()).contains("ota/update/")){   // 订阅ota相关的topic成功
                    otaSubscribeTopicSuccess = true;
                    unlock();
                }
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
            Loggor.debug(TAG, logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive message, topic[%s], message[%s]", topic, message.toString());
            Loggor.debug(TAG, logInfo);
        }
    }

    /**
     * 实现下行消息处理的回调接口
     */
    private static class SelfDownStreamCallBack extends TXDataTemplateDownStreamCallBack {
        @Override
        public void onReplyCallBack(String replyMsg) {
            //可根据自己需求进行处理属性上报以及事件的回复，根据需求填写
            Loggor.debug(TAG, "reply received : " + replyMsg);
        }

        @Override
        public void onGetStatusReplyCallBack(JSONObject data) {
            //可根据自己需求进行处理状态和控制信息的获取结果
            Loggor.debug(TAG, "event down stream message received : " + data);
        }

        @Override
        public JSONObject onControlCallBack(JSONObject msg) {
            Loggor.debug(TAG, "control down stream message received : " + msg);
            //do something

            //output
            try {
                JSONObject result = new JSONObject();
                result.put("code",0);
                result.put("status", "some message wher errorsome message when error");
                return result;
            } catch (JSONException e) {
                Loggor.error(TAG, "Construct params failed!");
                return null;
            }
        }

        @Override
        public  JSONObject onActionCallBack(String actionId, JSONObject params){
            Loggor.debug(TAG, String.format("action [%s] received, input:%s", actionId, params));
            //do something based action id and input
            if(actionId.equals("blink")) {
                try {
                    Iterator<String> it = params.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        Loggor.debug(TAG, String.format("Input parameter[%s]:%s", key, params.get(key)));
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

        @Override
        public void onUnbindDeviceCallBack(String msg) {
            //可根据自己需求进行用户删除设备的通知消息处理的回复，根据需求填写
            Loggor.debug(TAG, "unbind device received : " + msg);
        }

        @Override
        public void onBindDeviceCallBack(String msg) {
            //可根据自己需求进行用户绑定设备的通知消息处理的回复，根据需求填写
            Loggor.debug(TAG, "bind device received : " + msg);
        }
    }

    /** ============================================================================== Unit Test ============================================================================== **/

    private static final int COUNT = 1;
    private static final int TIMEOUT = 3000;
    private static CountDownLatch latch = new CountDownLatch(COUNT);
    private static boolean otaSubscribeTopicSuccess = false;

    private static void lock() {
        latch = new CountDownLatch(COUNT);
        try {
            latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void unlock() {
        latch.countDown();// 回调执行完毕，唤醒主线程
    }

    @Test
    public void testOTA() {
        // Loggor.saveLogs("explorer/explorer-device-java.log"); //保存日志到文件
        connect();
        lock();
        assertSame(mDataTemplateSample.getConnectStatus(), TXMqttConstants.ConnectStatus.kConnected);
        Loggor.debug(TAG, "after connect");

        checkFirmware();
        lock();
        assertTrue(otaSubscribeTopicSuccess);
        Loggor.debug(TAG, "checkFirmware subscribe ota");

    }
}
