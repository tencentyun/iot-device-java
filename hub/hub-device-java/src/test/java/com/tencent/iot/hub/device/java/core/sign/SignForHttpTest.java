package com.tencent.iot.hub.device.java.core.sign;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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

        return HMACSHA256.getSignature(signSourceStr.getBytes(), devicePsk.getBytes());
    }

    public static class HMACSHA256 {

        /**
         * 生成签名数据
         *
         * @param data 待加密的数据
         * @param key  加密使用的key
         * @return 生成16进制编码的字符串
         */
        public static String getSignature(byte[] data, byte[] key)  {
            Mac mac;
            SecretKeySpec signKey = new SecretKeySpec(key, HMAC_ALGO);
            try {
                mac = Mac.getInstance(HMAC_ALGO);
                mac.init(signKey);
                byte[] rawHmac = mac.doFinal(data);
                return Base64.getEncoder().encodeToString(rawHmac);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }

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
