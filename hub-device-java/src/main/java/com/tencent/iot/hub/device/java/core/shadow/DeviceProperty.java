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

	public DeviceProperty() {
	}

	/**
	 * 设备属性构造器
	 *
	 * @param key
	 *            属性名
	 * @param data
	 *            属性值
	 * @param dataType
	 *            属性值类型
	 */
	public DeviceProperty(String key, String data, TXShadowConstants.JSONDataType dataType) {
		this.mKey = key;
		this.mData = data;
		this.mDataType = dataType;
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
	public String toString() {
		return "DeviceProperty{" + "mKey='" + mKey + '\'' + ", mData='" + mData + '\'' + ", mDataType=" + mDataType
				+ '}';
	}
}
