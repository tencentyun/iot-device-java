package com.tencent.iot.explorer.device.android;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateClient;
import com.tencent.iot.explorer.device.android.utils.AsymcSslUtils;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.explorer.device.java.mqtt.TXMqttRequest;
import com.tencent.iot.hub.device.java.core.common.Status;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttActionCallBack;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

import explorer.unit.test.BuildConfig;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.ACTION_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.EVENT_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.PROPERTY_DOWN_STREAM_TOPIC;
import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.SERVICE_DOWN_STREAM_TOPIC;
import static java.lang.Thread.sleep;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@Config(sdk = 28)
@RunWith(AndroidJUnit4.class)
public class MqttSampleTest {

    private static final String TAG = MqttSampleTest.class.getSimpleName();

    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = BuildConfig.TESTMQTTSAMPLE_PRODUCT_ID;
    private static String mDevName = BuildConfig.TESTMQTTSAMPLE_DEVICE_NAME;
    private static String mDevPSK  = BuildConfig.TESTMQTTSAMPLE_DEVICE_PSK; //若使用证书验证，设为null
    private static String mJsonFileName = "struct.json";

    private static String mDevCertName = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevKeyName = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name

    /**
     * 请求ID
     */
    private static AtomicInteger requestID = new AtomicInteger(0);

    @Test
    public void testMqttConnect() {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TXMqttActionCallBack mqttActionCallBack = mock(TXMqttActionCallBack.class);
        TXDataTemplateDownStreamCallBack dataTemplateDownStreamCallBack = mock(TXDataTemplateDownStreamCallBack.class);
        TXDataTemplateClient mMqttConnection = new TXDataTemplateClient(context, mBrokerURL, mProductID, mDevName, mDevPSK,null,null, mqttActionCallBack,
                mJsonFileName, dataTemplateDownStreamCallBack);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(8);
        options.setKeepAliveInterval(240);
        options.setAutomaticReconnect(true);

        if (mDevPSK != null && mDevPSK.length() != 0){
            Log.i(TAG, "Using PSK");
//            options.setSocketFactory(AsymcSslUtils.getSocketFactory());   如果您使用的是3.3.0及以下版本的 explorer-device-android sdk，由于密钥认证默认配置的ssl://的url，请添加此句setSocketFactory配置。
        } else {
            Log.i(TAG, "Using cert assets file");
            options.setSocketFactory(AsymcSslUtils.getSocketFactoryByAssetsFile(context, mDevCertName, mDevKeyName));
        }

        TXMqttRequest mqttRequest1 = new TXMqttRequest("connect", requestID.getAndIncrement());
        mMqttConnection.connect(options, mqttRequest1);

        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mMqttConnection.setBufferOpts(bufferOptions);

        verify(mqttActionCallBack, timeout(2000).times(1)).onConnectCompleted(Matchers.eq(Status.OK), Matchers.eq(false), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(Status.OK != mMqttConnection.subscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC, 0)){
            Log.d(TAG, "subscribeTopic: subscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC, 0)){
            Log.d(TAG, "subscribeTopic: subscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC, 0)){
            Log.d(TAG, "subscribeTopic: subscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.subscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC, 0)){
            Log.d(TAG, "subscribeTopic: subscribe service down stream topic failed!");
        }

        verify(mqttActionCallBack, timeout(2000).times(1)).onSubscribeCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JSONObject property = new JSONObject();
        try {
            JSONObject structJson = new JSONObject();
            structJson.put("bool_param", 1);                    // 布尔类型
            structJson.put("int_param", 10);                    // 整数类型
            structJson.put("str_param", "testStrAndroid");      // 字符串类型
            structJson.put("float_param", 2.1001);              // 浮点类型
            structJson.put("enum_param", 1);                    // 枚举类型
            structJson.put("time_param", 1577871650);           // 时间戳类型
            property.put("struct", structJson);   // 自定义结构体属性

            property.put("power_switch",0);     // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("color",0);            // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("brightness",0);       // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性
            property.put("name","test");        // 创建产品时，选择产品品类为智能城市-公共事业-路灯照明，数据模板中系统推荐的标准功能属性

            JSONArray arrInt = new JSONArray();  // 整数数组
            arrInt.put(1);
            arrInt.put(3);
            arrInt.put(5);
            arrInt.put(7);
            property.put("arrInt", arrInt);

            JSONArray arrStr = new JSONArray();  // 字符串数组
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

            JSONArray arrStruct = new JSONArray();  // 结构体数组
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
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if(Status.OK != mMqttConnection.propertyReport(property, null)) {
            Log.e(TAG,"property report failed!");
        }
//        verify(mqttActionCallBack, timeout(2000).times(1)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //get status
        if(Status.OK != mMqttConnection.propertyGetStatus("report", false)) {
            Log.e(TAG, "property get report status failed!");
        }

        if(Status.OK != mMqttConnection.propertyGetStatus("control", false)) {
            Log.e(TAG, "property get control status failed!");
        }

        verify(mqttActionCallBack, timeout(2000).times(2)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
            Log.e(TAG,"Construct params failed!");
            return;
        }
        if(Status.OK != mMqttConnection.propertyReportInfo(params)) {
            Log.e(TAG,"property report failed!");
        }

        verify(mqttActionCallBack, timeout(2000).times(3)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //clear control
        if(Status.OK !=  mMqttConnection.propertyClearControl()){
            Log.e(TAG,"clear control failed!");
        }

        verify(mqttActionCallBack, timeout(2000).times(4)).onPublishCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(PROPERTY_DOWN_STREAM_TOPIC)){
            Log.d(TAG, "subscribeTopic: unSubscribe property down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(EVENT_DOWN_STREAM_TOPIC)){
            Log.d(TAG, "subscribeTopic: unSubscribe event down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(ACTION_DOWN_STREAM_TOPIC)){
            Log.d(TAG, "subscribeTopic: unSubscribe action down stream topic failed!");
        }
        if(Status.OK != mMqttConnection.unSubscribeTemplateTopic(SERVICE_DOWN_STREAM_TOPIC)){
            Log.d(TAG, "subscribeTopic: unSubscribe service down stream topic failed!");
        }

        verify(mqttActionCallBack, timeout(2000).times(4)).onUnSubscribeCompleted(Matchers.eq(Status.OK), (IMqttToken) Mockito.any(), Mockito.any(), Mockito.anyString());

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TXMqttRequest mqttRequest2 = new TXMqttRequest("disconnect", requestID.getAndIncrement());
        mMqttConnection.disConnect(mqttRequest2);

        verify(mqttActionCallBack, timeout(2000).times(1)).onDisconnectCompleted(Matchers.eq(Status.OK), Mockito.any(), Mockito.anyString());
    }
}