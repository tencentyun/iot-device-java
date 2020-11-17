package com.tencent.iot.explorer.device.face.business.lightstep;

import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.Frame.Format;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;
import com.tencent.iot.explorer.device.face.business.job.StuffId;
import com.tencent.youtu.YTImage;
import com.tencent.youtu.YTUtils;

/** 1. 帧数据预处理 */
public class PreprocessStep extends AbsStep<StuffBox> {

    /** 原始帧数据 */
    public static final StuffId<FrameGroup> IN_RAW_FRAME_GROUP = new StuffId<>(null);
    /** 预处理后的帧数据 */
    public static final StuffId<FrameGroup> OUT_CONVERTED_FRAME_GROUP = new StuffId<>(null);

    private StuffId<FrameGroup> mInFrameGroupId;
    private InputFrameGroupProvider mInputFrameGroupProvider;

    public PreprocessStep(StuffId<FrameGroup> frameGroupId) {
        if (frameGroupId == null) {
            throw new NullPointerException("frameGroupId can not be null");
        }
        mInFrameGroupId = frameGroupId;
    }

    public PreprocessStep(InputFrameGroupProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider can not be null");
        }
        mInputFrameGroupProvider = provider;
    }

    @Override
    public boolean onProcess(StuffBox stuffBox) {
        FrameGroup rawFrameGroup = null;
        if (mInFrameGroupId != null) {
            rawFrameGroup = stuffBox.find(mInFrameGroupId);
        } else if (mInputFrameGroupProvider != null) {
            rawFrameGroup = mInputFrameGroupProvider.onGetInputFrameGroup();
        }
        if (rawFrameGroup == null) {
            throw new NullPointerException("No Input FrameGroup found");
        }

        stuffBox.store(IN_RAW_FRAME_GROUP, rawFrameGroup);//给入参设置约定的ID, 方便流水线后续步骤使用

        Frame rawColorFrame = rawFrameGroup.colorFrame;
        Frame rawIrFrame = rawFrameGroup.irFrame;
        Frame rawDepthFrame = rawFrameGroup.depthFrame;

        Frame convertedColorFrame = null;
        Frame convertedIrFrame = null;
        Frame convertedDepthFrame = null;

        //彩色图处理
        switch (rawColorFrame.format) {
            case YUV_NV21:
                convertedColorFrame = convertYuvNv21Frame(rawColorFrame);
                break;
            case RGB888:
                convertedColorFrame = rotateRgb888FrameIfNeeded(rawColorFrame);
                break;
            default:
                throw new UnsupportedOperationException("不支持的彩图格式: " + rawColorFrame.format);
        }
        //红外图处理
        if (rawIrFrame != null) {
            switch (rawIrFrame.format) {
                case YUV_NV21:
                    convertedIrFrame = convertYuvNv21Frame(rawIrFrame);
                    break;
                case IR16:
                    convertedIrFrame = convertIr16Frame(rawIrFrame);
                    break;
                case RGB888:
                    convertedIrFrame = rotateRgb888FrameIfNeeded(rawIrFrame);
                    break;
                default:
                    throw new UnsupportedOperationException("不支持的红外图格式: " + rawIrFrame.format);

            }
        }
        //深度图处理
        if (rawDepthFrame != null) {
            switch (rawDepthFrame.format) {
                case DEPTH16:
                    if (rawDepthFrame.exifOrientation == 1) {
                        convertedDepthFrame = rawFrameGroup.depthFrame;//无需转换
                    } else {
                        throw new UnsupportedOperationException("不支持深度图旋转, exifOrientation=" + rawDepthFrame.exifOrientation);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("不支持的深度图格式: " + rawDepthFrame.format);
            }
        }

        FrameGroup convertedFrameGroup = new FrameGroup(convertedColorFrame, convertedIrFrame, convertedDepthFrame, rawFrameGroup.isContinuous);
        stuffBox.store(OUT_CONVERTED_FRAME_GROUP, convertedFrameGroup);
        return true; //告诉流水线此任务是否成功, 如果成功则会执行下游任务, 否则中断流水线
    }

    private Frame rotateRgb888FrameIfNeeded(Frame rgb888Frame) {
        if (rgb888Frame.format != Format.RGB888) {
            throw new IllegalArgumentException("Frame format != RGB888, " + rgb888Frame);
        }
        if (rgb888Frame.exifOrientation == 1) {
            return rgb888Frame;
        }
        YTImage ytImage = YTUtils.rotateRGB888(rgb888Frame.data, rgb888Frame.width, rgb888Frame.height, rgb888Frame.exifOrientation);
        return new Frame(Format.RGB888, ytImage.data, ytImage.width, ytImage.height, 1);
    }

    private Frame convertIr16Frame(Frame rawIrFrame) {
        Frame convertedIrFrame;
        byte[] rgb888_ir = YTUtils.convert16bit2RGB888(rawIrFrame.data, rawIrFrame.width, rawIrFrame.height, 1.5f);
        if (rawIrFrame.exifOrientation == 1) {
            convertedIrFrame = new Frame(Format.RGB888, rgb888_ir, rawIrFrame.width, rawIrFrame.height, 1);
        } else {
            YTImage ytImage = YTUtils.rotateRGB888(rgb888_ir, rawIrFrame.width, rawIrFrame.height, rawIrFrame.exifOrientation);
            convertedIrFrame = new Frame(Format.RGB888, ytImage.data, ytImage.width, ytImage.height, 1);
        }
        return convertedIrFrame;
    }

    private Frame convertYuvNv21Frame(Frame yuvFrame) {
        if (yuvFrame.format != Format.YUV_NV21) {
            throw new IllegalArgumentException("Frame format != YUV_NV21, " + yuvFrame);
        }
        Frame convertedColorFrame;
        YTImage ytImage = YTUtils.yuv420spToRGB888(yuvFrame.data, yuvFrame.width, yuvFrame.height);
        if (yuvFrame.exifOrientation != 1) {
            ytImage = YTUtils.rotateRGB888(ytImage.data, ytImage.width, ytImage.height, yuvFrame.exifOrientation);
        }
        convertedColorFrame = new Frame(Format.RGB888, ytImage.data, ytImage.width, ytImage.height, 1);
        return convertedColorFrame;
    }

    public interface InputFrameGroupProvider {

        FrameGroup onGetInputFrameGroup();
    }
}
