package com.tencent.iot.explorer.device.trtc.data_template.model;

import java.io.Serializable;
import java.util.List;

public class IntentParams implements Serializable {
    public List<UserInfo> mUserInfos;

    public IntentParams(List<UserInfo> userInfos) {
        mUserInfos = userInfos;
    }
}
