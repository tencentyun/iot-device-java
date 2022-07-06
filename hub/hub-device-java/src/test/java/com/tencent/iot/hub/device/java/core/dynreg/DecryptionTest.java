package com.tencent.iot.hub.device.java.core.dynreg;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertTrue;
import static junit.framework.TestCase.fail;


/**
 * 解密动态注册时服务器返回的经加密的Device PSK
 */
public class DecryptionTest {

    private static final String DECRYPT_MODE = "AES/CBC/NoPadding";
    private static final String productSecret = ""; // 产品密钥
    private static final String serverResponseJson = ""; // 形如 {"Response":{"Len":0,"Payload":"","RequestId":""}}

    @Test
    public void testDecryption() {
        try {
            System.out.println(/*decryption(serverResponseJson)*/);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private String decryption(String serverRsp) {
        final Base64.Decoder decoder = Base64.getDecoder();
        String payload;
        int len;
        try {
            JSONObject rsp = new JSONObject(serverRsp);
            rsp = rsp.getJSONObject("Response");
            payload = rsp.getString("Payload");
            len = rsp.getInt("Len");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

        byte[] decodeBytes;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(productSecret.substring(0, 16).getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(DECRYPT_MODE);
            byte[] ivArr = new byte[cipher.getBlockSize()];
            for (int i = 0; i < 16; i++) {
                ivArr[i] = (byte) '0';
            }
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(ivArr));
            decodeBytes = cipher.doFinal(decoder.decode(payload));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return "";
        }
        String decodeStr = new String(decodeBytes);
        decodeStr = decodeStr.substring(0, len);
        try {
            JSONObject rspObj = new JSONObject(decodeStr);
            return rspObj.getString("psk");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
}
