package com.tencent.iot.explorer.device.android.youtu.demo.business.job;

import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;

import java.util.HashMap;
import java.util.Map;

/** 物料箱{@link AbsJob#stuffBox} 的实际定义 */
public class StuffBox {

    private OnRecycleListener mOnRecycleListener;
    private final Map<StuffId<?>, Object> mBox = new HashMap<>();
    private final Map<Long, YTSDKManager> mYTSDKManagerMap = new HashMap<>();

    public <T> StuffBox/*方便链式调用*/ store(StuffId<T> id, T product) {
        mBox.put(id, product);
        return this;
    }

    public <T> T find(StuffId<T> id) {
        T t = (T) mBox.get(id);
        if (t == null) {
            t = id.getDefaultStuff();
        }
        return t;
    }

    /**
     * SDK 不是线程安全的, 因此每个线程需要使用单独的一个 SDK 实例. 用这方法为某个线程指定 SDK 实例
     */
    public StuffBox/*方便链式调用*/ setSdkManagerForThread(YTSDKManager sdkMgr, long threadId){
        mYTSDKManagerMap.put(threadId, sdkMgr);
        return this;
    }

    /**
     * 获取当前线程对应的SDK实例
     */
    public YTSDKManager getCurrentThreadSdkManager(){
        long id = Thread.currentThread().getId();
        YTSDKManager sdkMgr = mYTSDKManagerMap.get(id);
        if (sdkMgr == null) {
            throw new NullPointerException("No SDKManager instance found for current thread, forget calling StuffBox.setSdkManagerForThread() ?");
        }
        return sdkMgr;
    }

    public StuffBox/*方便链式调用*/ setOnRecycleListener(OnRecycleListener l) {
        mOnRecycleListener = l;
        return this;
    }

    public interface OnRecycleListener {

        void onRecycle(StuffBox stuffBox);
    }

    /*package*/ void recycle() {
        if (mOnRecycleListener != null) {
            mOnRecycleListener.onRecycle(this);
        }
    }

}
