package com.tencent.cloud.ai.fr.pipeline;

import android.support.annotation.WorkerThread;
import android.util.Log;

/**
 * 专门运行 {@link AbsJob} 的抽象线程, 功能:
 * <ol>
 *  <li> 单元素队列(队列中 {@link AbsJob} 数量最多为1) </li>
 *  <li> 当队列超出容量时, 旧  {@link AbsJob} 会被丢弃, 并回调 {@link AbsJob#recycle()} 方法 </li>
 *  <li> 当然了, 还有最基本的功能: 提供一个异步线程环境来运行 {@link AbsJob} </li>
 * </ol>
 * 如果你不需要以上功能, 那么可以不依赖此线程类, 直接调用 {@link AbsJob#run()} 即可. 
 */
public abstract class AbsPipelineThread<T> extends Thread {

    private volatile AbsJob<T> mPendingJob;

    private volatile boolean isRunning = true;
    private volatile boolean isPaused = false;

    private final Object mThreadPauseLock = new Object();
    private final Object mPendingJobRefreshLock = new Object();

    protected AbsPipelineThread(String threadName) {
        setName(threadName + "-" + getId());
    }

    /**
     * 异步接口, 添加原材料到后台流水线线程处理, 因为有丢弃策略, 适用于实时处理照相机帧流的场景, 不建议用于处理照片或视频文件
     * <p>
     * 注意, 丢弃策略: <br/>
     * 如果添加 {@link AbsJob} 的速度超出后台线程处理能力, 之前已添加但未处理的 job 会被放弃并调动 {@link AbsJob#recycle()} 回收. <br/>
     * 也就是说会保证队列中的 {@link AbsJob} 是最新的.
     * </p>
     */
    public void enqueue(AbsJob<T> job) {
        synchronized (mPendingJobRefreshLock) {
            if (mPendingJob != null) {//如果不为null, 表示未被使用, 需要回收
                mPendingJob.recycle();
            }
            mPendingJob = job;
        }
        resumeThreadIfNeeded();
    }

    @Override
    public void run() {
        // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);//降低一下线程优先级
        Log.i(getName(), " started.");
        onThreadStart();

        while (isRunning) {
            AbsJob<T> job;
            synchronized (mPendingJobRefreshLock) {
                job = mPendingJob;
                mPendingJob = null;//表示已使用
            }

            if (job != null) {
                boolean shouldContinue = job.run();
                if (!shouldContinue) {
                    job.recycle();//回收原料
                }
            } else {//如果无原材料, 则暂停线程
                pauseThread();
            }
        }

        onThreadFinish();
        Log.i(getName(), " finished.");
    }

    /** 线程启动回调, 被本线程本身调用 */
    @WorkerThread
    protected void onThreadStart(){/*子类必要时重写*/}
    
    /** 线程结束回调, 被本线程本身调用 */
    @WorkerThread
    protected void onThreadFinish(){/*子类必要时重写*/}

    private void pauseThread() {
        synchronized (mThreadPauseLock) {
            isPaused = true;
            long start = System.currentTimeMillis();
            try {
                Log.v(getName(), " paused.");
                mThreadPauseLock.wait();
                Log.v(getName(), " resumed after " + (System.currentTimeMillis() - start) + "ms");
            } catch (InterruptedException e) {
                Log.v(getName(), " resumed by interruption after " + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    private void resumeThreadIfNeeded() {
        if (isPaused) {
            synchronized (mThreadPauseLock) {
                if (isPaused) {
                    isPaused = false;
                    mThreadPauseLock.notify();
                }
            }
        }
    }

    /** 通知此线程完成当前任务后退出, 如果当前正在等待, 则中断等待然后退出. */
    public void finish() {
        if (!isAlive()) {
            new RuntimeException("Thread not start yet, nothing to do with finish()").printStackTrace();
            return;
        }
        isRunning = false;
        interrupt();//中断可能正在进行的等待
    }
}
