package com.tencent.iot.explorer.device.face.data_template;

public abstract class TXAuthCallBack {

    /**
     * AI人脸鉴权成功回调
     *
     */
    public abstract void onSuccess();
    /**
     * AI人脸鉴权失败回调
     *
     * @param code          鉴权状态码
     * @param status        结果
     */
    public abstract void onFailure(Integer code, String status);
}
