package com.tencent.cloud.ai.fr.sdksupport;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tencent.ytcommon.util.YTCommonInterface;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * SDK授权工具类
 */
public class Auth {

    private static final String TAG = Auth.class.getSimpleName();
    
    private static final String URL = "https://license.youtu.qq.com/youtu/sdklicenseapi/license_generate";

    static {
        System.loadLibrary("YTCommon");
    }

    public static AuthResult authWithDeviceSn(Context context, String appId, String secretKey) {
        final int code = YTCommonInterface.initAuth(context, URL, appId, secretKey,false);
        Log.d(TAG, "authWithDeviceSn code = " + code);
        return innerAuth(code);
    }

    public static AuthResult authWithLicence(Context context, String licenceFileName) {
        final int code = YTCommonInterface.initAuth(context, licenceFileName, 0);
        Log.d(TAG, "authWithLicence code = " + code);
        return innerAuth(code);
    }


    private static AuthResult innerAuth(int code) {
        IAuthCodeMsg result = AuthCodeMsgImpl.resultForCode(code);
        return new AuthResult(result, YTCommonInterface.getEndTime(), YTCommonInterface.getVersion());
    }

    public interface IAuthCodeMsg {
        boolean isSucceeded();
        int getCode();
        String getMsg();
    }

    public static class AuthResult implements IAuthCodeMsg {

        private final IAuthCodeMsg mCodeMsg;
        private final long mEndTime;
        private final int mVersionCode;

        AuthResult(IAuthCodeMsg codeMsg, long endTime, int versionCode) {
            mCodeMsg = codeMsg;
            mEndTime = endTime;
            mVersionCode = versionCode;
        }

        public long getEndTime() {
            return mEndTime;
        }

        public int getVersionCode() {
            return mVersionCode;
        }

        @Override
        public boolean isSucceeded() {
            return mCodeMsg.isSucceeded();
        }

        @Override
        public int getCode() {
            return mCodeMsg.getCode();
        }

        @Override
        public String getMsg() {
            return mCodeMsg.getMsg();
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("AuthResult{");
            sb.append("mCodeMsg=").append(mCodeMsg);
            sb.append(", mEndTime=").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(mEndTime * 1000));
            sb.append(", mVersionCode=").append(mVersionCode);
            sb.append('}');
            return sb.toString();
        }
    }

    private enum AuthCodeMsgImpl implements IAuthCodeMsg {
        RESULT_0(0, "授权成功"),
        RESULT_1(1, "无效授权, 请检查授权文件的文件名/路径是否正确，比如license和licence的拼写方式是否统一"),
        RESULT_2(2, "packageName 不匹配, 请检查您的 App 使用的 packageName 和申请的是否一致, 检查是否使用了正确的授权文件"),
        RESULT_16(16, "授权文件已过期, 请检查系统时间, 或者续期"),
        RESULT__1(-1, "授权文件路径错误, 请检查文件名/路径"),
        RESULT__10(-10, "授权文件路径错误, 请检查文件名/路径，或检查initType参数（0或2）"),
        RESULT__11(-11, "授权文件路径错误, 请检查文件名/路径，或检查initType参数（0或2）"),
        
        RESULT_1002(1002, "网络连接失败, 请检查是否正常联网、是否ping通initAuth中的授权服务器URL: " + URL),
        RESULT_1003(1003, "网络初始化错误, 服务器URL: " + URL),
        RESULT_1045(1045, "授权设备已达上限，请申请续期后再使用"),

        RESULT_2002(2002, "证书下载失败, 建议重装APP重新激活"),
        RESULT_2003(2003, "证书保存失败, 建议重装APP重新激活"),
        RESULT_2004(2004, "没有写权限, 检查APP读写权限"),
        
        RESULT_3003(3003, "证书文件为空, 建议重装APP重新激活"),
        RESULT_3004(3004, "授权文件解析失败，可能是授权文件损坏，或者授权文件版本不匹配, 检查授权文件是否正确，或者重新申请新版本的授权文件"),
        RESULT_3005(3005, "证书解析出错, 建议重装APP重新激活"),
        RESULT_3006(3006, "证书解析出错, 建议重装APP重新激活"),
        RESULT_3007(3007, "序列号为空, 设备生产厂商务必填写唯一的序列号"),
        RESULT_3008(3008, "序列号解析错误, 设备生产厂商务必填写唯一的序列号"),
        RESULT_3013(3013, "设备信息不匹配, 确认是否在平台上添加了序列号, 重装APP重新激活"),
        RESULT_3014(3014, "设备信息不匹配, 确认是否在平台上添加了序列号, 重装APP重新激活"),
        RESULT_3015(3015, "package name不匹配, 检查项目的package name"),
        RESULT_3016(3016, "package name为空, 建议重新申请授权"),
        RESULT_3017(3017, "证书已过期（累积时间）, 请续期"),
        RESULT_3018(3018, "证书已过期, 请续期"),
        RESULT_3019(3019, "license版本不匹配, 请更新common库或license"),
        RESULT_3022(3022, "设备信息不匹配, 建议重装APP重新激活"),
        
        RESULT_4001(4001, "设备序列号无效, 设备生产厂商务必填写唯一的序列号"),
        RESULT_4003(4003, "没有权限获取序列号, 检查READ_PHONE_STATE权限"),
        
        RESULT__1001(-1001, "请求字段中参数错误, 检查APPID是否正确"),
        RESULT__1005(-1005, "设备时间和服务器不符, 请确认设备时间正确"),
        
        RESULT__1104(-1104, "设备序列号不匹配, 检查在平台登记的序列号和设备实际序列号是否一致"),
        
        RESULT__1301(-1301, "序列号信息为空, 备生产厂商务必填写唯一的序列号"),
        RESULT__1302(-1302, "没有查询到序列号记录, 确认是否在平台上添加了序列号"),
        
        RESULT__1401(-1401, "该设备续期次数超过限制, 换一台设备重新申请授权，或申请解禁"),
        RESULT__1402(-1402, "授权时间无效, 请续期"),
        RESULT__1405(-1405, "appid没有匹配到设备, 检查授权代码中的appid是否正确, 检查账号下是否正确添加了序列号"),
        RESULT__1407(-1407, "授权已过期"),
        ;

        static @NonNull
        IAuthCodeMsg resultForCode(final int code) {
            for (AuthCodeMsgImpl c : AuthCodeMsgImpl.values()) {
                if (c.code == code) {
                    return c;
                }
            }
            return new IAuthCodeMsg() {
                @Override
                public boolean isSucceeded() {
                    return false;
                }

                @Override
                public int getCode() {
                    return code;
                }

                @Override
                public String getMsg() {
                    return YTCommonInterface.getFailedReason(code);
                }

                @Override
                public String toString() {
                    String sb = "AuthResultImpl{" + "code=" + getCode() +
                            ", msg='" + getMsg() + '\'' +
                            '}';
                    return sb;
                }
            };
        }

        private final int code;
        private final String msg;

        AuthCodeMsgImpl(int code, String msg) {
            this.msg = msg;
            this.code = code;
        }

        @Override
        public boolean isSucceeded() {
            return code == 0;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("AuthResultImpl{");
            sb.append("code=").append(code);
            sb.append(", msg='").append(msg).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

}
