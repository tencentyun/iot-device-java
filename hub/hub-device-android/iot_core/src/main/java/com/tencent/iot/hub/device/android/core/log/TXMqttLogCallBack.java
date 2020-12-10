package com.tencent.iot.hub.device.android.core.log;

public abstract class  TXMqttLogCallBack {

    /**
     * 打印生成的日志和调试信息
     * @param message 打印的信息
     */
    public abstract void printDebug(String message);

    /**
     * 设置密钥
     * @return 返回密钥
     */
    public abstract String setSecretKey();

    /**
     * 断线时保存日志
     * @param log 日志
     * @return 返回存储的日志长度
     */
    public abstract boolean saveLogOffline(String log);


    /**
     * 读取断线时保存的日志
     * @return 返回读取的日志内容
     */
    public abstract String readOfflineLog();

    /**
     * 删除断线时保存的日志
     * @return 删除成功返回true，失败返回false
     */
    public abstract boolean delOfflineLog();

}
