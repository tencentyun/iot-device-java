package com.tencent.iot.explorer.device.broadcast.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class IntentParams implements Serializable {
    public ArrayList<UserInfo> mUserInfos;

    public IntentParams(ArrayList<UserInfo> userInfos) {
        mUserInfos = userInfos;
    }

}
