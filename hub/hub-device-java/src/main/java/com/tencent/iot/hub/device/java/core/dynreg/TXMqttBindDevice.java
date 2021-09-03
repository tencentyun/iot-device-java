package com.tencent.iot.hub.device.java.core.dynreg;

import com.tencent.iot.hub.device.java.core.util.AsymcSslUtils;
import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.utils.Loggor;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 绑定设备类
 */
public class TXMqttBindDevice {
    private static final String TAG = TXMqttBindDevice.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TXMqttDynreg.class);
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final String mDefaultUrl ="http://ap-guangzhou.gateway.tencentdevices.com/register/tiddevice";


    private final String mProductId;
    private final String mDeviceName;
    private final String mTid;
    private final String mBindDeviceUrl;
    private final String mPEMPubKey;
    private final TXMqttBindDeviceCallback mCallback;

    static { Loggor.setLogger(logger); }

    /**
     * 构造函数
     *
     * @param url  绑定设备的 url
     * @param productId  产品 ID
     * @param deviceName 设备名
     * @param tid 设备 ID
     * @param pubKey 公钥
     * @param callback 操作结果回调 {@link TXMqttBindDeviceCallback}
     */
    public TXMqttBindDevice(String url, String productId, String deviceName, String tid,
                            String pubKey, TXMqttBindDeviceCallback callback) {
        this.mBindDeviceUrl = url;
        this.mProductId = productId;
        this.mDeviceName = deviceName;
        this.mTid = tid;
        this.mPEMPubKey = pubKey;
        this.mCallback = callback;
    }

    /**
     * 构造函数
     *
     * @param productId  产品 ID
     * @param deviceName 设备名
     * @param tid 设备 ID
     * @param pubKey 公钥
     * @param callback 操作结果回调 {@link TXMqttBindDeviceCallback}
     */
    public TXMqttBindDevice(String productId, String deviceName, String tid, String pubKey,
                            TXMqttBindDeviceCallback callback) {
        this(mDefaultUrl, productId, deviceName, tid, pubKey, callback);
    }

    private class HttpPostThread extends Thread {
        private String postData;
        private String url;

        HttpPostThread(String upStr, String upUrl) {
            this.postData = upStr;
            this.url = upUrl;
        }

        public void run() {
            this.setName("tencent-" + HttpPostThread.class.getSimpleName().toLowerCase());

            StringBuffer serverRsp = new StringBuffer();
            try {
                URL url = new URL(mBindDeviceUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(2000);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(postData);
                os.flush();
                os.close();

                int rc = conn.getResponseCode();
                String line;
                if (rc == 200) {
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        serverRsp.append(line);
                    }
                    if (mCallback != null) mCallback.onBindSuccess(serverRsp.toString());
                    conn.disconnect();
                } else {
                    Loggor.error(TAG, "Get error return code "+ rc);
                    conn.disconnect();
                    if (mCallback != null) {
                        mCallback.onBindFailed(new Throwable("Failed to get response from server, rc is " + rc));
                    }
                }
            } catch (IOException e) {
                Loggor.error(TAG, e.toString());
                e.printStackTrace();
                if (mCallback != null) mCallback.onBindFailed(e);
            }
        }
    }


    /**
     * 绑定设备
     */
    public void doBind() {
        int randNum = (int)(Math.random() * ((1 << 31) - 1));
        int timestamp = (int)(System.currentTimeMillis() / 1000);
        SecretKeySpec signKey = new SecretKeySpec(AsymcSslUtils.getRSAPublicKeyFromPem(mPEMPubKey), HMAC_ALGO);
        String signSourceStr = String.format("deviceName=%s&nonce=%d&tid=%s&productId=%s&timestamp=%d",
                mDeviceName, randNum, mTid, mProductId, timestamp);
        String hmacSign="";
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            if (mac != null) {
                mac.init(signKey);
                byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
                hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        final JSONObject obj = new JSONObject();
        try {
            obj.put("deviceName", mDeviceName);
            obj.put("nonce", randNum);
            obj.put("tid", mTid);
            obj.put("productId", mProductId);
            obj.put("timestamp", timestamp);
            obj.put("signature", hmacSign);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Loggor.info(TAG, "Register request " + obj);
        HttpPostThread httpThread = new HttpPostThread(obj.toString(), mBindDeviceUrl);
        httpThread.start();
    }
}
