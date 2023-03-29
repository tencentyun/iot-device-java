package com.tencent.iot.device.video.advanced.recorder;

public class ErrorCode {
    public static final int SUCCESS = 0; // 成功
    public static final int ERROR_STATE = -1; // 状态异常
    public static final int ERROR_INSTANCE = -2; // 没有实例对象
    public static final int ERROR_PARAM = -3; // 参数错误
    public static final int ERROR_FILE_NOT_FOUND = -4; // 没有对应的文件
    public static final int ERROR_IO = -5; // IO 异常
}
