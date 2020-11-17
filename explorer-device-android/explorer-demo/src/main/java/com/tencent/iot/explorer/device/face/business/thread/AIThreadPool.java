package com.tencent.iot.explorer.device.face.business.thread;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.tencent.cloud.ai.fr.sdksupport.YTSDKManager;

/**
 * 算法线程池
 */
public class AIThreadPool {

    private static final String TAG = "AIThreadPool";

    private static volatile AIThreadPool sInstance;
    private static volatile boolean sAlreadyInit = false;

    public static AIThreadPool instance() {
        if (sInstance == null) {
            synchronized (AIThreadPool.class) {
                if (sInstance == null) {
                    sInstance = new AIThreadPool();
                }
            }
        }
        return sInstance;
    }

    private LightThread mTrackThread;
    private HeavyThread mHeavyThread;

    private AIThreadPool() {/*禁止外部实例化*/}

    /**
     * 算法线程池全局初始化, 进程生命周期中只需要调用一次.
     * <ol>
     *     <li>此方法 {@link #init(Context)} 可以多次调用, 不会导致重复初始化</li>
     *     <li>如果后续调用了 {@link #destory()}, 那么需要再次调用 {@link #init(Context)} 才能正常使用算法.</li>
     *     <li>如果没有特别需求, 不建议调用 {@link #destory()} 释放算法, 避免当前正在使用算法的业务出错, 或者下次需要耗时执行初始化</li>
     * </ol>
     */
    public synchronized void init(Context context) {
        if (sAlreadyInit) {
            Log.w(TAG, "init() called, but already init, nothing to do");
            return;
        }
        sAlreadyInit = true;

        AssetManager assetManager = context.getAssets();

        // 算法初始化，并 log 版本信息
        // 模型全局加载必须先于 SDK 实例化
        YTSDKManager.loadLibs();//加载动态库 .so
        YTSDKManager.loadModels(assetManager);//模型全局加载

        mTrackThread = new LightThread(assetManager);
        mHeavyThread = new HeavyThread(assetManager);
        mTrackThread.start();
        mHeavyThread.start();
    }

    public LightThread getLightThread() {
        if (!sAlreadyInit) {
            throw new IllegalStateException("Must call AIThreadPool.init() first.");
        }
        return mTrackThread;
    }

    public HeavyThread getHeavyThread() {
        if (!sAlreadyInit) {
            throw new IllegalStateException("Must call AIThreadPool.init() first.");
        }
        return mHeavyThread;
    }

    /**
     * 释放算法, 销毁线程池.
     * 如果没有特别需求, 不建议调用, 避免当前正在使用算法的业务出错, 或者下次需要耗时执行初始化
     */
    public synchronized void destory() {
        mHeavyThread.finish();
        mTrackThread.finish();
        mHeavyThread = null;
        mTrackThread = null;
        sAlreadyInit = false;
    }

}
