package com.tencent.iot.explorer.device.broadcast.data_template.callback;

import com.tencent.iot.explorer.device.broadcast.data_template.model.RoomKey;

public interface BroadcastCallback {

    void joinBroadcast(RoomKey roomKey);
}
