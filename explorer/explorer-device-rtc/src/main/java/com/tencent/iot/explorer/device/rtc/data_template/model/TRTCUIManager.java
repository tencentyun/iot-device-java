package com.tencent.iot.explorer.device.rtc.data_template.model;

public class TRTCUIManager {

    private static TRTCUIManager instance;

    private TRTCCallingParamsCallback callingParamsCallback = null;

    private TRTCSessionManager sessionManager = null;

    public Boolean isCalling = false;

    public Boolean callMobile = false;  //主动呼叫手机

    public String callingUserId = "";  //应用端userid

    public synchronized static TRTCUIManager getInstance() {
        if (instance == null) {
            instance = new TRTCUIManager();
        }
        return instance;
    }

    public void setSessionManager(TRTCSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void addCallingParamsCallback(TRTCCallingParamsCallback callingParamsCallback) {
        this.callingParamsCallback = callingParamsCallback;
    }

    public void removeCallingParamsCallback() {
        this.callingParamsCallback = null;
    }

    public void didAcceptJoinRoom(Integer callingType, String deviceId, String agent) {
        sessionManager.joinRoom(callingType, deviceId, agent);
    }

    public void didExitRoom(Integer callingType, String deviceId) {
        sessionManager.didExitRoom(callingType, deviceId);
    }

    public void startOnThePhone(Integer callingType, String deviceId, String agent) {
        sessionManager.startOnThePhone(callingType, deviceId, agent);
    }

    public void joinRoom(Integer callingType, String deviceId, RoomKey roomKey) {
        if (callingParamsCallback != null) {
            callingParamsCallback.joinRoom(callingType, deviceId, roomKey);
        }
    }

    public void refuseEnterRoom() {
        if (callingParamsCallback != null) {
            callingParamsCallback.refuseEnterRoom();
        }
    }
}
