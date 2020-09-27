package com.tencent.iot.explorer.device.android.youtu.demo.business.thread;

import android.content.res.AssetManager;

import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.pipeline.AbsPipelineThread;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;
import com.tencent.iot.explorer.device.android.youtu.demo.business.job.StuffBox;

/**
 * 重型任务流水线线程, 对实时性要求不高的任务建议在这个线程里执行
 * @see LightThread
 */
public class HeavyThread extends AbsPipelineThread<StuffBox> {

    /** SDK 实例管理器, 由于 SDK 实例不是线程安全的, 所以每个线程必须创建属于自己的 SDK 实例管理器 */
    private final YTSDKManager mSdkManager;

    public HeavyThread(AssetManager assetManager) {
        super(HeavyThread.class.getSimpleName());
        mSdkManager = new YTSDKManager(assetManager);
    }

    @Override
    protected void onThreadFinish() {
        mSdkManager.destroy();
    }

    @Override
    public void enqueue(AbsJob<StuffBox> job) {
        job.stuffBox.setSdkManagerForThread(mSdkManager, HeavyThread.this.getId());
        super.enqueue(job);
    }
}
