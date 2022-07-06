package com.tencent.iot.hub.device.java.core.shadow;

/**
 * 设备属性信息
 */
public class DeviceProperty {

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

	/**
	 * 构造函数
	 */
	public DeviceProperty() {
	}

	/**
	 * 设备属性构造器
	 *
	 * @param key 属性名
	 * @param data 属性值
	 * @param dataType 属性值类型 {@link TXShadowConstants.JSONDataType}
	 */
	public DeviceProperty(String key, String data, TXShadowConstants.JSONDataType dataType) {
		this.mKey = key;
		this.mData = data;
		this.mDataType = dataType;
	}

	/**
	 * 设置属性名（字段名）
	 *
	 * @param mKey 属性名
	 * @return {@link DeviceProperty}
	 */
	public DeviceProperty key(String mKey) {
		this.mKey = mKey;
		return this;
	}

	/**
	 * 设置属性值
	 *
	 * @param mData 属性值
	 * @return {@link DeviceProperty}
	 */
	public DeviceProperty data(Object mData) {
		this.mData = mData;
		return this;
	}

	/**
	 * 设置属性值类型
	 *
	 * @param mDataType 属性类型 {@link TXShadowConstants.JSONDataType}
	 * @return {@link DeviceProperty}
	 */
	public DeviceProperty dataType(TXShadowConstants.JSONDataType mDataType) {
		this.mDataType = mDataType;
		return this;
	}

	/**
	 * 转换成标准格式的字符串内容
	 *
	 * @return 标准字符串内容
	 */
	@Override
	public String toString() {
		return "DeviceProperty{" + "mKey='" + mKey + '\'' + ", mData='" + mData + '\'' + ", mDataType=" + mDataType
				+ '}';
	}
}
