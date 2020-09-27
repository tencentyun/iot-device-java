package com.tencent.cloud.ai.fr.pipeline;

import android.support.annotation.AnyThread;

/**
 * 流水线是由多个任务串联而成的, 这就是一个任务的抽象定义
 */
public interface Task<T> {

    /**
     * 任务名字, 用于debug
     */
    String name();

    /**
     * 任务被执行时, 此方法会被回调
     * 回调线程:
     * <li>
     * <ul>如果通过 {@link AbsPipelineThread#enqueue(Object)} 提交任务, 那么此方法会被后台线程回调 </ul>
     * <ul>如果通过 {@link AbsPipelineThread#process(Object)} 提交任务, 那么此方法会被当前线程回调 </ul>
     * </li>
     * @param stuff 流水线各个任务共用的杂物箱, 存放各个任务的原料和产物
     * @return true 表示此步骤成功, 可进行下一步. false 表示此步骤失败, 流水线结束, 资源会被回收(典型地, 帧数据会被回收)
     */
    @AnyThread
  boolean onProcess(T stuff);
}
