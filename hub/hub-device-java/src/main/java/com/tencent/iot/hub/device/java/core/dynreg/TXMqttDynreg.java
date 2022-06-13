package com.tencent.iot.hub.device.java.core.dynreg;

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * MQTT 动态注册类
 */
public class TXMqttDynreg {
    private static final String TAG = TXMqttDynreg.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TXMqttDynreg.class);
    private static final String HMAC_ALGO = "hmacsha256";
    private static final String DECRYPT_MODE = "AES/CBC/NoPadding";

    private String mProductKey;
    private String mProductId;
    private String mDynRegUrl;
    private String mDeviceName;

    private TXMqttDynregCallback mCallback;

    // 默认的动态注册 URL，文档链接：https://cloud.tencent.com/document/product/634/47225
    private final String mDefaultDynRegUrl ="https://ap-guangzhou.gateway.tencentdevices.com/device/register";

    static { Loggor.setLogger(logger); }

    /**
     * 构造函数
     *
     * @param dynregUrl 动态注册 url
     * @param productId 产品 ID
     * @param productKey 产品密钥
     * @param deviceName 设备名
     * @param callback 动态注册结果回调 {@link TXMqttDynregCallback}
     */
    public TXMqttDynreg(String dynregUrl, String productId, String productKey, String deviceName, TXMqttDynregCallback callback) {
        this.mDynRegUrl = dynregUrl;
        this.mProductId = productId;
        this.mProductKey = productKey;
        this.mDeviceName = deviceName;
        this.mCallback = callback;
    }

    /**
     * 构造函数
     *
     * @param productId 产品 ID
     * @param productKey 产品密钥
     * @param deviceName 设备名
     * @param callback 动态注册结果回调 {@link TXMqttDynregCallback}
     */
    public TXMqttDynreg(String productId, String productKey, String deviceName, TXMqttDynregCallback callback) {
        this.mDynRegUrl = mDefaultDynRegUrl;
        this.mProductId = productId;
        this.mProductKey = productKey;
        this.mDeviceName = deviceName;
        this.mCallback = callback;
    }

    private String inputStream2String(InputStream in) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(in, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line = "";
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
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
            this.setName("tencent-dynreg-http-post-thread");

            StringBuffer serverRsp = new StringBuffer();
            try {
                URL url = new URL(mDynRegUrl);
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
                os.writeBytes(postData);
                os.flush();
                os.close();
                Loggor.info(TAG, "TXMqttDynreg postData "+ postData);

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
                    mCallback.onFailedDynreg(new Throwable("Failed to get response from server, rc is " + rc));
                    return;
                }
            } catch (IOException e) {
                Loggor.error(TAG, e.toString());
                e.printStackTrace();
                mCallback.onFailedDynreg(e);
                return;
            }

            String plStr;
            int actLen;
            Loggor.info(TAG, "Get response string " + serverRsp);
            try {
                JSONObject rspObj = new JSONObject(serverRsp.toString());
                rspObj = rspObj.getJSONObject("Response");
                plStr = rspObj.getString("Payload");
                actLen = rspObj.getInt("Len");
            } catch (JSONException e) {
                Loggor.error(TAG, e.toString());
                e.printStackTrace();
                mCallback.onFailedDynreg(e, "receive Msg " + serverRsp);
                return ;
            }

            byte[] plBytes;
            try {
                SecretKeySpec skeySpec = new SecretKeySpec(mProductKey.substring(0, 16).getBytes(), "AES");
                Cipher cipher = Cipher.getInstance(DECRYPT_MODE);
                byte [] ivArr = new byte[cipher.getBlockSize()];
                for (int i = 0; i < 16; i++) {
                    ivArr[i] = (byte)'0';
                }
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(ivArr));
                plBytes = cipher.doFinal(Base64.decode(plStr, Base64.DEFAULT));
            } catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|IllegalBlockSizeException|BadPaddingException|InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                mCallback.onFailedDynreg(e);
                return;
            }
            String rspSb = new String(plBytes);
            rspSb = rspSb.substring(0, actLen);
            try {
                JSONObject rspObj = new JSONObject(rspSb.toString());
                int encryptionType = rspObj.getInt("encryptionType");

                // Cert
                if (encryptionType == 1) {
                    mCallback.onGetDeviceCert(rspObj.getString("clientCert"), rspObj.getString("clientKey"));
                } else if (encryptionType == 2) {
                    // PSK
                    mCallback.onGetDevicePSK(rspObj.getString("psk"));
                } else {
                    mCallback.onFailedDynreg(new Throwable("Get wrong encryption type:" + encryptionType));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                mCallback.onFailedDynreg(e);
            }
        }
    }

    /**
     * 动态注册
     *
     * @return 动态注册结果；true：OK；false：ERROR
     */
    public boolean doDynamicRegister() {
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
        SecretKeySpec signKey = new SecretKeySpec(mProductKey.getBytes(), HMAC_ALGO);

        final JSONObject obj = new JSONObject();
        try {
            obj.put("ProductId", mProductId);
            obj.put("DeviceName", mDeviceName);
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
        }

        @SuppressWarnings("DefaultLocale")
        String signSourceStr = String.format("%s\n%s\n%s\n%s\n%s\n%d\n%d\n%s",
                "POST",
                "ap-guangzhou.gateway.tencentdevices.com",
                "/device/register",
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

        Loggor.info(TAG, "Register request " + obj + "; signSourceStr:" + signSourceStr);
        HttpPostThread httpThread = new HttpPostThread(obj.toString(), mDynRegUrl,
                String.valueOf(timestamp), String.valueOf(randNum), hmacSign);
        httpThread.start();

        return true;
    }
}
