package com.tencent.iot.hub.device.java.core.log;

/**
 * MQTT 日志回调接口
 */
public abstract class  TXMqttLogCallBack {

    /**
     * 打印生成的日志和调试信息
     *
     * @param message 打印的信息
     */
    public abstract void printDebug(String message);

    /**
     * 设置密钥
     *
     * @return 返回密钥
     */
    public abstract String setSecretKey();

    /**
     * 断线时保存日志
     *
     * @param log 日志
     * @return 操作结果
     */
    public abstract boolean saveLogOffline(String log);


    /**
     * 读取断线时保存的日志
     *
     * @return 返回读取的日志内容
     */
    public abstract String readOfflineLog();

    /**
     * 删除断线时保存的日志
     *
     * @return 删除成功返回 true，失败返回 false
     */
    public abstract boolean delOfflineLog();

}
