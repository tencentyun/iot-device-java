package com.tencent.iot.explorer.device.face.business.job;

import com.tencent.cloud.ai.fr.pipeline.AbsJob;
import com.tencent.cloud.ai.fr.pipeline.AbsPipelineThread;
import com.tencent.cloud.ai.fr.pipeline.AbsStep;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class AsyncJobBuilder {

    private final StuffBox mStuffBox;
    private PipelineBuilder mPipelineBuilder;

    public AsyncJobBuilder(StuffBox stuffBox, PipelineBuilder pipelineBuilder) {
        mStuffBox = stuffBox;
        mPipelineBuilder = pipelineBuilder;
    }

    /**
     * 1. 把多个流水线拼接成一条嵌套结构的流水线 2. 把物料箱融合进去
     */
    public SynthesizedJob synthesize() {
        return mPipelineBuilder.synthesize(mStuffBox);
    }
    
    public static class SynthesizedJob{

        private AbsJob<StuffBox> mJob;
        private AbsPipelineThread<StuffBox> mLaunchThread;

        public SynthesizedJob(AbsJob<StuffBox> job, AbsPipelineThread<StuffBox> launchThread) {
            this.mJob = job;
            this.mLaunchThread = launchThread;
        }

        public void launch(){
            mLaunchThread.enqueue(mJob);
        }
    }

    public static class PipelineBuilder {

        private LinkedList<ThreadPipelinePair> mPairs = new LinkedList<>();
        private ThreadPipelinePair mCurrentPair;

        public PipelineBuilder onThread(AbsPipelineThread<StuffBox> thread) {
            if (mCurrentPair != null) {
                throw new IllegalStateException("You must call PipelineBuilder.submit() before calling PipelineBuilder.onThread()");

            }
            mCurrentPair = new ThreadPipelinePair(thread);
            return this;
        }

        public PipelineBuilder addStep(AbsStep<StuffBox> step) {
            if (mCurrentPair == null) {
                throw new IllegalStateException("You must call PipelineBuilder.onThread() before calling PipelineBuilder.addTask()");
            }
            mCurrentPair.appendStep(step);
            return this;
        }

        public PipelineBuilder submit() {
            if (mCurrentPair != null) {
                mPairs.add(mCurrentPair);
            }
            mCurrentPair = null;
            return this;
        }

        /**
         * 1. 把多个流水线拼接成一条嵌套结构的流水线 2. 把物料箱融合进去
         */
        private SynthesizedJob synthesize(StuffBox stuffBox) {
            AbsPipelineThread<StuffBox> launchThread = null;

            List<AbsStep<StuffBox>> headPipeline = new ArrayList<>();
            List<AbsStep<StuffBox>> tailPipeline = new ArrayList<>();

            if (mPairs == null || mPairs.size() == 0) {
                throw new NullPointerException("No Jobs Found");
            }

            ListIterator<ThreadPipelinePair> i = mPairs.listIterator();
            ConcatStep mLastConcatTask = null;

            while (i.hasNext()) {
                int index = i.nextIndex();
                final ThreadPipelinePair pair = i.next();

                if (index == 0) {
                    launchThread = pair.thread;
                    headPipeline.addAll(pair.pipeline);
                    tailPipeline = headPipeline;
                } else {// 2 个线程之间以 ConcatStep 作为转接
                    if (mLastConcatTask == null) {
                        mLastConcatTask = new ConcatStep(stuffBox, pair.thread, pair.pipeline);
                        headPipeline.add(mLastConcatTask);
                    } else {
                        mLastConcatTask = mLastConcatTask.concat(stuffBox, pair.thread, pair.pipeline);
                    }
                    tailPipeline = mLastConcatTask.mNextPipeline;
                }
            }

            //任务链最后必须添加一个返回值为 false 的 step 以结束流水线 
            tailPipeline.add(new AbsStep<StuffBox>() {

                @Override
                public String name() {
                    return "EndTask";
                }

                @Override
                protected boolean onProcess(StuffBox stuffBox) {
                    return false;//返回 false 以中断流水线
                }
            });

            AbsJob<StuffBox> job = new AbsJob<StuffBox>(stuffBox, headPipeline) {
                @Override
                protected void recycle() {
                    stuffBox.recycle();
                }
            };

            return new SynthesizedJob(job, launchThread);
        }

        private static class ConcatStep extends AbsStep<StuffBox> {

            final StuffBox mStuff;
            final AbsPipelineThread<StuffBox> mNextThread;
            final List<AbsStep<StuffBox>> mNextPipeline;

            ConcatStep(StuffBox stuff, AbsPipelineThread<StuffBox> nextThread, List<AbsStep<StuffBox>> nextPipeline) {
                mStuff = stuff;
                mNextThread = nextThread;
                mNextPipeline = nextPipeline;
            }

            ConcatStep concat(StuffBox stuff, AbsPipelineThread<StuffBox> nextThread, List<AbsStep<StuffBox>> nextPipeline) {
                ConcatStep concatTask = new ConcatStep(stuff, nextThread, nextPipeline);
                mNextPipeline.add(concatTask);
                return concatTask;
            }

            @Override
            protected boolean onProcess(StuffBox stuffBox) {
                AbsJob<StuffBox> nextJob = new AbsJob<StuffBox>(mStuff, mNextPipeline) {
                    @Override
                    protected void recycle() {
                        this.stuffBox.recycle();
                    }
                };
                mNextThread.enqueue(nextJob);
                return true;
            }
        }

        private static class ThreadPipelinePair {

            final AbsPipelineThread<StuffBox> thread;
            final List<AbsStep<StuffBox>> pipeline = new ArrayList<>();

            ThreadPipelinePair(AbsPipelineThread<StuffBox> thread) {
                this.thread = thread;
            }

            void appendStep(AbsStep<StuffBox> step) {
                pipeline.add(step);
            }
        }

    }

}
