package com.tencent.iot.explorer.device.java.data_template;

import com.tencent.iot.explorer.device.java.utils.CustomLog;
import com.tencent.iot.hub.device.java.core.mqtt.TXMqttConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TXDataTemplate extends DataTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(TXDataTemplate.class);
    private static final CustomLog CUSTOM_LOG = new CustomLog(LOG);

    /**
     * @param connection         mqtt连接
     * @param productId          产品名
     * @param deviceName         设备名，唯一
     * @param jsonFileName       数据模板描述文件
     * @param downStreamCallBack 下行数据接收回调函数
     */
    public TXDataTemplate(TXMqttConnection connection, String productId, String deviceName,
                          final String jsonFileName, TXDataTemplateDownStreamCallBack downStreamCallBack) {
        super(connection, productId, deviceName, new TXDataTemplateJson(jsonFileName), downStreamCallBack, CUSTOM_LOG);
    }
}
