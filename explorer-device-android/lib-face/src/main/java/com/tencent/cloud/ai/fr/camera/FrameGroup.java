package com.tencent.cloud.ai.fr.camera;

public class FrameGroup {

    public final Frame colorFrame;
    public final Frame irFrame;
    public final Frame depthFrame;
    /** 帧的名字, 例如用于表示图片文件名, 或者时间戳 */
    public String name = "";

    public FrameGroup(Frame colorFrame, Frame irFrame, Frame depthFrame) {
        this.colorFrame = colorFrame;
        this.irFrame = irFrame;
        this.depthFrame = depthFrame;
    }

    /**
     * 深拷贝
     */
    public FrameGroup(FrameGroup frameGroup) {
        this.colorFrame = new Frame(frameGroup.colorFrame);
        this.irFrame = frameGroup.irFrame == null ? null : new Frame(frameGroup.irFrame);
        this.depthFrame = frameGroup.depthFrame == null ? null : new Frame(frameGroup.depthFrame);
        this.name = frameGroup.name;
    }

}
