package com.tencent.iot.explorer.device.rtc.data_template.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IntentParams implements Serializable {
    public ArrayList<UserInfo> mUserInfos;

    public IntentParams(ArrayList<UserInfo> userInfos) {
        mUserInfos = userInfos;
    }

}
