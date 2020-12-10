package com.tencent.iot.explorer.device.java.gateway;

import com.tencent.iot.explorer.device.java.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.java.data_template.TXDataTemplateDownStreamCallBack;
import com.tencent.iot.hub.device.java.core.common.Status;

public class TXGatewaySubdev extends TXDataTemplate {
    public static final String TAG = "TXGatewaySubdev";
    //设备信息
    public String mDeviceName;
    public String mProductId;

    public TXGatewaySubdevActionCallBack mActionCallBack;
    private Status mStat;

    /**
     * @param connection         网关设备连接句柄
     * @param productId         产品名
     * @param deviceName        设备名，唯一
     * @param jsonFileName      数据模板描述文件
     * @param actionCallBack    上下线回调
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXGatewaySubdev(TXGatewayClient connection,  String productId, String deviceName, final String jsonFileName,
                           TXGatewaySubdevActionCallBack actionCallBack, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(connection, productId, deviceName, jsonFileName, downStreamCallBack);
        this.mDeviceName = deviceName;
        this.mProductId = productId;
        this.mStat = Status.SUBDEV_STAT_INIT;
        this.mActionCallBack = actionCallBack;
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
        if(stat == Status.SUBDEV_STAT_ONLINE) {
            this.mActionCallBack.onSubDevOnline();
        } else {
            this.mActionCallBack.onSubDevOffline();
        }
    }

}
