package com.tencent.cloud.ai.fr.camera;

public class FrameGroup {

    public final Frame colorFrame;
    public final Frame irFrame;
    public final Frame depthFrame;
    public final boolean isContinuous;
    /** 帧的名字, 例如用于表示图片文件名, 或者时间戳 */
    public String name = "";

    /**
     * @param isContinuous 是否连续的帧. 一般情况下, 来自相机的帧选择 true, 来自图片文件的帧选 false. true 会提高性能, 但是前后帧人脸位置突变时可能会漏检.
     */
    public FrameGroup(Frame colorFrame, Frame irFrame, Frame depthFrame, boolean isContinuous) {
        this.colorFrame = colorFrame;
        this.irFrame = irFrame;
        this.depthFrame = depthFrame;
        this.isContinuous = isContinuous;
    }

    /**
     * 深拷贝
     */
    public FrameGroup(FrameGroup src) {
        this.colorFrame = new Frame(src.colorFrame);
        this.irFrame = src.irFrame == null ? null : new Frame(src.irFrame);
        this.depthFrame = src.depthFrame == null ? null : new Frame(src.depthFrame);
        this.isContinuous = src.isContinuous;
        this.name = src.name;
    }

}
