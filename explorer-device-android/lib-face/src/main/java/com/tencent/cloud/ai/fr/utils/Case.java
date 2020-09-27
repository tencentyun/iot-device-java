package com.tencent.cloud.ai.fr.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.instagram.igdiskcache.EditorOutputStream;
import com.instagram.igdiskcache.IgDiskCache;
import com.instagram.igdiskcache.OptionalStream;
import com.tencent.cloud.ai.fr.camera.Frame;
import com.tencent.cloud.ai.fr.camera.FrameGroup;
import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.pipeline.AbsPipelineThread;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 案例工具包
 * <p>
 * 包含案例定义, 案例保存线程, 磁盘缓存工具
 * </p>
 */
public class Case {

    private static final CaseSaveThread sSaveThread = new CaseSaveThread();

    static {
        sSaveThread.start();
    }

    private final String aiFunction;
    private final boolean isOk;
    private final long time;
    private final float score;
    private final FrameGroup frameGroup;
    private final float threshold;
    private final String version;

    public Case(String aiFunction, boolean isOk, long time, float score, FrameGroup frameGroup, float threshold, String version) {
        this.aiFunction = aiFunction;
        this.isOk = isOk;
        this.time = time;
        this.score = score;
        this.threshold = threshold;
        this.version = version;
        //因为源数据来自其它线程, 所以需要深拷贝, 避免源数据发生变化影响本线程
        this.frameGroup = new FrameGroup(frameGroup);
    }

    public void save() {
        sSaveThread.enqueue(this);
    }

    /**
     * 案例保存线程, 保存案例到磁盘, 供以后有需要时分析问题用
     */
    private static class CaseSaveThread extends AbsPipelineThread<Case> {

        private static final String CASE_SAVE_DIR = "/sdcard/SavedCases/";
        
        private DiskCache mDiskCache;
        private ArrayList<AbsStep<Case>> mPipeline;

        public CaseSaveThread() {
            super(CaseSaveThread.class.getSimpleName());
    

            mPipeline = new ArrayList<>();
            mPipeline.add(new AbsStep<Case>() {

                @Override
                protected boolean onProcess(Case aCase) {
                    mDiskCache.saveCase(aCase);
                    return false;//没有下一个步骤了
                }
            });
        }

        void enqueue(Case aCase) {
            enqueue(new AbsJob<Case>(aCase, mPipeline) {
                @Override
                protected void recycle() {/*nothing to do*/}
            });
        }

        @Override
        protected void onThreadStart() {
            try {
                mDiskCache = new DiskCache(CASE_SAVE_DIR);
            } catch (IOException e) {
                throw new RuntimeException("创建磁盘缓存失败: " + CASE_SAVE_DIR, e);
            }
        }

        @Override
        protected void onThreadFinish() {
            if (mDiskCache != null) {
                mDiskCache.onDestroy();
            }
        }
    }

    private static class DiskCache {

        // AIFUNCTION_SUCCESS_TIME_VERSION_thrs%s_score%s_WIDTHxHEIGHT
        private static final String FORMAT = "%s_%s_%s_%s_thrs%s_score%.4f_%sx%s";//缓存工具不支持大写
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS", Locale.getDefault());

        public static final String FRAME_TYPE_COLOR = "_color";
        public static final String FRAME_TYPE_IR = "_ir";
        public static final String FRAME_TYPE_DEPTH = "_depth";

        private IgDiskCache mCache;

        public DiskCache(String cacheDirPath) throws IOException {
            File dir = new File(cacheDirPath);
            if (!dir.exists()) {
                boolean mkdirsSuccess = dir.mkdirs();
                if (!mkdirsSuccess || !dir.exists()) {
                    throw new IOException("create cache dir fail: " + dir.getAbsolutePath());
                }
            }
            int maxSize = 256 * 1024 * 1024;// 256MB
            int maxCount = 1000;
            mCache = new IgDiskCache(dir, maxSize, maxCount);
        }

        public void onDestroy() {
            if (mCache != null) {
                mCache.flush();
                mCache.close();
            }
        }

        public void saveCase(Case c) {
            save(c, c.frameGroup.colorFrame, FRAME_TYPE_COLOR);
            if (c.frameGroup.irFrame != null) {
                save(c, c.frameGroup.irFrame, FRAME_TYPE_IR);
            }
            if (c.frameGroup.depthFrame != null) {
                save(c, c.frameGroup.depthFrame, FRAME_TYPE_DEPTH);
            }
        }

        private void save(Case c, Frame frame, String frameType) {
            String baseFileName = String.format(FORMAT, c.aiFunction, c.isOk, DATE_FORMAT.format(c.time),
                    c.version.replaceAll("\\.", ""),//缓存工具不支持点
                    c.threshold, c.score, frame.width, frame.height)
                    .replaceAll("\\.", "_")//很遗憾不支持点
                    .toLowerCase();//很遗憾也不支持大写

            save(frame.data, baseFileName + frameType);//原始图片

            if (FRAME_TYPE_COLOR.equals(frameType)) {//把RGB可视化
                Bitmap bitmap = ImageConverter.rgbToBitmap(frame.data, frame.width, frame.height);
                save(bitmap, baseFileName + "_jpg");
            }
        }

        private void save(byte[] data, String fileName) {
            OptionalStream<EditorOutputStream> outputStream = mCache.edit(fileName);
            if (outputStream.isPresent()) {
                try {
                    outputStream.get().write(data);
                    outputStream.get().commit();
                } finally {
                    outputStream.get().abortUnlessCommitted();
                }
            }
        }

        private void save(Bitmap bitmap, String fileName) {
            OptionalStream<EditorOutputStream> outputStream = mCache.edit(fileName);
            if (outputStream.isPresent()) {
                try {
                    bitmap.compress(CompressFormat.JPEG, 70, outputStream.get());
                    outputStream.get().commit();
                } finally {
                    outputStream.get().abortUnlessCommitted();
                }
            }
        }
    }
}
