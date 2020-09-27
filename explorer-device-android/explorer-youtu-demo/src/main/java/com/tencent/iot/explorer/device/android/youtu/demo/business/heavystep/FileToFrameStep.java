package com.tencent.iot.explorer.device.android.youtu.demo.business.heavystep;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffId;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.Frame.Format;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;
import com.tencent.cloud.ai.fr.utils.ImageConverter;

import java.io.File;

public class FileToFrameStep extends AbsStep<StuffBox> {

    private static final String TAG = FileToFrameStep.class.getSimpleName();

    public static final StuffId<File> IN_FILE = new StuffId<>(null);

    public static final StuffId<Bitmap> OUT_BITMAP = new StuffId<>(null);
    public static final StuffId<FrameGroup> OUT_FRAME_GROUP = new StuffId<>(null);

    private StuffId<File> mInFileId;

    public FileToFrameStep(StuffId<File> inFileId) {
        mInFileId = inFileId;
    }

    @Override
    protected boolean onProcess(StuffBox stuffBox) {
        File inFile = stuffBox.find(mInFileId);
        String inFilePath = inFile.getAbsolutePath();
        Bitmap bitmap = BitmapFactory.decodeFile(inFilePath);
        if (bitmap == null) {
            Log.w(TAG, "Decode image file failed: " + inFilePath);
            return false;
        }
        byte[] rgb = ImageConverter.bitmap2RGB(bitmap);//图片转成rgb格式
        Frame frame = new Frame(Format.RGB888, rgb, bitmap.getWidth(), bitmap.getHeight(), 1);
        FrameGroup frameGroup = new FrameGroup(frame, null, null, false);
        frameGroup.name = inFile.getName();

        stuffBox.store(OUT_BITMAP, bitmap);
        stuffBox.store(OUT_FRAME_GROUP, frameGroup);

        return true;
    }
}
