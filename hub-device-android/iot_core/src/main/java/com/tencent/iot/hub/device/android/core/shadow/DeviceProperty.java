package com.qcloud.iot.shadow;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 设备属性信息
 */
public class DeviceProperty implements Parcelable {

    /**
     * 属性名（字段名）
     */
    public String mKey;

    /**
     * 属性值
     */
    public Object mData;

    /**
     * 属性值类型
     */
    public TXShadowConstants.JSONDataType mDataType;

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

    /**
     * 设备属性构造器
     *
     * @param key      属性名
     * @param data     属性值
     * @param dataType 属性值类型
     */
    public DeviceProperty(String key, String data, TXShadowConstants.JSONDataType dataType) {
        this.mKey = key;
        this.mData = data;
        this.mDataType = dataType;
    }

    protected DeviceProperty(Parcel in) {
        // 注意，此处的读值顺序应与writeToParcel()方法中一致
        mKey = in.readString();
        mData = in.readString();
        mDataType = TXShadowConstants.JSONDataType.valueOf(TXShadowConstants.JSONDataType.class, in.readString());
    }

    /**
     * 设置属性名（字段名）
     *
     * @param mKey
     * @return
     */
    public DeviceProperty key(String mKey) {
        this.mKey = mKey;
        return this;
    }

    /**
     * 设置属性值
     *
     * @param mData
     * @return
     */
    public DeviceProperty data(Object mData) {
        this.mData = mData;
        return this;
    }

    /**
     * 设置属性值类型
     *
     * @param mDataType
     * @return
     */
    public DeviceProperty dataType(TXShadowConstants.JSONDataType mDataType) {
        this.mDataType = mDataType;
        return this;
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

    @Override
    public String toString() {
        return "DeviceProperty{" +
                "mKey='" + mKey + '\'' +
                ", mData='" + mData + '\'' +
                ", mDataType=" + mDataType +
                '}';
    }
}
