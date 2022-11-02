package com.tencent.iot.explorer.device.central.entity;

public class Device {
    public String id;
    public int status;

    public Device(String id, int status) {
        this.id = id;
        this.status = status;
    }
}
