package com.tencent.iot.explorer.device.video.device_and_device.entity;

public class DeviceStatus {
    // 0   接收请求
    // 1   拒绝请求
    // 404 error request message
    // 405 connect number too many
    // 406 current command don't support
    // 407 device process error
    public int status = 0;
    public int appConnectNum = 0;
}
