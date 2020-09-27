package com.tencent.cloud.ai.fr.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import java.nio.ByteBuffer;

public class ImageConverter {

    public static byte[] bitmap2RGB(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();  //返回可用于储存此位图像素的最小字节数

        ByteBuffer buffer = ByteBuffer.allocate(bytes); //  使用allocate()静态方法创建字节缓冲区
        bitmap.copyPixelsToBuffer(buffer); // 将位图的像素复制到指定的缓冲区

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[(rgba.length / 4) * 3];

        int count = rgba.length / 4;

        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {

            pixels[i * 3] = rgba[i * 4];        //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    //G
            pixels[i * 3 + 2] = rgba[i * 4 + 2];       //B

        }
        return pixels;
    }
    
    public static Bitmap argbToBitmap(byte[] argb, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argb));
        return bitmap;
    }

    /**
     * TODO 性能优化
     */
    public static Bitmap rgbToBitmap(byte[] rgb, int width, int height) {
        int[] colors = convertByteToColor(rgb);    //取RGB值转换为int数组
        if (colors == null) {
            return null;
        }
        return Bitmap.createBitmap(colors, 0, width, width, height, Config.ARGB_8888);
    }

    /** 将纯RGB数据数组转化成int像素数组 */
    private static int[] convertByteToColor(byte[] rgb) {
        int size = rgb.length;
        if (size == 0) {
            return null;
        }
        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }
        // 一般RGB字节数组的长度应该是3的倍数，
        // 不排除有特殊情况，多余的RGB数据用黑色0XFF000000填充
        int[] color = new int[size / 3 + arg];
        int red, green, blue;
        int colorLen = color.length;
        if (arg == 0) {
            for (int i = 0; i < colorLen; ++i) {
                red = convertByteToInt(rgb[i * 3]);
                green = convertByteToInt(rgb[i * 3 + 1]);
                blue = convertByteToInt(rgb[i * 3 + 2]);
                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
        } else {
            for (int i = 0; i < colorLen - 1; ++i) {
                red = convertByteToInt(rgb[i * 3]);
                green = convertByteToInt(rgb[i * 3 + 1]);
                blue = convertByteToInt(rgb[i * 3 + 2]);
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
            color[colorLen - 1] = 0xFF000000;
        }
        return color;
    }

    /** 将byte数当成无符号的变量去转化成int */
    private static int convertByteToInt(byte data) {
        int heightBit = (int) ((data >> 4) & 0x0F);
        int lowBit = (int) (0x0F & data);
        return heightBit * 16 + lowBit;
    }

}
