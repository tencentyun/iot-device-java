package com.tencent.iot.hub.device.android.core.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacSha1 {

    private static final String HMAC_SHA1= "HmacSHA1";

    /**
     * 生成签名数据
     *
     * @param data 待加密的数据
     * @param key  加密使用的key
     * @return 生成16进制编码的字符串
     */
    public static String getSignature(byte[] data, byte[] key)  {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1);
            Mac mac = Mac.getInstance(HMAC_SHA1);
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
}
