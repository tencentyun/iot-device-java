package com.tencent.iot.hub.device.java.core.httppublish;

import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;
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
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * The type Tx iothub http publish.
 */
public class TXHTTPPulish {
    private static final String TAG = TXHTTPPulish.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TXHTTPPulish.class);
    private static final String HMAC_ALGO = "hmacsha256";
    private static final String DECRYPT_MODE = "AES/CBC/NoPadding";

    private String mDevicePsk;
    private String mProductId;
    private String mHttpPublishUrl;
    private String mDeviceName;

    private TXHttpPublishCallback mCallback;

    // 默认的设备基于 HTTP 协议接入 URL，文档链接：https://cloud.tencent.com/document/product/634/36123
    private final String mDefaultHttpPublishUrl ="https://ap-guangzhou.gateway.tencentdevices.com/device/publish";

    static { Loggor.setLogger(logger); }

    /**
     * Instantiates a new Tx iothub http publish.
     *
     * @param httpPublishUrl  the http publish url
     * @param productId  the product id
     * @param devicePsk the device password
     * @param deviceName the device name
     * @param callback    the callback for operation result
     */
    public TXHTTPPulish(String httpPublishUrl, String productId, String devicePsk, String deviceName, TXHttpPublishCallback callback) {
        this.mHttpPublishUrl = httpPublishUrl;
        this.mProductId = productId;
        this.mDevicePsk = devicePsk;
        this.mDeviceName = deviceName;
        this.mCallback = callback;
    }

    /**
     * Instantiates a new Tx iothub http publish.
     *
     * @param productId  the product id
     * @param devicePsk the device password
     * @param deviceName the device name
     * @param callback callback for operation result
     */
    public TXHTTPPulish(String productId, String devicePsk, String deviceName, TXHttpPublishCallback callback) {
        this.mHttpPublishUrl = mDefaultHttpPublishUrl;
        this.mProductId = productId;
        this.mDevicePsk = devicePsk;
        this.mDeviceName = deviceName;
        this.mCallback = callback;
    }

    private class HttpPostThread extends Thread {
        private String postData;
        private String url;
        private String timestamp;
        private String nonce;
        private String signature;

        /**
         * Instantiates a new Http post thread.
         *
         * @param upStr 请求body
         * @param upUrl 请求url
         * @param timestamp 时间戳
         * @param nonce 随机数
         * @param signature 签名
         */
        HttpPostThread(String upStr, String upUrl, String timestamp, String nonce, String signature) {
            this.postData = upStr;
            this.url = upUrl;
            this.timestamp = timestamp;
            this.nonce = nonce;
            this.signature = signature;
        }

        @Override
        public void run() {
            this.setName("tencent-" + HttpPostThread.class.getSimpleName().toLowerCase());

            StringBuffer serverRsp = new StringBuffer();
            try {
                URL url = new URL(mHttpPublishUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept","application/json");
                conn.setRequestProperty("X-TC-Algorithm", HMAC_ALGO);
                conn.setRequestProperty("X-TC-Timestamp", timestamp);
                conn.setRequestProperty("X-TC-Nonce", nonce);
                conn.setRequestProperty("X-TC-Signature", signature);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(2000);
                Loggor.info(TAG, "HttpURLConnection header: "+ conn.getRequestProperties());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.write(postData.getBytes());
                os.flush();
                os.close();
                Loggor.info(TAG, "TXHTTPPulish postData "+ postData);

                int rc = conn.getResponseCode();
                String line;
                if (rc == 200) {
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        serverRsp.append(line);
                    }
                    conn.disconnect();
                } else {
                    Loggor.error(TAG, "Get error rc "+ rc);
                    conn.disconnect();
                    mCallback.onFailedPublish(new Throwable("Failed to get response from server, rc is " + rc));
                    return;
                }
            } catch (IOException e) {
                Loggor.error(TAG, e.toString());
                e.printStackTrace();
                mCallback.onFailedPublish(e);
                return;
            }

            String requestId;
            Loggor.info(TAG, "Get response string " + serverRsp);
            try {
                JSONObject rspObj = new JSONObject(serverRsp.toString());
                rspObj = rspObj.getJSONObject("Response");
                Loggor.info(TAG, rspObj.toString());
                if (rspObj.has("Error")) {
                    mCallback.onFailedPublish(new Exception(rspObj.toString()));
                } else {
                    requestId = rspObj.getString("RequestId");
                    mCallback.onSuccessPublishGetRequestId(requestId);
                }
            } catch (JSONException e) {
                Loggor.error(TAG, e.toString());
                e.printStackTrace();
                mCallback.onFailedPublish(e, "receive Msg " + serverRsp);
                return;
            }
        }
    }

    /**
     * Do http publish message
     *
     * @return true for publish OK, false for publish ERROR
     */
    public boolean doHttpPublish(String topicName, JSONObject payload, Integer qos) {
        Mac mac = null;

        try {
            mac = Mac.getInstance(HMAC_ALGO);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        int randNum = (int)(Math.random() * ((1 << 31) - 1));
        String hmacSign;
        int timestamp = (int)(System.currentTimeMillis() / 1000);
        SecretKeySpec signKey = new SecretKeySpec(mDevicePsk.getBytes(), HMAC_ALGO);

        final JSONObject obj = new JSONObject();
        try {
            obj.put("ProductId", mProductId);
            obj.put("DeviceName", mDeviceName);
            obj.put("TopicName", topicName);
            obj.put("Payload", payload.toString());
            obj.put("Qos", qos);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        String originRequest = obj.toString();
        String hashedRequest = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(originRequest.getBytes(Charset.forName("UTF-8")));
            hashedRequest = HmacSha256.bytesToHexString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        @SuppressWarnings("DefaultLocale")
        String signSourceStr = String.format("%s\n%s\n%s\n%s\n%s\n%d\n%d\n%s",
                "POST",
                "ap-guangzhou.gateway.tencentdevices.com",
                "/device/publish",
                "",
                HMAC_ALGO,
                timestamp,
                randNum,
                hashedRequest
                );

        try {
            mac.init(signKey);
            byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
            hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

        Loggor.info(TAG, "Publish request " + obj + "; signSourceStr:" + signSourceStr);
        HttpPostThread httpThread = new HttpPostThread(obj.toString(), mHttpPublishUrl,
                String.valueOf(timestamp), String.valueOf(randNum), hmacSign);
        httpThread.start();

        return true;
    }
}
