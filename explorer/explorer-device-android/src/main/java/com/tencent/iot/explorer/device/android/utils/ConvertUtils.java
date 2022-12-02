package com.tencent.iot.explorer.device.android.utils;

public class ConvertUtils {

    /**
     * 将byte转为16进制
     *
     * @param bytes`
     * @return
     */
    public static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                // 1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    /**
     * 将byte转为16进制
     *
     * @param bytes`
     * @return
     */
    public static String byte2HexOnlyLatest8(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        int count = bytes.length;
        if (bytes.length > 8) {
            count = 8;
        }
        for (int i = 0; i < count; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                // 1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    /**
     * 16进制转10进制
     *
     * @param str
     * @return
     */
    public static int hexString2Decimal(String str) {

        try {
            int in = Integer.parseInt(str, 16);
            return in;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    /**
     * int到byte[] 由高位到低位
     * @param i 需要转换为byte数组的整行值。
     * @return byte数组
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[1];
        result[0] = (byte) (i & 0xFF);
        return result;
    }
}
