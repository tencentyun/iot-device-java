package com.tencent.iot.hub.device.java.core.sign;

import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class SignForMqttTest {

    @Test
    public void testMqttSign() {
        try {
            System.out.println(SignForMqttTest("YourProductId","YourDeviceName","YourPsk"));
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public static Map<String, String> SignForMqttTest(String productID, String devicename, String
            devicePsk) throws Exception {
        final Base64.Decoder decoder = Base64.getDecoder();
        //1. 生成 connid 为一个随机字符串，方便后台定位问题
        String connid = HMACSHA256.getConnectId(5);
        //2. 生成过期时间，表示签名的过期时间,从纪元1970年1月1日 00:00:00 UTC 时间至今秒数的 UTF8 字符串
        Long expiry = Calendar.getInstance().getTimeInMillis()/1000 + 600;
        //3. 生成 MQTT 的 clientid 部分, 格式为 ${productid}${devicename}
        String clientid = productID+devicename;
        //4. 生成 MQTT 的 username 部分, 格式为 ${clientid};${sdkappid};${connid};${expiry}
        String username = clientid+";"+"12010126;"+connid+";"+expiry;
        //5.  对 username 进行签名，生成token、根据物联网通信平台规则生成 password 字段
        String password = HMACSHA256.getSignature(username.getBytes(), decoder.decode(devicePsk)) + ";hmacsha256";
        Map<String,String> map = new HashMap<>();
        map.put("clientid",clientid);
        map.put("username",username);
        map.put("password",password);
        return map;
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
        public static String getSignature(byte[] data, byte[] key)  {
            try {
                SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA256);
                Mac mac = Mac.getInstance(HMAC_SHA256);
                mac.init(signingKey);
                byte[] rawHmac = mac.doFinal(data);
                return bytesToHexString(rawHmac);
            }catch (Exception e) {
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


        /**
         * 获取连接ID（长度为5的数字字母随机字符串）
         */
        public static String getConnectId(int length) {
            StringBuffer connectId = new StringBuffer();
            for (int i = 0; i < length; i++) {
                int flag = (int) (Math.random() * Integer.MAX_VALUE) % 3;
                int randNum = (int) (Math.random() * Integer.MAX_VALUE);
                switch (flag) {
                    case 0:
                        connectId.append((char) (randNum % 26 + 'a'));
                        break;
                    case 1:
                        connectId.append((char) (randNum % 26 + 'A'));
                        break;
                    case 2:
                        connectId.append((char) (randNum % 10 + '0'));
                        break;
                }
            }

            return connectId.toString();
        }
    }
}