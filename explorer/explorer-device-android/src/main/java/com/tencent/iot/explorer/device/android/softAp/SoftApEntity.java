package com.tencent.iot.explorer.device.android.softAp;

import org.json.JSONException;
import org.json.JSONObject;

public class SoftApEntity {
    private Integer cmdType; //cmdType
    private String ssid; //目标WiFi名称ssid
    private String bssid; //目标WiFi名称bssid
    private String password; //目标WiFi密码password
    private String token; //设备绑定app用户所需token令牌
    private String region; //app用户的区域

    public SoftApEntity(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.has("cmdType")) {
                this.cmdType = jsonObject.getInt("cmdType");
            }
            if (jsonObject.has("ssid")) {
                this.ssid = jsonObject.getString("ssid");
            }
            if (jsonObject.has("bssid")) {
                this.bssid = jsonObject.getString("bssid");
            }
            if (jsonObject.has("password")) {
                this.password = jsonObject.getString("password");
            }
            if (jsonObject.has("token")) {
                this.token = jsonObject.getString("token");
            }
            if (jsonObject.has("region")) {
                this.region = jsonObject.getString("region");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Integer getCmdType() {
        return cmdType;
    }

    public String getSsid() {
        return ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public String getRegion() {
        return region;
    }
}
