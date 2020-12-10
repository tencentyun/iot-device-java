package com.tencent.iot.hub.device.android.core.shadow;

import android.os.Parcel;
import android.os.Parcelable;

import com.tencent.iot.hub.device.java.core.shadow.TXShadowConstants;

/**
 * 设备属性信息
 */
public class DeviceProperty extends com.tencent.iot.hub.device.java.core.shadow.DeviceProperty implements Parcelable {

    public static final Creator<DeviceProperty> CREATOR = new Creator<DeviceProperty>() {
        @Override
        public DeviceProperty createFromParcel(Parcel in) {
            return new DeviceProperty(in);
        }

        @Override
        public DeviceProperty[] newArray(int size) {
            return new DeviceProperty[size];
        }
    };

    public DeviceProperty() {
    }

    protected DeviceProperty(Parcel in) {
        // 注意，此处的读值顺序应与writeToParcel()方法中一致
        mKey = in.readString();
        mData = in.readString();
        mDataType = TXShadowConstants.JSONDataType.valueOf(TXShadowConstants.JSONDataType.class, in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flag) {
        out.writeString(mKey);
        out.writeString(mData.toString());
        out.writeString(mDataType.name());
    }
}
