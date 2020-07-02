package com.tencent.iot.hub.device.java.core.gateway;


import com.tencent.iot.hub.device.java.core.common.Status;

/**
 * Created by willssong on 2018/12/25.
 */

public class TXGatewaySubdev {
    public static final String TAG = "TXGatewaySubdev";
    public String mDevName;
    public String mProductId;
    private Status mStat;

    /**
     *
     * @param devName           子设备设备名
     * @param productId         子设备设备ID
     */
    public TXGatewaySubdev(String productId, String devName) {
        mDevName = devName;
        mProductId = productId;
        mStat = Status.SUBDEV_STAT_INIT;
    }

    /**
     *
     * @return                  子设备当前状态
     */
    public synchronized Status getSubdevStatus() {
        return mStat;
    }

    /**
     *
     * @param stat              更新子设备状态
     */
    public synchronized  void setSubdevStatus(Status stat) {
        mStat = stat;
    }

}
