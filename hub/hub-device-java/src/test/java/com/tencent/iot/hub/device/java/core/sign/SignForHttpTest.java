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
                    "YourDevicePsk", "YourTopicName", yourPayload,0);
            System.out.println(signature);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public static String getSignature(String productID, String deviceName, String devicePsk,
                                      String topicName, JSONObject payload, Integer qos) {
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_ALGO);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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
            hashedRequest = HmacSha256.bytesToHexString(encodedhash);
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

        String hmacSign = "";
        try {
            mac.init(signKey);
            byte[] rawHmac = mac.doFinal(signSourceStr.getBytes());
            hmacSign = Base64.encodeToString(rawHmac, Base64.NO_WRAP);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return hmacSign;
    }

    public static class HMACSHA256 {
        private static final String HMAC_SHA256 = "HmacSHA256";

        /**
         * 生成签名数据
         *
         * @param data 待加密的数据
         * @param key  加密使用的key
         * @return 生成16进制编码的字符串
         */
        public static String getSignature(byte[] data, byte[] key) {
            try {
                SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA256);
                Mac mac = Mac.getInstance(HMAC_SHA256);
                mac.init(signingKey);
                byte[] rawHmac = mac.doFinal(data);
                return bytesToHexString(rawHmac);
            } catch (Exception e) {
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

        public static String getRandomString2(int length) {
            Random random = new Random();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < length; i++) {
                int number = random.nextInt(3);
                long result = 0;
                switch (number) {
                    case 0:
                        result = Math.round(Math.random() * 25 + 65);
                        sb.append(String.valueOf((char) result));
                        break;
                    case 1:
                        result = Math.round(Math.random() * 25 + 97);
                        sb.append(String.valueOf((char) result));
                        break;
                    case 2:
                        sb.append(String.valueOf(new Random().nextInt(10)));
                        break;
                }
            }
            return sb.toString();
        }
    }
}
