package com.tencent.cloud.ai.fr.pipeline;

import android.support.annotation.WorkerThread;

/**
 * 流水线是由多个步骤串联而成的, 这就是一个步骤的抽象定义
 */
public abstract class AbsStep<T> {

    /** 步骤名称, 用于 debug */
    public String name() {
        return getClass().getSimpleName();
    }

    /**
     * 此步骤被执行时, 此方法会被工作线程回调.<br/>
     * @param stuffBox 流水线各个步骤共用的杂物箱, 存放各个步骤所需的的原料和产物
     * @return true 表示此步骤成功, 可进行下一步. false 表示此步骤不成功, 请求中断流水线并回收资源(典型地, 帧数据会被回收)
     */
    @WorkerThread
    protected abstract boolean onProcess(T stuffBox);

}
