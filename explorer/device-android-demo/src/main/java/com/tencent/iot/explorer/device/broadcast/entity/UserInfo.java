package com.tencent.iot.explorer.device.broadcast.entity;


import com.tencent.iot.explorer.device.rtc.data_template.utils.Utils;

import java.io.Serializable;

public class UserInfo implements Serializable {
    private String userId;
    public String userAvatar;
    public String userName;
    public String agent;

    public void setUserId(String userId) {
        this.userId = userId;
        userAvatar = Utils.getAvatarUrl(this.userId);
        userName = this.userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getAgent() {
        return agent;
    }
}
