# Demo 架构设计简介

## 1、核心功能

1、注册
2、检索

## 2、Demo演示功能模块
1、1:N 注册(图片文件) => RegWithFileActivity
2、1:N 搜索(Android相机) => RetrieveWithAndroidCameraActivity


## 3、核心线程问题

1. `LightThread` 轻量任务流水线, 主要运行实时性要求高的任务, 例如人脸检测, 快速筛选
2. `HeavyThread` 重型任务线程, 对实时性要求不高的任务建议在这个线程里执行

## 4、整体架构

Demo运行由物料(帧相关数据)、流水线步骤(具体需要做哪些操作)、工作线程(HeavyThread（重型）和LightThread（轻量级）)
三大部分组成
线程(Thread):处理线程(LightThread/LightThread)
物料(StuffBox):帧数据或者本地图片
流水线步骤(xxxxStep):提取特征、人脸搜索......

Demo中实现线程和流水线分离原则,流水线与线程解耦, 与任务结合,也就避免核心线程(LightThread/HeavyThread)多次线程创建销毁
同时流水线可以自定义组建，Demo已有的流水线有: 人脸检索流水线、人脸注册流水线、提取特征流水线、比对流水线、恢复人脸数据流水线等

具体流程为: 组装流水线==>转到线程中执行 (流水线的处理结果存储在容器中作为后面流水线步骤的输入)

## 5、同步异步的问题

同步异步是针对流水线任务

异步:存在丢帧，用于实时相机帧

> private PipelineBuilder xxxxxx = new PipelineBuilder() ...

同步:处理每一帧数据，用于类似本地照片的处理场景

> private List<AbsStep<StuffBox>> xxxxxx = new SyncJobBuilder() ...

## 6、XXXXStep

流水线是由多个步骤串联而成的, 这个就是具体步骤的实现，所有的Step继承AbsStep，其中的onProcess会在工作线程(LightThread/HeavyThread)中回调
同时xxxxStep分为 heavystep 和 lightstep，也就是轻量级(实时性要求不高)和重量级（实时性要求比较高）的步骤。每个Step通过StuffId来实现处理结果的传递

同步方式:new SyncJobBuilder().addStep
异步方式:new PipelineBuilder().addStep 通过onThread切换线程

其中一个Step执行失败，数据就会被回收，后面的Step就不会执行

提示:用户可自定义XXXXStep，并不局限现有的Step

## 7、Job

Demo中的job包中包含AsyncJobBuilder和SyncJobBuilder。
AsyncJobBuilder是异步容器箱子
SyncJobBuilder是异步容器箱子
两者都是XXXXStep的容器，也就是组装后的流水线，此时只是流水线步骤以及相关配置的集合

## 8、整体流程

相机帧 or 本地照片  +  流水线步骤   ==>   线程(轻量级/重型)   ==>   回调结果

## 9、线程池问题

Demo采用类似线程池的方式处理工作线程，但是这个并不是Android里面的线程池，而是一个全局的线程容器
里面永久的维持 HeavyThread（重型）和LightThread（轻量级），也就是说这两个线程在程序运行后就只会创建一次。后期的
流水线都是在这两个线程中工作的

## 10、使用流程

1、初始化StuffBox物料箱，也就是初始帧等数据
2、确定选择是同步流水线还是异步流水线(相机视频流是异步)
3、组装流水线AsyncJobBuilder(或者SyncJobBuilder)，选择需要的xxxxStep，也可以自定义Step
4、执行任务

## 11、原子能力库版本

    YTFaceQualityPro    Version: v1.3.3-v205
    YTFaceRetrieve      Version: v2.3.4
    mYTFaceFeature      Version: v4.1.1-v705
    mYTFaceLive3D       Version: v4.2.0-v410
    mYTFaceLiveIR       Version: v4.4.9-v202
    mYTFaceLiveColor    Version: v2.2.5-v124
    mYTFaceQuality      Version: v2.6.0-v120
    YTFaceTracker       Version: v1.2.9-v535_v420
    mYTFaceFeature      Version: v4.1.1-v705
    YTFaceAlign         Version: v1.0.0-v6.3.0

## 11、注意事项

1、 1)异步接口, 添加原材料到后台流水线线程处理, 因为有丢弃策略, 适用于实时处理照相机帧流的场景, 不建议用于处理照片
    丢弃策略:
    如果添加AbsJob的速度超出后台线程处理能力, 之前已添加但未处理的 job 会被放弃并调动recycle回收,如:
    if (mPendingJob != null) {//如果不为null, 表示未被使用, 需要回收
        mPendingJob.recycle();
    }
    也就是说会保证队列中的AbsJob是最新的.
    2)同步调用 同步调用就不存在丢帧的问题,每一帧都会进行处理,建议处理照片文件