package com.tencent.iot.explorer.device.face.resource;

import org.json.JSONArray;

public interface TXResourceCallBack {
    /**
     * 上报资源版本信息回调
     *
     * @param resultCode  上报结果码；0：成功；其它：失败
     * @param resourceList  JSONArray内部装载 {"resource_name": "audio_woman_mandarin", "version": "1.0.0", "resource_type": "FILE"},此格式的JSONObject
     * @param resultMsg  上报结果码描述
     */
    void onReportResourceVersion(int resultCode, JSONArray resourceList, String resultMsg);

    /**
     * 资源文件有新的版本可以升级
     *
     * @param url  文件 url 用于下载最新版本
     * @param md5  md5 值用于校验
     * @param version  最新版本号
     */
    boolean onLastestResourceReady(String url, String md5, String version);

    /**
     * 资源文件下载进度回调
     *
     * @param resourceName  人脸库资源文件名称 或 资源文件名称，不含路径，包含featureId和文件格式;
     * @param percent  下载进度（0 ~ 100）;
     * @param version  版本；
     */
    void onDownloadProgress(String resourceName, int percent, String version);

    /**
     * 资源文件下载完成回调
     * @param outputFile  已下载完成的资源文件名（包含全路径）；
     * @param version  版本；
     */
    void onDownloadCompleted(String outputFile, String version);

    /**
     * 资源文件下载失败回调
     *
     * @param resourceName  人脸库资源文件名称 或 资源文件名称，不含路径，包含featureId和文件格式;
     * @param errCode  失败错误码; -1: 下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
     * @param version  版本；
     */
    void onDownloadFailure(String resourceName, int errCode, String version);

    /**
     * 删除特征回调
     *
     * @param featureId     特征Id   featureId
     * @param resourceName  资源文件名称，不含路径，包含featureId和文件格式
     */
    void onFeatureDelete(String featureId, String resourceName);

    /**
     * 人脸库删除回调
     *
     * @param version       人脸库资源文件版本号
     * @param resourceName  人脸库资源文件名称
     */
    void onFaceLibDelete(String version, String resourceName);

    /**
     * 离线检索事件需要保存的回调
     *
     * @param feature_id    特征id，对应控制台的人员ID。
     * @param score         检索分数
     * @param sim           检索和特征的相似度
     * @param timestamp     时间戳
     */
    void onOfflineRetrievalResultEventSave(String feature_id, float score, float sim, int timestamp);
}
