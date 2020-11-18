package com.tencent.iot.explorer.device.face.business.job;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class StuffId<T> {

    private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
    
    private final int mInstanceIndex;
    private final T mDefaultStuff;

    /**
     * @param defaultStuff 提供默认值, 避免 {@link StuffBox#find(StuffId)} 无果时返回 null 导致业务需要做额外的检查.
     */
    public <S extends T> StuffId(S defaultStuff) {
        mDefaultStuff = defaultStuff;
        mInstanceIndex = INSTANCE_COUNT.getAndAdd(1);
    }

    T getDefaultStuff(){
         return mDefaultStuff;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StuffId<?> that = (StuffId<?>) o;
        return mInstanceIndex == that.mInstanceIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mInstanceIndex);
    }
}
