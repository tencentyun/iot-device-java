package com.tencent.cloud.ai.fr.pipeline;

import android.os.Process;
import android.support.annotation.AnyThread;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 原材料处理流水线抽象线程
 */
public abstract class AbsPipelineThread<T> extends Thread {

    private final String TAG;

    private volatile T mWorkingStuff;
    private volatile List<Task<T>> mPendingPipeline;

    private volatile boolean isRunning = true;
    private volatile boolean isPaused = false;

    private final Object mThreadPauseLock = new Object();
    private final Object mWorkingStuffRefreshLock = new Object();

    protected AbsPipelineThread(String threadName) {
        setName(threadName + "-" + getId());
        TAG = getName();
    }

    /**
     * 异步接口, 添加原材料到后台流水线线程处理, 因为有丢弃策略, 适用于实时处理照相机帧流的场景, 不建议用于处理照片或视频文件
     * <p>
     * 注意, 丢弃策略: <br/>
     * 如果添加 stuff 的速度超出后台线程处理能力, 之前已添加但未处理的 stuff 会被放弃并调动 {@link #onRecycleWorkingStuff(Object)} 回收. <br/>
     * 也就是说会保证队列中的 stuff 是最新的.
     * </p>
     * @see #process(Object)
     */
    public void enqueue(T stuff) {
        synchronized (mWorkingStuffRefreshLock) {
            if (mWorkingStuff != null) {//如果不为null, 表示未被使用, 需要回收
                onRecycleWorkingStuff(mWorkingStuff);
            }
            mWorkingStuff = stuff;
        }
        resumeThreadIfNeeded();
    }

    /**
     * 同步接口: 立即通过流水线处理原材料 stuff
     * <ol>
     * <li>没有丢弃策略, 每一个 stuff 都会被处理, 所以不建议用于对实时性要求较高的场景, 例如处理相机预览帧</li>
     * <li>在当前线程执行, 因此会阻塞直到处理完成.</li>
     * </ol>
     * @see #enqueue(Object)
     */
    public void process(T stuff) {
        if (mPendingPipeline == null) {
            throw new NullPointerException("forget to call AbsPipelineThread.switchToPipeline ?");
        }
        runPipeline(mPendingPipeline, stuff);
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);//降低一下线程优先级
        Log.i(TAG, getName() + " started.");
        onThreadStart();

        while (isRunning) {
            List<Task<T>> runningPipeline = mPendingPipeline;//防止运行过程中流水线发生变化

            if (runningPipeline == null || runningPipeline.isEmpty()) {// 如果流水线未配置(未调用switchToPipeline)或为空, 则等待.
                Log.w(TAG, getName() + " No task found in pipeline, wait...");
                pauseThread();
                continue;
            }
            
            T stuff;
            synchronized (mWorkingStuffRefreshLock) {
                stuff = mWorkingStuff;
                mWorkingStuff = null;//表示已使用
            }

            if (stuff != null) {
                boolean shouldContinue = runPipeline(runningPipeline, stuff);
                if (!shouldContinue) {
                    onRecycleWorkingStuff(stuff);//回收原料
                }
            } else {//如果无原材料, 则暂停线程
                pauseThread();
            }
        }
        
        onThreadFinish();
        Log.i(TAG, getName() + " finished.");
    }

    /**
     * 把 stuff 交给 pipeline 进行处理.
     * @return stuff 是否会被继续处理, 如果 false, 则不要回收 stuff
     */
    @AnyThread
    // synchronized: 避免 enqueue(T stuff) 与 process(T stuff) 同时执行 
    private synchronized boolean runPipeline(List<Task<T>> pipeline, T stuff) {
        // 按顺序执行各个任务
        StringBuilder msg = new StringBuilder("runPipeline(): ");
        for (Task<T> task : pipeline) {
            boolean shouldContinue = task.onProcess(stuff);
            msg.append(task.name()).append(" ");
            if (!shouldContinue) {//任务请求中断流水线
                Log.v(TAG, msg.toString());
                return false;
            }
        }
        Log.v(TAG, msg.toString());
        return true;
    }

    protected void switchToPipeline(List<Task<T>> pipeline) {
        if (pipeline == null || pipeline.isEmpty()) {
            throw new IllegalArgumentException("pipeline is null or empty !!");
        }

        StringBuilder sb = new StringBuilder();
        for (Task<T> t : pipeline) {
            sb.append(t.name()).append(" ");
        }
        Log.d(TAG, "switchToPipeline() called with: pipeline = [ " + sb + "]");
        
        mPendingPipeline = new ArrayList<>(pipeline);
        resumeThreadIfNeeded();
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
                Log.v(TAG, getName() + " paused.");
                mThreadPauseLock.wait();
                Log.v(TAG, getName() + " resumed after " + (System.currentTimeMillis() - start) + "ms");
            } catch (InterruptedException e) {
                Log.v(TAG, getName() + " resumed by interruption after " + (System.currentTimeMillis() - start) + "ms");
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

    /**
     * 回收原材料数据, 当以下情况发生时, 此方法会被调用:<br/>
     * <ol>
     * <li>原材料没有来得及被处理, 直接被回收</li>
     * <li>原材料被处理了, 但是如果 {@link Task#onProcess(Object)} 返回 false, 则停止处理, 并被回收</li>
     * </ol>
     * 相应地, 可能被以下线程调用:<br/>
     * <ol>
     * <li>调用 {@link #enqueue(Object)} 的线程</li>
     * <li>调用 {@link #enqueue(Object)} 的线程</li>
     * <li>{@link AbsPipelineThread} 线程本身调用.</li>
     * </ol>
     */
    @AnyThread
    protected abstract void onRecycleWorkingStuff(T stuff);
}
