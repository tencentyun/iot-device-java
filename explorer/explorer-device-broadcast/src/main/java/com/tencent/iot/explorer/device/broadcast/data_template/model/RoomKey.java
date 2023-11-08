package com.tencent.iot.explorer.device.broadcast.data_template.model;

public class RoomKey {
    private int appId = 0;
    private String roomId = "";
    public String userId = "";
    public String userSig = "";

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public int getAppId() {
        return appId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserSig(String userSig) {
        this.userSig = userSig;
    }

    public String getUserSig() {
        return userSig;
    }
}