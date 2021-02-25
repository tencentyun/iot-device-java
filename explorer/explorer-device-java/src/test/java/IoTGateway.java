import com.tencent.iot.explorer.device.java.gateway.TXGatewayClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.iot.explorer.device.java.server.samples.gateway.GatewaySample;
import com.tencent.iot.explorer.device.java.utils.ReadFile;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.iot.explorer.device.java.data_template.TXDataTemplateConstants.TemplateSubTopic.*;

public class IoTGateway {
    private static final Logger LOG = LoggerFactory.getLogger(IoTGateway.class);
    private static final String TAG = "TXGatewaySample";
    private static GatewaySample mGatewaySample;
    private static String mBrokerURL = null;  //传入null，即使用腾讯云物联网通信默认地址 "${ProductId}.iotcloud.tencentdevices.com:8883"  https://cloud.tencent.com/document/product/634/32546
    private static String mProductID = "PRODUCT_ID";
    private static String mDevName = "DEVICE_NAME";
    private static String mDevPSK  = "DEVICE_PSK"; //若使用证书验证，设为null
    private static String mDevCert = "DEVICE_CERT_FILE_NAME";           // Device Cert File Name
    private static String mDevPriv = "DEVICE_PRIVATE_KEY_FILE_NAME";            // Device Private Key File Name
    private static AtomicInteger requestID = new AtomicInteger(0);
    private static String mJsonFileName = "TEMPLATE_JSON_FILE_NAME";

    private static String mSubDev1ProductId = "SUB_PRODUCT_ID";
    private static String mSubDev1DeviceName = "SUB_DEV_NAME";
    private static String mSubDev1DevicePSK  = "SUB_DEV_PSK";

    private static String mSubDev2ProductId = "SUB_PRODUCT_ID2";
    private static String mSubDev2DeviceName = "SUB_DEV_NAME2";
    private static String mSubDev2DevicePSK  = "SUB_DEV_PSK2";

    private static TXGatewayClient mConnection;
    private static int pubCount = 0;
    private static final int testCnt = 100;

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
                mJsonFileName = json.getString("TEMPLATE_JSON_FILE_NAME").length() == 0 ? "gateway.json" : json.getString("TEMPLATE_JSON_FILE_NAME");
                mSubDev1ProductId = json.getString("SUB_PRODUCT_ID");
                mSubDev1DeviceName = json.getString("SUB_DEV_NAME");
                mSubDev1DevicePSK = json.getString("SUB_DEV_PSK");
                mSubDev2ProductId = json.getString("SUB_PRODUCT_ID2");
                mSubDev2DeviceName = json.getString("SUB_DEV_NAME2");
                mSubDev2DevicePSK = json.getString("SUB_DEV_PSK2");
            } catch (JSONException t) {
                LOG.error("device_info.json file format is invalid!." + t);
            }
        } else{
            LOG.error("Cannot open device_info.json File.");
        }
    }

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
        readDeviceInfoJson();

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
