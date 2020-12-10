package com.tencent.iot.explorer.device.java.gateway;



import com.tencent.iot.hub.device.java.core.common.Status;

import org.eclipse.paho.client.mqttv3.IMqttToken;

public abstract class TXGatewaySubdevActionCallBack {

    /**
     * 网关子设备上线回调
     *
     */
    public abstract void onSubDevOnline();

    /**
     * 网关子设备下线回调
     *
     */
    public abstract void onSubDevOffline();

    /**
     * 订阅主题完成回调
     *
     * @param status      Status.OK: 订阅成功； Status.ERROR: 订阅失败
     * @param token       消息token，包含消息内容结构体
     * @param userContext 用户上下文
     * @param msg        详细信息
     */
    public abstract void onSubscribeCompleted(Status status, IMqttToken token, Object userContext, String msg);
}
