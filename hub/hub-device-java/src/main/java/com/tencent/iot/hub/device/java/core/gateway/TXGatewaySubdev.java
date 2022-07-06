package com.tencent.iot.hub.device.java.core.gateway;


import com.tencent.iot.hub.device.java.core.common.Status;

/**
 * 网关子设备类
 */
public class TXGatewaySubdev {
    /**
     * 类标记
     */
    private static final String TAG = "TXGatewaySubdev";
    /**
     * 设备名
     */
    public String mDevName;
    /**
     * 产品 ID
     */
    public String mProductId;
    private Status mStat;

    /**
     * 构造函数
     *
     * @param productId 子产品 ID
     * @param devName 子设备名
     */
    public TXGatewaySubdev(String productId, String devName) {
        mDevName = devName;
        mProductId = productId;
        mStat = Status.SUBDEV_STAT_INIT;
    }

    /**
     * 获取网关子设备状态
     *
     * @return 子设备当前状态
     */
    public synchronized Status getSubdevStatus() {
        return mStat;
    }

    /**
     * 设置网关子设备状态
     *
     * @param stat 更新子设备状态
     */
    public synchronized  void setSubdevStatus(Status stat) {
        mStat = stat;
    }

}
