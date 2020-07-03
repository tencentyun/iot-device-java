package com.tencent.iot.explorer.device.android.gateway;

import android.content.Context;

import com.tencent.iot.explorer.device.android.common.Status;
import com.tencent.iot.explorer.device.android.data_template.TXDataTemplate;
import com.tencent.iot.explorer.device.android.data_template.TXDataTemplateDownStreamCallBack;

public class TXGatewaySubdev extends TXDataTemplate {
    public static final String TAG = "TXGatewaySubdev";
    //设备信息
    public String mDeviceName;
    public String mProductId;

    public TXGatewaySubdevActionCallBack mActionCallBack;
    private Status mStat;

    /**
     * @param connection         网关设备连接句柄
     * @param context           用户上下文（这个参数在回调函数时透传给用户）
     * @param productId         产品名
     * @param deviceName        设备名，唯一
     * @param jsonFileName      数据模板描述文件
     * @param actionCallBack    上下线回调
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXGatewaySubdev(TXGatewayClient connection, Context context, String productId, String deviceName, final String jsonFileName,
                           TXGatewaySubdevActionCallBack actionCallBack, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(context, connection, productId, deviceName, jsonFileName, downStreamCallBack);
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
