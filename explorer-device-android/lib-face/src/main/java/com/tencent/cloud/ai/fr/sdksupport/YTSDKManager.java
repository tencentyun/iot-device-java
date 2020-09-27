package com.tencent.cloud.ai.fr.sdksupport;

import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.youtu.YTFaceAlignment;
import com.tencent.youtu.YTFaceFeature;
import com.tencent.youtu.YTFaceLive3D;
import com.tencent.youtu.YTFaceLiveColor;
import com.tencent.youtu.YTFaceLiveIR;
import com.tencent.youtu.YTFaceQuality;
import com.tencent.youtu.YTFaceQualityPro;
import com.tencent.youtu.YTFaceRetrieval;
import com.tencent.youtu.YTFaceTracker;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class YTSDKManager {
    private static final String TAG = "YTSDKManager";

    /** 人脸特征长度：512 维，取决于 YTFaceFeature 的版本 */
    public static final int FACE_FEAT_LENGTH = YTFaceFeature.FEATURE_LENGTH;
    /** 人脸检索阈值：跟 模型版本/使用场景 相关 */
    public static final float FACE_RETRIEVE_THRESHOLD = 80.0f;

    /** 全局初始化标志位, 避免重复或错误的初始化/释放 */
    private static volatile boolean alreadyGlobalInit = false;
    /** 引用计数 */
    private static AtomicInteger sInstancesCount = new AtomicInteger(0);
    /** 当引用计数 {@link #sInstancesCount} 为 0 时是否自动调用 {@link #releaseModels()} 全局释放算法模型 */
    private static final boolean IS_AUTO_RELEASE = false;

    public YTFaceFeature mYTFaceFeature;
    public YTFaceQuality mYTFaceQuality;
    public YTFaceRetrieval mYTFaceRetriever;
    public YTFaceQualityPro mYTFaceQualityPro;
    public YTFaceLive3D mYTFaceLive3D;
    public YTFaceLiveIR mYTFaceLiveIR;
    public YTFaceLiveColor mYTFaceLiveColor;
    public YTFaceTracker mYTFaceTracker;
    public YTFaceTracker mYTIrFaceTracker;
    public YTFaceAlignment mYTFaceAlignment;

    /** 由于SDK不是线程安全的, 最好每个线程一个实例. */
    public YTSDKManager(AssetManager assetManager) {
        sInstancesCount.incrementAndGet();
        
        mYTFaceAlignment = new YTFaceAlignment();
        mYTFaceFeature = new YTFaceFeature();
        mYTFaceLive3D = new YTFaceLive3D();
        mYTFaceLiveIR = new YTFaceLiveIR();
        mYTFaceLiveColor = new YTFaceLiveColor();
        mYTFaceQuality = new YTFaceQuality();
        mYTFaceQualityPro = new YTFaceQualityPro();

        // YTFaceTracker 初始化需要指定实例的配置参数
        // 详细说明可以在 优图人脸追踪文档 下查询
        YTFaceTracker.Options options = new YTFaceTracker.Options();
        options.biggerFaceMode = true;
        options.maxFaceSize = 999999;
        options.minFaceSize = 100;
        mYTFaceTracker = new YTFaceTracker(options);
        mYTIrFaceTracker = new YTFaceTracker(options);

        // YTFaceRetrieve 初始化实例时，需要指定检索的 cvtTable 和 特征长度
        // 详细说明可以在 优图人脸检索文档 下查询
        float[] cvtTable = YTFaceRetrieval.loadConvertTable(assetManager, "models/face-feature-v704/cvt_table_1vN_704.txt");
        mYTFaceRetriever = new YTFaceRetrieval(cvtTable, FACE_FEAT_LENGTH);
    }

    public void destroy() {
        if (mYTFaceAlignment != null) {
            mYTFaceAlignment.destroy();
            mYTFaceAlignment = null;
        }
        if (mYTFaceFeature != null) {
            mYTFaceFeature.destroy();
            mYTFaceFeature = null;
        }
        if (mYTFaceTracker != null) {
            mYTFaceTracker.destroy();
            mYTFaceTracker = null;
        }
        if (mYTIrFaceTracker != null) {
            mYTIrFaceTracker.destroy();
            mYTIrFaceTracker = null;
        }
        if (mYTFaceRetriever != null) {
            mYTFaceRetriever.destroy();
            mYTFaceRetriever = null;
        }
        if (mYTFaceQuality != null) {
            mYTFaceQuality.destroy();
            mYTFaceQuality = null;
        }
        if (mYTFaceQualityPro != null) {
            mYTFaceQualityPro.destroy();
            mYTFaceQualityPro = null;
        }
        if (mYTFaceLive3D != null) {
            mYTFaceLive3D.destroy();
            mYTFaceLive3D = null;
        }
        if (mYTFaceLiveIR != null) {
            mYTFaceLiveIR.destroy();
            mYTFaceLiveIR = null;
        }
        if (mYTFaceLiveColor != null) {
            mYTFaceLiveColor.destroy();
            mYTFaceLiveColor = null;
        }

        if (sInstancesCount.get() == 0) {
            return;
        }
        
        int count = sInstancesCount.decrementAndGet();
        Log.d(TAG, "destroy() called, instancesCount=" + count);
        if (count == 0 && IS_AUTO_RELEASE) {
            releaseModels();
        }
    }

    /** 加载 .so */
    public static void loadLibs() {
        System.loadLibrary("YTUtils");
        System.loadLibrary("YTFaceFeature");
        System.loadLibrary("YTFaceRetrieval");
        System.loadLibrary("YTFaceLive3D");
        System.loadLibrary("YTFaceLiveColor");
        System.loadLibrary("YTFaceLiveIR");
        System.loadLibrary("YTFaceQuality");
        System.loadLibrary("YTFaceQualityPro");
        System.loadLibrary("YTFaceTracker");
        System.loadLibrary("YTFaceAlignment");
    }

    /** 搜索全部 .so 并加载. 效果与 {@link #loadLibs()} 一致. */
    public static void loadLibs(ApplicationInfo applicationInfo) {
        if (TextUtils.isEmpty(applicationInfo.nativeLibraryDir)) {
            throw new IllegalStateException("Can not load .so: applicationInfo.nativeLibraryDir is empty");
        }
        File nativeLibraryDir = new File(applicationInfo.nativeLibraryDir);
        if (!nativeLibraryDir.exists()) {
            throw new IllegalStateException("Can not load .so, file not exists: " + nativeLibraryDir.getAbsolutePath());
        }
        final String[] primaryNativeLibraries = nativeLibraryDir.list();
        if (primaryNativeLibraries == null || primaryNativeLibraries.length == 0) {
            throw new IllegalStateException("No .so file found, please check if the files are copied correctly.");
        }
        Log.d(TAG, "Load .so from: " + nativeLibraryDir.getAbsolutePath());
        for (String libFileName : primaryNativeLibraries) {
            int end = libFileName.lastIndexOf(".so");
            String libName = libFileName.substring(3/*"lib"*/, end);
            System.loadLibrary(libName);
            Log.d(TAG, "Loaded " + libFileName);
        }
        Log.d(TAG, "Load .so finished");
    }

    public static void loadModels(AssetManager assetManager) {
        // 防止多次全局初始化
        if (alreadyGlobalInit) {
            Log.d(TAG, "loadModels() called, alreadyGlobalInit == true, nothing to do.");
            return;
        }
        alreadyGlobalInit = true;
        Log.d(TAG, "loadModels() called with: assetManager = [" + assetManager + "]");
        
        // 加载模型文件：输入 `assets` 目录下模型配置文件的目录和 config 文件名，例如 `models/v7114/config.ini`
        // 请输入模型文件的 `config.ini` 所在 assets 下的相对路径，和 `config.ini` 文件名。
        // 请确保 `config.ini` 文件与模型文件处于同一目录下
        // globalInit 需要判断返回值是否成功，具体接口内容请查看提供的 docs/api.md 文件

        int result;

        result = YTFaceAlignment.globalInit(assetManager, "models/face-align-v6.3.0", "config.ini");
        Log.d(TAG, "YTFaceAlign Version: " + YTFaceAlignment.getVersion() + "; global result = " + result);

        result = YTFaceFeature.globalInit(assetManager, "models/face-feature-v704", "config.ini");
        Log.d(TAG, "YTFaceFeature Version: " + YTFaceFeature.getVersion() + "; global result = " + result);

        result = YTFaceLive3D.globalInit(assetManager, "models/face-live-3d-v300", "config.ini");
        Log.d(TAG, "YTFaceLive3D Version: " + YTFaceLive3D.getVersion() + "; global result = " + result);

        result = YTFaceLiveColor.globalInit(assetManager, "models/face-live-color-v124", "config.ini");
        Log.d(TAG, "YTFaceLiveColor Version: " + YTFaceLiveColor.getVersion() + "; global result = " + result);

        result = YTFaceLiveIR.globalInit(assetManager, "models/face-live-ir-v201", "config.ini");
        Log.d(TAG, "YTFaceLiveIR Version: " + YTFaceLiveIR.getVersion() + "; global result = " + result);

        result = YTFaceQuality.globalInit(assetManager, "models/face-quality-v111", "config.ini");
        Log.d(TAG, "YTFaceQuality Version: " + YTFaceQuality.getVersion() + "; global result = " + result);

        result = YTFaceQualityPro.globalInit(assetManager, "models/face-quality-pro-v201", "config.ini");
        Log.d(TAG, "YTFaceQualityPro Version: " + YTFaceQualityPro.getVersion() + "; global result = " + result);

        result = YTFaceTracker.globalInit(assetManager, "models/face-tracker-v5.3.5+v4.1.0", "config.ini");
        Log.d(TAG, "YTFaceTracker Version: " + YTFaceTracker.getVersion() + "; global result = " + result);

        // YTFaceRetrieve 不包含模型，只包含具体的检索算法
        Log.d(TAG, "YTFaceRetrieve Version: " + YTFaceRetrieval.getVersion());
    }

    /**
     * 释放全局模型, 如果进程内存不是特别紧张, 可以不释放.
     */
    private static void releaseModels() {
        // 防止多次释放
        if (!alreadyGlobalInit) {
            Log.d(TAG, "releaseModels() called, alreadyGlobalInit == false, no need to release.");
            return;
        }
        alreadyGlobalInit = false;
        Log.d(TAG, "releaseModels() called");
        
        YTFaceFeature.globalRelease();
        YTFaceLiveColor.globalRelease();
        YTFaceLive3D.globalRelease();
        YTFaceLiveIR.globalRelease();
        YTFaceQuality.globalRelease();
        YTFaceQualityPro.globalRelease();
        YTFaceTracker.globalRelease();
        YTFaceAlignment.globalRelease();
    }
}
