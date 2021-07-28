package com.tencent.iot.hub.device.java.core.sign;

import com.tencent.iot.hub.device.java.core.util.Base64;
import com.tencent.iot.hub.device.java.core.util.HmacSha256;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class SignForHttpTest {

    private static final String HMAC_ALGO = "hmacsha256";

    @Test
    public void testSign() {
        try {
            JSONObject yourPayload = new JSONObject();
            String signature = getSignature("YourProductId", "YourDeviceName",
                    "YourDevicePsk", "YourTopicName", yourPayload, 0);
            System.out.println(signature);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public static String getSignature(String productID, String deviceName, String devicePsk,
                                      String topicName, JSONObject payload, Integer qos) {
        int randNum = (int) (Math.random() * ((1 << 31) - 1));
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        SecretKeySpec signKey = new SecretKeySpec(devicePsk.getBytes(), HMAC_ALGO);
        final JSONObject obj = new JSONObject();
        try {
            obj.put("ProductId", productID);
            obj.put("DeviceName", deviceName);
            obj.put("TopicName", topicName);
            obj.put("Payload", payload.toString());
            obj.put("Qos", qos);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String originRequest = obj.toString();
        String hashedRequest = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(originRequest.getBytes(Charset.forName("UTF-8")));
            hashedRequest = HMACSHA256.bytesToHexString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
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

        Mac mac;
        String hmacSign = "";
        try {
            mac = Mac.getInstance(HMAC_ALGO);
            mac.init(signKey);
            byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
            hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hmacSign;
    }

    public static class HMACSHA256 {
        /**
         * byte[]数组转换为16进制的字符串
         *
         * @param bytes 要转换的字节数组
         * @return 转换后的结果
         */
        private static String bytesToHexString(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        }
    }
}
