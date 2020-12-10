package com.tencent.iot.hub.device.android.core.dynreg;

import android.os.NetworkOnMainThreadException;
import android.util.Base64;
import android.util.Log;

import com.tencent.iot.hub.device.android.core.util.TXLog;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * The type Tx iothub dynreg.
 */
public class TXMqttDynreg {
    private static final String TAG = "TXMQTT";
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final String DECRYPT_MODE = "AES/CBC/NoPadding";

    private String mProductKey;
    private String mProductId;
    private String mDynRegUrl;
    private String mDeviceName;

    private TXMqttDynregCallback mCallback;

    // 默认的动态注册URL，文档链接：https://cloud.tencent.com/document/product/634/47225
    private final String mDefaultDynRegUrl ="http://ap-guangzhou.gateway.tencentdevices.com/register/dev";


    /**
     * Instantiates a new Tx iothub dynreg.
     *
     * @param dynregUrl  the dynreg url
     * @param productId  the product id
     * @param productKey the product key
     * @param deviceName the device name
     * @param callback    the callback for operation result
     */
    public TXMqttDynreg(String dynregUrl, String productId, String productKey, String deviceName, TXMqttDynregCallback callback) {
        this.mDynRegUrl = dynregUrl;
        this.mProductId = productId;
        this.mProductKey = productKey;
        this.mDeviceName = deviceName;
        this.mCallback = callback;
    }

    /**
     * Instantiates a new Tx iothub dynreg.
     *
     * @param productId  the product id
     * @param productKey the product key
     * @param deviceName the device name
     * @param callback callback for operation result
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

        /**
         * Instantiates a new Http post thread.
         *
         * @param upStr the up str
         * @param upUrl the up url
         */
        HttpPostThread(String upStr, String upUrl) {
            this.postData = upStr;
            this.url = upUrl;
        }

        public void run() {
            StringBuffer serverRsp = new StringBuffer();
            try {
                URL url = new URL(mDynRegUrl);
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
                if(rc == 200){
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        serverRsp.append(line);
                    }
                    conn.disconnect();
                }else {
                    Log.e(TAG, "Get error rc "+ rc);
                    conn.disconnect();

                    mCallback.onFailedDynreg(new Throwable("Failed to get response from server, rc is " + rc));
                    return;
                }

            } catch (IOException|NetworkOnMainThreadException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();

                mCallback.onFailedDynreg(e);
                return;
            }

            String plStr;
            int actLen;
            Log.i(TAG, "Get response string " + serverRsp);
            try {
                JSONObject rspObj = new JSONObject(serverRsp.toString());
                plStr = rspObj.getString("payload");
                actLen = rspObj.getInt("len");
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
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
                return;
            }
        }
    }

    /**
     * Do dynamic register
     *
     * @return true for register OK, false for register ERROR
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

        String signSourceStr = String.format("deviceName=%s&nonce=%d&productId=%s&timestamp=%d", mDeviceName, randNum, mProductId, timestamp);

        try {
            mac.init(signKey);
            byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
            StringBuffer sBuffer = new StringBuffer();
            for (int i = 0; i < rawHmac.length; i++) {
                sBuffer.append(String.format("%02x", rawHmac[i] & 0xff));
            }

            hmacSign = Base64.encodeToString(sBuffer.toString().getBytes(), Base64.NO_WRAP);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

        final JSONObject obj = new JSONObject();
        try {
            obj.put("deviceName", mDeviceName);
            obj.put("nonce", randNum);
            obj.put("productId", mProductId);
            obj.put("timestamp", timestamp);
            obj.put("signature", hmacSign);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        TXLog.i(TAG, "Register request " + obj);
        HttpPostThread httpThread = new HttpPostThread(obj.toString(), mDefaultDynRegUrl);
        httpThread.start();

        return true;
    }
}
