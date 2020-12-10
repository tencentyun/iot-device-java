package com.tencent.iot.hub.device.java.core.mqtt;

public interface TXOTACallBack {
    /**
     * 上报固件版本回调
     *
     * @param resultCode  上报结果码；0：成功；其它：失败
     * @param version  版本；
     * @param resultMsg  上报结果码描述
     */
    void onReportFirmwareVersion(int resultCode, String version, String resultMsg);

    /**
     * OTA有新的版本可以升级
     *
     * @param url  文件 url 用于下载最新版本
     * @param md5  md5 值用于校验
     * @param version  最新版本号
     */
    boolean onLastestFirmwareReady(String url, String md5, String version);

    /**
     * OTA升级包下载进度回调
     *
     * @param percent  下载进度（0 ~ 100）;
     * @param version  版本；
     */
    void onDownloadProgress(int percent, String version);

    /**
     * OTA升级包下载完成回调
     * @param outputFile  已下载完成的升级包文件名（包含全路径）；
     * @param version  版本；
     */
    void onDownloadCompleted(String outputFile, String version);

    /**
     * OTA升级包下载失败回调
     *
     * @param errCode  失败错误码; -1: 下载超时; -2:文件不存在；-3:签名过期；-4:校验错误；-5:更新固件失败
     * @param version  版本；
     */
    void onDownloadFailure(int errCode, String version);
}
