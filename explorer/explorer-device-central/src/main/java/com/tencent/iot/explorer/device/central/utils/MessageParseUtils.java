package com.tencent.iot.explorer.device.central.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.Base64;
import com.tencent.iot.explorer.device.central.message.payload.Payload;
import com.tencent.iot.explorer.device.central.message.payload.PayloadMessage;

public class MessageParseUtils {
    public static Payload parseMessage(String message) {
        Payload payload = null;
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(message);
        if (jsonObject != null) {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data != null && "DeviceChange".equals(data.getString("action"))) {
                PayloadMessage payloadMessage = JSON.parseObject(data.toJSONString(), PayloadMessage.class);
                PayloadMessage.Param params = payloadMessage.getParams();
                payload = new Payload();
                payload.setJson(message);
                if (params != null) {
                    payload.setDeviceId(params.getDeviceId());
                    String p = new String(Base64.decodeFast(params.getPayload()));
                    payload.setPayload(p);
                    payload.setData(getPayload(p));
                    payload.setType(params.getType());
                    payload.setSubtype(params.getSubType());
                }
            }
        }

        return payload;
    }

    // {"type":"delta", "payload":{"state":{"power_switch":0},"version":0}}
    // {"type":"update","state":{"reported":{"brightness":23}},"version":0,"clientToken":"API-ControlDeviceData-1571981804"}
    // { "method": "report", "params": { "brightness": 14, "color": 0, "power_switch": 0, "name": "test-light-position-3556"}, "timestamp": 1581585022, "clientToken": "22"}
    private static String getPayload(String str) {
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(str);
        if (jsonObject == null) {
            return "";
        }
        String type = jsonObject.getString("type");
        if ("update".equals(type)) {
            com.alibaba.fastjson.JSONObject state = jsonObject.getJSONObject("state");
            if (state != null) {
                return state.getString("reported");
            }
        } else if ("delta".equals(type)) {
            com.alibaba.fastjson.JSONObject state = jsonObject.getJSONObject("payload");
            if (state != null) {
                return state.getString("state");
            }
        } else {
            if (jsonObject.containsKey("params")) {
                return jsonObject.getString("params");
            }
        }
        return "";
    }
}
