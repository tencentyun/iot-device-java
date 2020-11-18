package com.tencent.iot.explorer.device.face.business.heavystep;

import android.graphics.Bitmap;

import com.tencent.youtu.YTFaceTracker.TrackedFace;

public class FaceForConfirm {

    public TrackedFace face;
    public String name;
    public Bitmap faceBmp;

    public FaceForConfirm(TrackedFace face, String name, Bitmap faceBmp) {
        this.face = face;
        this.name = name;
        this.faceBmp = faceBmp;
    }
}
