package com.tencent.cloud.ai.fr.pipeline;

import android.support.annotation.AnyThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/** 流水线处理任务的抽象定义, 由 物料箱{@link AbsJob#stuffBox} 和 流水线{@link AbsJob#pipeline} 组成 */
public class AbsJob<T> {

    private static final String TAG = "AbsJob";

    /** 流水线运行过程中, 存储输入&输出资料的物料箱, 请根据业务逻辑自行定义 */
    public final T stuffBox;
    
    /** 流水线 */
    public final List<AbsStep<T>> pipeline;

    /** @see AbsJob */
    public AbsJob(T stuffBox, List<AbsStep<T>> pipeline) {
        this.stuffBox = stuffBox;
        this.pipeline = new ArrayList<>(pipeline);
    }

    /**
     * 执行此任务<br/>
     * <ol>
     *     <li> 被 {@link AbsJob#pipeline } 中指定的工作线程线程调用. </li>
     *     <li> 也可以在当前线程直接调用此方法, 那么就变成同步调用. </li>
     * </ol>
     * @return 是否执行成功, 如果返回 false 会导致流水线中断并回收资源. <br/>
     */
    public boolean run() {
        StringBuilder msg = new StringBuilder("runningPipeline(): ");

        for (AbsStep<T> step : pipeline) {
            boolean shouldContinue = step.onProcess(stuffBox);
            msg.append(step.name()).append(" ");
            if (!shouldContinue) {//任务请求结束流水线
                Log.v(TAG, msg.append("<< pipeline end").toString());
                return false;
            }
        }
        Log.v(TAG, msg.toString());

        return true;
    }

    /**
     * 本次处理任务被回收时会触发此方法, 多数情况下你需要回收 物料箱{@link AbsJob#stuffBox} 中的内容或资源
     * <p>
     * 回调线程可能有:
     * <ul>
     *     <li>如果此对象(本次处理任务)还没被处理就被回收, 那么回调线程就是调用 {@link AbsPipelineThread#enqueue(com.tencent.cloud.ai.fr.pipeline.AbsJob)} 的线程</li>
     *     <li>如果此对象已经被处理, 那么回调线程是流水线指定的工作线程</li>
     * </ul>
     * </p>
     */
    @AnyThread
    protected void recycle() {/*nothing*/}

}
