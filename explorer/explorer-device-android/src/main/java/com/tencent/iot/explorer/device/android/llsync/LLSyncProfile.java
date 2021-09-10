package com.tencent.iot.explorer.device.android.llsync;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

import com.tencent.iot.explorer.device.android.llsync.LLSyncGattServerConstants.LLSyncDeviceBindStatus;
import static com.tencent.iot.explorer.device.android.utils.ConvertUtils.intToByteArray;

public class LLSyncProfile {

    private static final String TAG = LLSyncProfile.class.getSimpleName();

    /* LLSYNC Version */
    public static String LLSYNC_VERSION = "2";

    /* LLSYNC BLE WIFI COMBO Service UUID */
    public static UUID LLSYNC_BLE_WIFI_COMBO_SERVICE_16bitUUID   = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    /* LLSYNC BLE WIFI COMBO Service UUID */
    public static UUID LLSYNC_BLE_WIFI_COMBO_SERVICE_UUID        = UUID.fromString("0000fff0-65d0-4e20-b56a-e493541ba4e2");
    /* LLSYNC BLE ONLY Service UUID */
    public static UUID LLSYNC_BLE_ONLY_SERVICE_16bitUUID         = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    /* LLSYNC BLE ONLY Service UUID */
    public static UUID LLSYNC_BLE_ONLY_SERVICE_UUID              = UUID.fromString("0000ffe0-65d0-4e20-b56a-e493541ba4e2");
    /* LLSYNC DEVICE INFO Characteristic UUID */
    public static UUID LLSYNC_DEVICE_INFO_CHARACTERISTIC_UUID    = UUID.fromString("0000ffe1-65d0-4e20-b56a-e493541ba4e2");
    /* LLSYNC DATA Characteristic UUID */
    public static UUID LLSYNC_DATA_CHARACTERISTIC_UUID           = UUID.fromString("0000ffe2-65d0-4e20-b56a-e493541ba4e2");
    /* LLSYNC EVENT Characteristic UUID */
    public static UUID LLSYNC_EVENT_CHARACTERISTIC_UUID          = UUID.fromString("0000ffe3-65d0-4e20-b56a-e493541ba4e2");
    /* LLSYNC OTA Characteristic UUID */
    public static UUID LLSYNC_OTA_CHARACTERISTIC_UUID            = UUID.fromString("0000ffe4-65d0-4e20-b56a-e493541ba4e2");
    /* LLSYNC CLIENT CONFIG Descriptor UUID */
    public static UUID LLSYNC_CLIENT_CONFIG_DESCRIPTOR_UUID      = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * LLSYNC Ble Wifi Combo Service.
     */
    public static BluetoothGattService createBleWifiComboLLSyncService() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BluetoothGattService service = new BluetoothGattService(LLSYNC_BLE_WIFI_COMBO_SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // LLSYNC Device Info characteristic
            BluetoothGattCharacteristic deviceinfo = new BluetoothGattCharacteristic(LLSYNC_DEVICE_INFO_CHARACTERISTIC_UUID,
                    //write characteristic
                    BluetoothGattCharacteristic.PROPERTY_WRITE ,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            // LLSYNC Event characteristic
            BluetoothGattCharacteristic event = new BluetoothGattCharacteristic(LLSYNC_EVENT_CHARACTERISTIC_UUID,
                    //support notify characteristic
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(LLSYNC_CLIENT_CONFIG_DESCRIPTOR_UUID,
                    //write descriptor
                    BluetoothGattDescriptor.PERMISSION_WRITE);
            event.addDescriptor(configDescriptor);

            service.addCharacteristic(deviceinfo);
            service.addCharacteristic(event);

            return service;
        } else {
            return null;
        }
    }

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * LLSYNC Ble Only Service.
     */
    public static BluetoothGattService createBleOnlyLLSyncService() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BluetoothGattService service = new BluetoothGattService(LLSYNC_BLE_ONLY_SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // LLSYNC Device Info characteristic
            BluetoothGattCharacteristic deviceinfo = new BluetoothGattCharacteristic(LLSYNC_DEVICE_INFO_CHARACTERISTIC_UUID,
                    //write characteristic
                    BluetoothGattCharacteristic.PROPERTY_WRITE ,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            // LLSYNC Data characteristic
            BluetoothGattCharacteristic data = new BluetoothGattCharacteristic(LLSYNC_DATA_CHARACTERISTIC_UUID,
                    //write characteristic
                    BluetoothGattCharacteristic.PROPERTY_WRITE ,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            // LLSYNC Event characteristic
            BluetoothGattCharacteristic event = new BluetoothGattCharacteristic(LLSYNC_EVENT_CHARACTERISTIC_UUID,
                    //support notify characteristic
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(LLSYNC_CLIENT_CONFIG_DESCRIPTOR_UUID,
                    //write descriptor
                    BluetoothGattDescriptor.PERMISSION_WRITE);
            event.addDescriptor(configDescriptor);

            // LLSYNC OTA characteristic
            BluetoothGattCharacteristic ota = new BluetoothGattCharacteristic(LLSYNC_OTA_CHARACTERISTIC_UUID,
                    //write characteristic
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            service.addCharacteristic(deviceinfo);
            service.addCharacteristic(data);
            service.addCharacteristic(event);
            service.addCharacteristic(ota);

            return service;
        } else {
            return null;
        }
    }

    public static byte[] bleOnlyAdvertisingData(String productId, String mac, LLSyncDeviceBindStatus bindStatus) {

        Log.d(TAG, mac);

        byte[] llsyncVersionAndBindStatus = new byte[0];
        if (bindStatus == LLSyncDeviceBindStatus.UNBIND) {
            llsyncVersionAndBindStatus = new byte[]{(byte) 0x20};
        } else if (bindStatus == LLSyncDeviceBindStatus.BINDING) {
            llsyncVersionAndBindStatus = new byte[]{(byte) 0x21};
        } else { //bindStatus == LLSyncDeviceBindStatus.BINDED
            llsyncVersionAndBindStatus = new byte[]{(byte) 0x22};
        }

        byte[] mac1 = {(byte) 0x74, (byte) 0x2A, (byte) 0xD2, (byte) 0x62, (byte) 0x3D, (byte) 0xF0};
        byte[] productIdByte = productId.getBytes();
        int rawAdvDataMidLen = llsyncVersionAndBindStatus.length + mac1.length + productIdByte.length;

        byte[] rawAdvData = new byte[rawAdvDataMidLen];
        int index = 0;
        System.arraycopy(llsyncVersionAndBindStatus, 0, rawAdvData, index, llsyncVersionAndBindStatus.length);
        index += llsyncVersionAndBindStatus.length;
        System.arraycopy(mac1, 0, rawAdvData, index, mac1.length);
        index += mac1.length;
        System.arraycopy(productIdByte, 0, rawAdvData, index, productIdByte.length);
        return rawAdvData;
    }

    public static byte[] bleWifiComboAdvertisingData(String productId, String mac) {

        Log.d(TAG, mac);

        byte[] llsyncVersion = intToByteArray(Integer.parseInt(LLSYNC_VERSION));

        byte[] mac1 = {(byte) 0x74, (byte) 0x2A, (byte) 0xD2, (byte) 0x62, (byte) 0x3D, (byte) 0xF0};
        byte[] productIdByte = productId.getBytes();
        int rawAdvDataMidLen = llsyncVersion.length + mac1.length + productIdByte.length;

        byte[] rawAdvData = new byte[rawAdvDataMidLen];
        int index = 0;
        System.arraycopy(llsyncVersion, 0, rawAdvData, index, llsyncVersion.length);
        index += llsyncVersion.length;
        System.arraycopy(mac1, 0, rawAdvData, index, mac1.length);
        index += mac1.length;
        System.arraycopy(productIdByte, 0, rawAdvData, index, productIdByte.length);
        return rawAdvData;
    }

    public static byte[] getDeviceInfo(String deviceName) {

        byte[] type = {(byte) 0x08};
        byte[] llsyncVersion = intToByteArray(Integer.parseInt(LLSYNC_VERSION));
        byte[] mtuFiled = {(byte) 0x82, (byte) 0x05};
        byte[] deviceNameByte = deviceName.getBytes();
        byte[] deviceNameLen = {(byte)(deviceNameByte.length)};
        byte[] valueLen = {(byte) 0x00, (byte)(llsyncVersion.length + mtuFiled.length + deviceNameLen.length + deviceNameByte.length)};
        byte[] deviceInfoData = new byte[type.length + valueLen.length + llsyncVersion.length + mtuFiled.length + deviceNameLen.length + deviceNameByte.length];
        int index = 0;
        System.arraycopy(type, 0, deviceInfoData, index, type.length);
        index += type.length;
        System.arraycopy(valueLen, 0, deviceInfoData, index, valueLen.length);
        index += valueLen.length;
        System.arraycopy(llsyncVersion, 0, deviceInfoData, index, llsyncVersion.length);
        index += llsyncVersion.length;
        System.arraycopy(mtuFiled, 0, deviceInfoData, index, mtuFiled.length);
        index += mtuFiled.length;
        System.arraycopy(deviceNameLen, 0, deviceInfoData, index, deviceNameLen.length);
        index += deviceNameLen.length;
        System.arraycopy(deviceNameByte, 0, deviceInfoData, index, deviceNameByte.length);
        return deviceInfoData;
    }

    public static byte[] bindAppIsSuccess(boolean isSuccess) {
        if (isSuccess) {
            return new byte[]{(byte) 0xE3, (byte) 0x00, (byte) 0x01, (byte) 0x00};
        } else {
            return new byte[]{(byte) 0xE3, (byte) 0x00, (byte) 0x01, (byte) 0x01};
        }
    }

    public static byte[] connectWifiIsSuccess(String ssid, boolean isWifiConnected) {
        byte[] type = {(byte) 0xE2};
        byte[] wifiMode = {(byte) 0x01}; //只支持STA模式
        byte[] station = isWifiConnected ? new byte[]{(byte) 0x00} : new byte[]{(byte) 0x01}; //0x00表示已连接，0x01表示未连接
        byte[] softAP = {(byte) 0x00}; //SoftAp状态当前不使用。
        byte[] ssidByte = ssid.getBytes();;
        byte[] ssidLen = {(byte)(ssidByte.length)};
        byte[] valueLen = {(byte) 0x00, (byte)(wifiMode.length + station.length + softAP.length + ssidLen.length + ssidByte.length)};

        byte[] connectWifiData = new byte[type.length + valueLen.length + wifiMode.length + station.length + softAP.length + ssidLen.length + ssidByte.length];
        int index = 0;
        System.arraycopy(type, 0, connectWifiData, index, type.length);
        index += type.length;
        System.arraycopy(valueLen, 0, connectWifiData, index, valueLen.length);
        index += valueLen.length;
        System.arraycopy(wifiMode, 0, connectWifiData, index, wifiMode.length);
        index += wifiMode.length;
        System.arraycopy(station, 0, connectWifiData, index, station.length);
        index += station.length;
        System.arraycopy(softAP, 0, connectWifiData, index, softAP.length);
        index += softAP.length;
        System.arraycopy(ssidLen, 0, connectWifiData, index, ssidLen.length);
        index += ssidLen.length;
        System.arraycopy(ssidByte, 0, connectWifiData, index, ssidByte.length);
        return connectWifiData;
    }

    public static byte[] getWifiInfoSuccess(boolean isWifiInfoSuccess) {
        if (isWifiInfoSuccess) {
            return new byte[]{(byte) 0xE1, (byte) 0x00, (byte) 0x01, (byte) 0x00};
        } else {
            return new byte[]{(byte) 0xE1, (byte) 0x00, (byte) 0x01, (byte) 0x01};
        }
    }

    public static byte[] setMTUSuccess() {
        return new byte[]{(byte) 0x0B, (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x05};
    }

    public static byte[] setWifiModeSuccess(boolean isWifiModeSupport) {
        if (isWifiModeSupport) {
            return new byte[]{(byte) 0xE0, (byte) 0x00, (byte) 0x01, (byte) 0x00};
        } else {
            return new byte[]{(byte) 0xE0, (byte) 0x00, (byte) 0x01, (byte) 0x01};
        }
    }
}
