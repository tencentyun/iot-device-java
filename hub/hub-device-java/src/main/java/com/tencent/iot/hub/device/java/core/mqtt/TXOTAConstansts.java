package com.tencent.iot.hub.device.java.core.mqtt;

/**
 * OTA 常量
 */
public interface TXOTAConstansts {
    enum ReportState {
        /**
         * 正在升级
         */
        BURNING,
        /**
         * 升级失败
         */
        FAIL,
        /**
         * 升级完成
         */
        DONE,
    }
}
