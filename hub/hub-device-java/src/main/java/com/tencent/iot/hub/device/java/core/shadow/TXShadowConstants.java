package com.tencent.iot.hub.device.java.core.shadow;

public interface TXShadowConstants {
    String TYPE = "type";
    String GET = "get";
    String UPDATE = "update";
    String DELETE= "delete";
    String RESULT = "result";
    String SHADOW = "shadow";
    String DELTA = "delta";
    String VERSION = "version";
    String STATE = "state";
    String REPORTED = "reported";
    String CLIENT_TOKEN = "clientToken";
    String DESIRED = "desired";
    String PAYLOAD = "payload";

    /**
     * 属性的JSON数据类型
     */
    enum JSONDataType {
        /**
         * 整型
         */
        INT,

        /**
         * 长整型
         */
        LONG,

        /**
         * 单精度浮点型
         */
        FLOAT,

        /**
         * 双精度浮点型
         */
        DOUBLE,

        /**
         * 布尔型
         */
        BOOLEAN,

        /**
         * 字符串
         */
        STRING,

        /**
         * 数组类型
         */
        ARRAY,

        /**
         * 对象类型
         */
        OBJECT

    }
}

