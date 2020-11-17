package com.tencent.iot.explorer.device.face.business.thread;

import android.content.res.AssetManager;

import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.pipeline.AbsPipelineThread;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.face.business.job.StuffBox;

/**
 * 轻量任务流水线, 主要运行实时性要求高的任务, 例如人脸检测, 快速筛选
 * @see HeavyThread
 */
public class LightThread extends AbsPipelineThread<StuffBox> {

    /**
     * SDK 实例管理器, 由于 SDK 实例不是线程安全的, 所以每个线程必须创建属于自己的 SDK 实例管理器
     */
    private final YTSDKManager mSDKManager;

    public LightThread(AssetManager assetManager) {
        super(LightThread.class.getSimpleName());
        mSDKManager = new YTSDKManager(assetManager);
    }

    @Override
    protected void onThreadFinish() {
        mSDKManager.destroy();
    }

    @Override
    public void enqueue(AbsJob<StuffBox> job) {
        job.stuffBox.setSdkManagerForThread(mSDKManager, LightThread.this.getId());
        super.enqueue(job);
    }

}
