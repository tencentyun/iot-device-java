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
     * @param percent  下载进度（0 ~ 100）;
     * @param version  版本；
     */
    void onDownloadProgress(int percent, String version);

    /**
     * 资源文件下载完成回调
     * @param outputFile  已下载完成的资源文件名（包含全路径）；
     * @param version  版本；
     */
    void onDownloadCompleted(String outputFile, String version);

    /**
     * 资源文件下载失败回调
     *
     * @param errCode  失败错误码; -1: 下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
     * @param version  版本；
     */
    void onDownloadFailure(int errCode, String version);
}
