# 优图人脸配准SDK

## 功能说明

使用人脸配准SDK，需要用户实现如下功能：

1. 使用人脸检测SDK获取人脸框坐标

## 导入优图相关lib

1. YTFaceAlignment.jar 安卓sdk包
2. arm64-v8a       armv8库
3. armeabi-v7a     armv7库
4. ace-align-v1.0.0 配准模型

## 主要接口说明

### 流程调用图

![流程调用](../人脸配准SDK调用流程.png)

#### 说明

1. 多张图片或者多人使用同一个配准实例时需要将align接口的isFirstFrame参数设置true
