package com.tencent.iot.explorer.device.tme.entity;

public class UserInfo {

    private String pid;
    private String pkey;
    private String userId;
    private String token;
    private long expire;

    public UserInfo(String pid, String pkey, String userId, String token, long expire) {
        this.pid = pid;
        this.pkey = pkey;
        this.userId = userId;
        this.token = token;
        this.expire = expire;
    }

    public String getPid() {
        return pid;
    }

    public String getPkey() {
        return pkey;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public long getExpire() {
        return expire;
    }
}
