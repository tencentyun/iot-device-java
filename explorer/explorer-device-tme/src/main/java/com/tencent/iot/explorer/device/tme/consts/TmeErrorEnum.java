package com.tencent.iot.explorer.device.tme.consts;

import com.kugou.ultimatetv.constant.ErrorCode;
import com.kugou.ultimatetv.constant.PlayerErrorCode;

public enum TmeErrorEnum {

    KPLAYER_ERROR_SONG_OVERSEAS(PlayerErrorCode.KPLAYER_ERROR_SONG_OVERSEAS, "海外地区不能播放"),

    KPLAYER_ERROR_SONG_NO_COPYRIGHT(PlayerErrorCode.KPLAYER_ERROR_SONG_NO_COPYRIGHT, "歌曲无版权不能播放"),

    KPLAYER_ERROR_SONG_NEED_VIP(PlayerErrorCode.KPLAYER_ERROR_SONG_NEED_VIP, "会员歌曲，非会员不能播放"),

    KPLAYER_ERROR_SONG_NEED_PAY(PlayerErrorCode.KPLAYER_ERROR_SONG_NEED_PAY, "付费内容，须购买才可播放"),

    KPLAYER_ERROR_SONG_NIU_NEED_VIP(PlayerErrorCode.KPLAYER_ERROR_SONG_NIU_NEED_VIP, "牛方案策略，非会员不能播放"),

    KPLAYER_ERROR_SONG_PLATFORM_NO_COPYRIGHT(PlayerErrorCode.KPLAYER_ERROR_SONG_PLATFORM_NO_COPYRIGHT, "因定向版权下架不能播放（针对APP有权但设备端无权的情况）"),

    KPLAYER_ERROR_SONG_UNKNOWN(PlayerErrorCode.KPLAYER_ERROR_SONG_UNKNOWN, "未知原因,无权播放"),

    KPLAYER_ERROR_SONG_NETWORK_ERROR(PlayerErrorCode.KPLAYER_ERROR_SONG_NETWORK_ERROR, "网络错误，请检查网络后重试"),

    PLAY_NET_MUSIC_ERR(PlayerErrorCode.PLAY_NET_MUSIC_ERR, "播放网络音乐错误"),

    NO_AVALID_NET(PlayerErrorCode.NO_AVALID_NET, "未找到可用的网络连接"),

    INAVALID_AREA(PlayerErrorCode.INAVALID_AREA, "该地区无法播放"),

    TRACKER_URL_ERROR(PlayerErrorCode.TRACKER_URL_ERROR, "播放链接错误"),

    NO_SDCARD(PlayerErrorCode.NO_SDCARD, "已经拨出SD卡,暂时无法使用"),

    NO_ENOUGH_SPACE(PlayerErrorCode.NO_ENOUGH_SPACE, "SD卡未插入或SD卡空间不足"),

    MAKE_STREAM_FAIL(PlayerErrorCode.MAKE_STREAM_FAIL, "流转换失败"),

    UNDEFINED_ERROR_CODE(ErrorCode.UNDEFINED_ERROR_CODE, "未定义错误码"),

    PARAMETER_ERROR(ErrorCode.PARAMETER_ERROR, "参数错误"),

    SYSTEM_BUSY(ErrorCode.SYSTEM_BUSY, "系统繁忙"),

    AUTHENTICATION_INFORMATION_OUT_OF_DATE_OR_WRONG(ErrorCode.AUTHENTICATION_INFORMATION_OUT_OF_DATE_OR_WRONG, "认证信息过期或错误,请重新登录"),

    CODE_DEVICE_NOTACTIVATE(ErrorCode.CODE_DEVICE_NOTACTIVATE, "设备未激活,请使用api激活"),

    SYSTEM_ERROR(ErrorCode.SYSTEM_ERROR, "系统错误"),

    NO_RIGHT_TO_CALL_THIS_INTERFACE(ErrorCode.NO_RIGHT_TO_CALL_THIS_INTERFACE, "无权调用此接口");

    final int code;

    final String msg;

    TmeErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public final String msg() {
        return msg;
    }

    public final int code() {
        return code;
    }

    public static TmeErrorEnum byCode(int code) {
        for (TmeErrorEnum value : values()) {
            if(value.code() == code) {
                return value;
            }
        }
        return null;
    }
}
