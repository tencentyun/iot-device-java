# 人脸识别能力简介
人脸识别不是单一的 AI 能力， 一般来说， 涉及以下细分能力：
 - 人脸追踪（YTFaceTracker）： 快速检测出画面中的人脸， 获得 [人脸大小， 位置， 角度]， 并由此可以进行初步筛选。
 - 人脸精确配准（YTFaceAlignment）： 【注: 在戴口罩的场景下, 由于脸部特征缺失, 不建议使用此能力】获得人脸关键点坐标， 以及关键点可见度， 可用于遮挡判断， 表情判断。
 - 人脸质量（YTFaceQuality）： 人脸图片质量打分。
 - 人脸质量归因（YTFaceQualityPro）： 【注: 在戴口罩的场景下, 由于脸部特征缺失, 不建议使用此能力】获得人脸的 [角度，遮挡，模糊，光照] 几个维度的分析结果， 用于进一步筛选。
 - 彩色图活体（YTFaceLiveColor）： 分析彩色图， 判断图中的人是否活体。 （防止照片， 显示屏等欺骗手段）
 - 红外活体（YTFaceLiveIR）： 红外摄像头获得的图像与彩色摄像头获得的图像一起分析， 判断摄像头前面的人是否活体。
 - 深度活体（YTFaceLive3D）： 深度摄像头获得的图像与彩色摄像头获得的图像一起分析， 判断摄像头前面的人是否活体。
 - 人脸提特征（YTFaceFeature）： 提取人脸特征， 并可进行人脸 1：1 比对（比较 2 个人脸相似度， 判断是否同一人）， 提特征同时也是 1：N 检索的前置条件。
 - 人脸 1：N 检索（YTFaceRetrieval）： 从人脸库中找出与给定的人脸最相似的若干个结果， 判断给定的人脸是谁。此外还要人脸库管理功能（增删查改）。
    > 注意：
    > - 彩色图活体：不推荐用于需要严格校验的场景，单目活体防御能力有限，可实际测试是否满足业务要求。
    > - 红外活体：依赖红外摄像头硬件，手机上无法使用。
    > - 深度活体：依赖深度图摄像头硬件，手机上无法使用。
# 快速体验 demo
## 1. 获取 SDK 授权，并在 demo 中填入授权信息 【重要】
- 从 [人脸识别控制台](https://console.cloud.tencent.com/aiface/sdk) 获取 `APPID` 和 `SECRET_KEY`
- 找到 `AuthActivity.java` 文件 `onAllPermissionGranted()` 方法, 填上 `APPID` 和 `SECRET_KEY`.  
    ```java
    AuthResult authResult = auth(AuthActivity.this, 
        "123456"/*修改APPID为实际的值*/, 
        "Y7QinfHe6CF3bsuqV6vTr00"/*修改SECRET_KEY为实际的值*/);
    ```
    > 详细请参考 `demo/lib-face/接口文档/YTCommon-Android-v1.4.1/授权说明.md` 文档。

## 2. 编译，运行，体验 demo
使用 `Android Studio` 打开 `demo/build.gradle` 文件，即可编译并运行 demo 。

# 业务流程介绍
人脸识别业务常见可以分为 1. `判断是否有人`，2. `判断是否同一人`，3. `找出某人是谁` 3 种类型。  

## 1. 判断是否有人
`人脸追踪(YTFaceTracker)` 即可完成。
> SDK 调用流程：
> ```
> ==> 授权（YTCommon）
> ==> 人脸追踪（YTFaceTracker） 
> ```

## 2. 判断是否同一人
> SDK 调用流程：
> ```
> ==> 授权（YTCommon）
> ==> 人脸追踪（YTFaceTracker） 
>     ==> [可选] 人脸精确配准（YTFaceAlignment 
>     ==> [可选] 人脸质量（YTFaceQuality） 
>     ==> [可选] 人脸质量归因（YTFaceQualityPro） 
>     ==> [可选] 彩色图活体（YTFaceLiveColor） 
>     ==> [可选] 红外活体（YTFaceLiveIR） 
>     ==> [可选] 深度活体（YTFaceLive3D）
> ==> 人脸提特征（YTFaceFeature） => 特征 1:1 比对
> ```

## 3. 判断某人是谁
首先需要把人脸注册入库：
> SDK 调用流程：
> ```
> ==> 授权（YTCommon）
> ==> 人脸追踪（YTFaceTracker） 
>     ==> [可选] 人脸精确配准（YTFaceAlignment 
>     ==> [可选] 人脸质量（YTFaceQuality） 
>     ==> [可选] 人脸质量归因（YTFaceQualityPro） 
> ==> 人脸提特征（YTFaceFeature） 
> ==> 人脸注册入库（YTFaceRetrieval） 
> ```
然后进行人脸检索：
> SDK 调用流程：
> ```
> ==> 授权（YTCommon）
> ==> 人脸追踪（YTFaceTracker） 
>     ==> [可选] 人脸精确配准（YTFaceAlignment 
>     ==> [可选] 人脸质量（YTFaceQuality） 
>     ==> [可选] 人脸质量归因（YTFaceQualityPro） 
>     ==> [可选] 彩色图活体（YTFaceLiveColor） 
>     ==> [可选] 红外活体（YTFaceLiveIR） 
>     ==> [可选] 深度活体（YTFaceLive3D）
> ==> 人脸提特征（YTFaceFeature） 
> ==> 人脸 1：N 检索（YTFaceRetrieval） 
> ```

demo 默认展示 `人脸检索` 的全部流程。

# 如何集成 SDK 到你的工程 (不是demo工程)

## 1. 复制 demo/lib-face 到你的工程

## 2. 引入 lib-face 
`你的工程/settings.gradle` 文件:
```groovy
include ':lib-face'             // [必选] 人脸识别 SDK
include ':lib-camera-android'   // [可选] 普通 Android 相机, 一般适用于手机自带的摄像头
include ':lib-camera-imi'       // [可选] 外接的华捷艾米摄像头, 可支持彩色图, 红外图, 深度图
```

`你的工程/build.gradle` 文件:
```groovy
android {
    defaultConfig {
     	applicationId "com.example.myapp" // 必须修改为申请授权的 apk 包名
 	}
    packagingOptions{
        // 必须添加这一句 doNotStrip !! 
        // 否则会报错：empty/missing DT_HASH/DT_GNU_HASH in ".../lib/arm64/libYTCommon.so"
        doNotStrip "**/libYTCommon.so"
    }
}

dependencies {
    implementation project(':lib-face')             // [必选] 人脸识别 SDK
    implementation project(':lib-camera-android')   // [可选] 普通 Android 相机, 一般适用于手机自带的摄像头
    implementation project(':lib-camera-imi')       // [可选] 外接的华捷艾米摄像头, 可支持彩色图, 红外图, 深度图
}
```

## 3. 调用 SDK 接口
具体功能介绍以及接口调用方式， 请参见每个 `lib-face/接口文档` 目录下的文档。
