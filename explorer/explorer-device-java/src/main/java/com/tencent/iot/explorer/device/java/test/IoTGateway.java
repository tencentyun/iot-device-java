package com.tencent.iot.explorer.device.java.test;


import com.tencent.iot.explorer.device.java.gateway.TXGatewayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.iot.explorer.device.java.server.samples.gateway.GatewaySample;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.*;

public class IoTGateway {
    private static final Logger LOG = LoggerFactory.getLogger(IoTGateway.class);
    private static final String TAG = "TXGatewaySample";
    private static GatewaySample mGatewaySample;
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = "YOUR_PRODUCT_ID";
    private static String mDevName = "YOUR_DEVICE_NAME";
    private static String mDevPSK  = "null"; //若使用证书验证，设为null
    private static String mSubDev1ProductId = "YOUR_SUB_DEV_PROD_ID";
    private static String mSubDev1DeviceName = "YOUR_SUB_DEV_NAME";

    private final static String mJsonFileName = "gateway.json";
    private static TXGatewayClient mConnection;

    private static String mSubDev2ProductId = "YOUR_SUB_DEV_NAME";
    private static String mSubDev2DeviceName = "YOUR_SUB_DEV_NAME";
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mDevCert = "";           // Cert String
    private static String mDevPriv = "";           // Priv String
    private static int pubCount = 0;
    private static final int testCnt = 100;

    public static void main(String[] args) {

        mGatewaySample = new GatewaySample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mJsonFileName, mSubDev1ProductId, mSubDev2ProductId);

        mGatewaySample.online();

        try {
            Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//        mGatewaySample.delSubDev(mSubDev1ProductId,mSubDev1DeviceName);

          mGatewaySample.addSubDev(mSubDev1ProductId,mSubDev1DeviceName);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mGatewaySample.onlineSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mGatewaySample.subscribeSubDevTopic(mSubDev1ProductId,mSubDev1DeviceName,PROPERTY_DOWN_STREAM_TOPIC,0);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

 //        这里添加获取到的数据
        JSONObject property = new JSONObject();
        property.put("power_switch",1);
        property.put("color",1);
        property.put("brightness",100);
        property.put("name","test2");
        mGatewaySample.subDevPropertyReport(mSubDev1ProductId,mSubDev1DeviceName,property,null);


//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        mGatewaySample.offlineSubDev(mSubDev1ProductId,mSubDev1DeviceName);
//
////        mGatewaySample.publish(mSubDev1ProductId,mSubDev1DeviceName);
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        mGatewaySample.delSubDev(mSubDev1ProductId,mSubDev1DeviceName);

    }

}
