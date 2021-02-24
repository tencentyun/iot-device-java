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
    private static String mSubDev1DevicePSK  = "null"; //若使用证书验证，设为null

    private final static String mJsonFileName = "gateway.json";
    private static TXGatewayClient mConnection;

    private static String mSubDev2ProductId = "YOUR_SUB_DEV_PROD_ID";
    private static String mSubDev2DeviceName = "YOUR_SUB_DEV_NAME";
    private static String mSubDev2DevicePSK  = "null"; //若使用证书验证，设为null
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mDevCert = "";           // Cert String
    private static String mDevPriv = "";           // Priv String
    private static int pubCount = 0;
    private static final int testCnt = 100;

    private static void gatewayOffline() {
        try {
            Thread.sleep(2000);
            mGatewaySample.offline();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayBindSubdev(String productId, String deviceName, String devicePsk) {
        try {
            Thread.sleep(2000);
            mGatewaySample.gatewayBindSubdev(productId, deviceName, devicePsk);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayUnbindSubdev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.gatewayUnbindSubdev(productId, deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayAddSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.addSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayDelSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.delSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayOnlineSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.onlineSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewayOfflineSubDev(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            mGatewaySample.offlineSubDev(productId,deviceName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void gatewaySubDevPropertyReport(String productId, String deviceName) {
        try {
            Thread.sleep(2000);
            // 这里添加获取到的数据
            JSONObject property = new JSONObject();
            property.put("power_switch",1);
            property.put("color",1);
            property.put("brightness",100);
            property.put("name","test2");
            mGatewaySample.subDevPropertyReport(productId,deviceName,property,null);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        mGatewaySample = new GatewaySample(mBrokerURL, mProductID, mDevName, mDevPSK, mDevCert, mDevPriv, mJsonFileName, mSubDev1ProductId, mSubDev2ProductId);

        mGatewaySample.online();

        gatewayBindSubdev(mSubDev1ProductId,mSubDev1DeviceName,mSubDev1DevicePSK);

        gatewayAddSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayOnlineSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayOfflineSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayDelSubDev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayUnbindSubdev(mSubDev1ProductId,mSubDev1DeviceName);

        gatewayOffline();
    }

}
