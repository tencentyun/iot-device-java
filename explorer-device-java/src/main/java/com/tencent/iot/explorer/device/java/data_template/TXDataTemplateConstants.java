package com.tencent.iot.explorer.device.java.data_template;

public interface TXDataTemplateConstants {
    /**
     * topic
     */
    String TOPIC_PROPERTY_DOWN_PREFIX = "$thing/down/property/";
    String TOPIC_PROPERTY_UP_PREFIX = "$thing/up/property/";
    String TOPIC_EVENT_DOWN_PREFIX = "$thing/down/event/";
    String TOPIC_EVENT_UP_PREFIX = "$thing/up/event/";
    String TOPIC_ACTION_DOWN_PREFIX = "$thing/down/action/";
    String TOPIC_ACTION_UP_PREFIX = "$thing/up/action/";
    String TOPIC_SERVICE_DOWN_PREFIX = "$thing/down/service/";
    String TOPIC_SERVICE_UP_PREFIX = "$thing/up/service/";

    enum TemplateSubTopic{
        PROPERTY_DOWN_STREAM_TOPIC,
        EVENT_DOWN_STREAM_TOPIC,
        ACTION_DOWN_STREAM_TOPIC
    }

    enum TemplatePubTopic{
        PROPERTY_UP_STREAM_TOPIC,
        EVENT_UP_STREAM_TOPIC,
        ACTION_UP_STREAM_TOPIC
    }

    /**
     * property method
     */
    String METHOD_PROPERTY_REPORT = "report";
    String METHOD_PROPERTY_REPORT_REPLY = "report_reply";

    String METHOD_PROPERTY_CONTROL = "control";
    String METHOD_PROPERTY_CONTROL_REPLY = "control_reply";

    String METHOD_PROPERTY_GET_STATUS = "get_status";
    String METHOD_PROPERTY_GET_STATUS_REPLY = "get_status_reply";

    String METHOD_PROPERTY_CLEAR_CONTROL = "clear_control";
    String METHOD_PROPERTY_CLEAR_CONTROL_REPLY = "clear_control_reply";

    String METHOD_PROPERTY_REPORT_INFO = "report_info";
    String METHOD_PROPERTY_REPORT_INFO_REPLY = "report_info_reply";

    /**
     * event method
     */
    String METHOD_EVENT_POST = "event_post";
    String METHOD_EVENTS_POST = "events_post";
    String METHOD_EVENT_REPLY = "event_reply";
    String METHOD_EVENTS_REPLY = "events_reply";

    /**
     * action method
     */
    String METHOD_ACTION = "action";
    String METHOD_ACTION_REPLY = "action_reply";

    /**
     * service method
     */
    String METHOD_SERVICE_REPORT_VERSION = "report_version";
    String METHOD_SERVICE_REPORT_VERSION_RSP = "report_version_rsp";
    String METHOD_SERVICE_UPDATE_RESOURCE = "update_resource";
    String METHOD_SERVICE_REPORT_PROGRESS = "report_progress";
    String METHOD_SERVICE_REPORT_RESULT = "report_result";
    String METHOD_SERVICE_DEL_RESOURCE = "del_resource";
    String METHOD_SERVICE_DEL_RESULT = "del_result";
    String METHOD_SERVICE_REQUEST_URL = "request_url";
    String METHOD_SERVICE_REQUEST_URL_RESP = "request_url_resp";
    String METHOD_SERVICE_REPORT_POST_RESULT = "report_post_result";
    String METHOD_SERVICE_REQUEST_RESOURCE = "request_resource";
    String METHOD_SERVICE_REQUEST_RESOURCE_RSP = "request_resource_rsp";

}
