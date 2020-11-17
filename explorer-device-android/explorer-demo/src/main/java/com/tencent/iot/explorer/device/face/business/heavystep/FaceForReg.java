package com.tencent.iot.explorer.device.face.business.heavystep;

import com.tencent.youtu.YTFaceTracker.TrackedFace;

public class FaceForReg {

    public TrackedFace face;
    public String name;
    public float[] feature;

    public FaceForReg(TrackedFace face, String name, float[] feature) {
        this.face = face;
        this.name = name;
        this.feature = feature;
    }
}
