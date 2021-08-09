package com.tencent.iot.explorer.device.face.consts;

public class Common {

    /**
     * 事件ID
     */
    public static final String EVENT_UPDATE_RESULT_REPORT = "update_result_report";

    /**
     * 事件类型
     */
    public static final String EVENT_TYPE_INFO = "info";

    /**
     * 资源名称
     */
    public static final String PARAMS_RESOURCE_NAME = "resource_name";

    /**
     * 资源版本
     */
    public static final String PARAMS_VERSION = "version";

    /**
     * 特征ID
     */
    public static final String PARAMS_FEATURE_ID = "feature_id";

    /**
     * 更新结果
     */
    public static final String PARAMS_UPDATE_RESULT = "update_result";

    /**
     * 人脸资源下载成功，待注册
     */
    public static final int RESULT_DOWNLOAD_SUCCESS = 1;

    /**
     * 人脸资源下载失败
     */
    public static final int RESULT_DOWNLOAD_FAIL = 2;

    /**
     * 人脸资源注册成功
     */
    public static final int RESULT_REGISTER_SUCCESS = 3;

    /**
     * 人脸资源注册失败
     */
    public static final int RESULT_REGISTER_FAIL = 4;

    /**
     * 人脸资源删除成功
     */
    public static final int RESULT_DELETE_SUCCESS = 5;

    /**
     * 人脸资源删除失败
     */
    public static final int RESULT_DELETE_FAIL = 6;

    /**
     * 新增或更新
     */
    public static final String STATUS_UPDATE = "0";

    /**
     * 删除
     */
    public static final String STATUS_DELETE = "1";

    public static final String SPLITER = "__";

}
