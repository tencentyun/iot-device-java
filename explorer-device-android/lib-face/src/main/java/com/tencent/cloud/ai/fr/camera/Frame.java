package com.tencent.cloud.ai.fr.camera;

import android.support.annotation.NonNull;

public class Frame {

    public enum Format {
        YUV_NV21,
        RGB888,
        DEPTH16,
        IR16,
    }

    public final Format format;

    public final byte[] data;
    public final int width;
    public final int height;
    public final int exifOrientation;
    public final long enqueueTime;

    public Frame(Format format, byte[] data, int width, int height, int exifOrientation) {
        this.format = format;
        this.data = data;
        this.width = width;
        this.height = height;
        this.exifOrientation = exifOrientation;
        this.enqueueTime = System.currentTimeMillis();
    }

    /**
     * 深拷贝
     */
    public Frame(@NonNull Frame src) {
        this.format = src.format;
        this.width = src.width;
        this.height = src.height;
        this.exifOrientation = src.exifOrientation;
        this.enqueueTime = System.currentTimeMillis();

        this.data = new byte[src.data.length];
        System.arraycopy(src.data, 0, this.data, 0, src.data.length);
    }
}
