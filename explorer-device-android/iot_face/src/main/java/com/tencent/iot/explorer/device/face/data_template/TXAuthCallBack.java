package com.tencent.iot.explorer.device.face.data_template;

public abstract class TXAuthCallBack {

    /**
     * 获取AI人脸License回调
     *
     * @param code          请求状态码， 0表示获取License成功，其他表示获取失败
     * @param status        请求结果
     * @param license       请求成功对应的license
     */
    public abstract void onGetAIFaceLicenseCallBack(Integer code, String status, String license);
}
