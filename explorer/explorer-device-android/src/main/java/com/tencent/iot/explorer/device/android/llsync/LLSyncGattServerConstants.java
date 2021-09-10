package com.tencent.iot.explorer.device.android.llsync;

public class LLSyncGattServerConstants {

    /**
     * LLSyncGattServer类型
     */
    public enum LLSyncGattServerType {

        /**
         * 纯蓝牙类型
         */
        BLE_ONLY,

        /**
         * 蓝牙辅助WIFI类型
         */
        BLE_WIFI_COMBO,
    }

    /**
     * LLSync设备绑定状态
     */
    public enum LLSyncDeviceBindStatus {

        /**
         * 未绑定
         */
        UNBIND,

        /**
         * 绑定中
         */
        BINDING,

        /**
         * 已绑定
         */
        BINDED,
    }
}
