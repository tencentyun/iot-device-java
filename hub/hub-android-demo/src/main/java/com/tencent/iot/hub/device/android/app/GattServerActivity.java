/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.iot.hub.device.android.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class GattServerActivity extends Activity {
    private static final String TAG = GattServerActivity.class.getSimpleName();

    /* Local UI */
    private TextView mLocalTimeView;
    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mLocalTimeView = (TextView) findViewById(R.id.text_time);

        // Devices with a display should not go to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish();
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }

        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }

        }
    };

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        bluetoothAdapter.setName("c");
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")))
//                .addManufacturerData(0xfff0, advertisingData())
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addManufacturerData(0xFEE7, advertisingData())
                .build();


        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, scanResponse, mAdvertiseCallback);
    }

    private byte[] advertisingData() {

//        byte[] rawAdvDataHead = {(byte)0x02, (byte)0x01, (byte)0x06, (byte)0x03, (byte)0x03, (byte)0xF0, (byte)0xFF};

        byte[] rawAdvDataMidType = {(byte)0xE0};
//        byte[] companyIdentifier = {(byte)0xE7, (byte)0xFE};
        byte[] llsyncVersion = {(byte)0x02};
        byte[] mac = {(byte)0x74, (byte)0x2A, (byte)0xD2, (byte)0x62, (byte)0x3D, (byte)0xF0};
        String productId = "ZAV4BUY8UQ";
        byte[] productIdByte = productId.getBytes();
//        byte[] rawAdvDataMidLen = {(byte)(rawAdvDataMidType.length + companyIdentifier.length + llsyncVersion.length + mac.length + productIdByte.length)};
        int rawAdvDataMidLen = llsyncVersion.length + mac.length + productIdByte.length;

        byte[] rawAdvDataFoot = {(byte)0x02, (byte)0x09, (byte)0x6C};

        byte[] rawAdvData = new byte[rawAdvDataMidLen];
        int index = 0;
//        System.arraycopy(rawAdvDataHead, 0, rawAdvData, index, rawAdvDataHead.length);
//        index += rawAdvDataHead.length;
//        System.arraycopy(rawAdvDataMidLen, 0, rawAdvData, index, rawAdvDataMidLen.length);
//        index += rawAdvDataMidLen.length;
//        System.arraycopy(rawAdvDataMidType, 0, rawAdvData, index, rawAdvDataMidType.length);
//        index += rawAdvDataMidType.length;
//        System.arraycopy(companyIdentifier, 0, rawAdvData, index, companyIdentifier.length);
//        index += companyIdentifier.length;
        System.arraycopy(llsyncVersion, 0, rawAdvData, index, llsyncVersion.length);
        index += llsyncVersion.length;
        System.arraycopy(mac, 0, rawAdvData, index, mac.length);
        index += mac.length;
        System.arraycopy(productIdByte, 0, rawAdvData, index, productIdByte.length);
//        index += productIdByte.length;
//        System.arraycopy(rawAdvDataFoot, 0, rawAdvData, index, rawAdvDataFoot.length);
        return rawAdvData;
    }

    private byte[] getDeviceInfo() {
        byte[] type = {(byte)0x08};
        byte[] llsyncVersion = {(byte)0x02};
        byte[] mtuFiled = {(byte)0x82, (byte)0x05};
        String deviceName = "ble_wifi_001";
        byte[] deviceNameByte = deviceName.getBytes();
        byte[] deviceNameLen = {(byte)(deviceNameByte.length)};
        byte[] valueLen = {(byte)0x00, (byte)(llsyncVersion.length + mtuFiled.length + deviceNameLen.length + deviceNameByte.length)};
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

    private byte[] setMTUSuccess() {
        return new byte[]{(byte) 0x0B, (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x05};
    }

    private byte[] setWifiModeSuccess() {
        return new byte[]{(byte) 0xE0, (byte) 0x00, (byte) 0x01, (byte) 0x00};
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(IOTBLEProfile.createIOTBLEService());

        // Initialize the local UI
        updateLocalUi(System.currentTimeMillis());
    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    /**
     * Update graphical UI on devices that support it with the current time.
     */
    private void updateLocalUi(long timestamp) {
        Date date = new Date(timestamp);
        String displayDate = DateFormat.getMediumDateFormat(this).format(date)
                + "\n"
                + DateFormat.getTimeFormat(this).format(date);
        mLocalTimeView.setText(displayDate);
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(device, txPhy, rxPhy, status);
            Log.i(TAG, "onPhyUpdate" + device);
//            mBluetoothGattServer.setPreferredPhy(device, txPhy, rxPhy, 0);
        }

        @Override
        public void onMtuChanged(final BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.i(TAG, "onMtuChanged" + device);
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            long now = System.currentTimeMillis();
            if (IOTBLEProfile.IOT_BLE_DEVICE_INFO.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read Device Info");
            } else if (IOTBLEProfile.IOT_BLE_EVENT.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read Event");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        IOTBLEProfile.getLocalTimeInfo(now));
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                                                 boolean responseNeeded, int offset, byte[] value) {

            if (IOTBLEProfile.IOT_BLE_DEVICE_INFO.equals(characteristic.getUuid())) {
                Log.i(TAG, "Write Device Info"+byte2Hex(value));
                if (byte2Hex(value).equals("e0")) { //小程序APP通过LLDeviceInfo向设备下发信息获取指令。
                    BluetoothGattCharacteristic deviceinfoCharacteristic = mBluetoothGattServer
                            .getService(IOTBLEProfile.IOT_BLE_SERVICE)
                            .getCharacteristic(IOTBLEProfile.IOT_BLE_EVENT);
                    deviceinfoCharacteristic.setValue(getDeviceInfo());
                    mBluetoothGattServer.notifyCharacteristicChanged(device, deviceinfoCharacteristic, false);
                    Log.i(TAG, "notify getDeviceInfo "+byte2Hex(getDeviceInfo()));
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                } else if (byte2Hex(value).equals("e101")) { //小程序APP通过LLDeviceInfo向设备下发设置wifi结果指令。
                    BluetoothGattCharacteristic deviceinfoCharacteristic = mBluetoothGattServer
                            .getService(IOTBLEProfile.IOT_BLE_SERVICE)
                            .getCharacteristic(IOTBLEProfile.IOT_BLE_EVENT);
                    deviceinfoCharacteristic.setValue(setWifiModeSuccess());
                    mBluetoothGattServer.notifyCharacteristicChanged(device, deviceinfoCharacteristic, false);
                    Log.i(TAG, "notify setWifiModeSuccess "+byte2Hex(setWifiModeSuccess()));
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                } else if (byte2Hex(value).equals("090000")) { //小程序APP通过LLDeviceInfo向设备下发设置mtu指令。
                    BluetoothGattCharacteristic deviceinfoCharacteristic = mBluetoothGattServer
                            .getService(IOTBLEProfile.IOT_BLE_SERVICE)
                            .getCharacteristic(IOTBLEProfile.IOT_BLE_EVENT);
                    deviceinfoCharacteristic.setValue(setMTUSuccess());
                    mBluetoothGattServer.notifyCharacteristicChanged(device, deviceinfoCharacteristic, false);
                    Log.i(TAG, "notify setMTUSuccess "+byte2Hex(setMTUSuccess()));
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                } else if (byte2Hex(value).startsWith("e2")) { //小程序APP通过LLDeviceInfo向设备下发wifi信息

                    Log.i(TAG, "notify setMTUSuccess "+byte2Hex(setMTUSuccess()));
                    String e2InfoLen = byte2Hex(value).substring(2, 6);
                    String ssidLen = byte2Hex(value).substring(6, 8);
                    String ssid = hexString2String(byte2Hex(value).substring(9, 8+hexString2Decimal(ssidLen)*2-1));
                    String pwdLen = byte2Hex(value).substring(8+hexString2Decimal(ssidLen)*2, 8+hexString2Decimal(ssidLen)*2+1);
                    String pwd = hexString2String(byte2Hex(value).substring(8+hexString2Decimal(ssidLen)*2+2, 8+hexString2Decimal(ssidLen)*2+2+hexString2Decimal(pwdLen)*2-1));

                    BluetoothGattCharacteristic deviceinfoCharacteristic = mBluetoothGattServer
                            .getService(IOTBLEProfile.IOT_BLE_SERVICE)
                            .getCharacteristic(IOTBLEProfile.IOT_BLE_EVENT);
                    deviceinfoCharacteristic.setValue(setMTUSuccess());
                    mBluetoothGattServer.notifyCharacteristicChanged(device, deviceinfoCharacteristic, false);
                    Log.i(TAG, "notify setMTUSuccess "+byte2Hex(setMTUSuccess()));
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else if (IOTBLEProfile.IOT_BLE_EVENT.equals(characteristic.getUuid())) {
                Log.i(TAG, "Write Event");
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Write: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        /**
         * 将byte转为16进制
         *
         * @param bytes
         * @return
         */
        private String byte2Hex(byte[] bytes) {
            StringBuffer stringBuffer = new StringBuffer();
            String temp = null;
            for (int i = 0; i < bytes.length; i++) {
                temp = Integer.toHexString(bytes[i] & 0xFF);
                if (temp.length() == 1) {
                    // 1得到一位的进行补0操作
                    stringBuffer.append("0");
                }
                stringBuffer.append(temp);
            }
            return stringBuffer.toString();
        }

        /**
         * 16进制转10进制
         *
         * @param str
         * @return
         */
        public int hexString2Decimal(String str) {

            try {
                int in = Integer.parseInt(str, 16);
                return in;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }
        }

        /**
         * 16进制转字符串格式
         *
         * @param src
         * @return
         */
        public String hexString2String(String src) {
            String temp = "";
            for (int i = 0; i < src.length() / 2; i++) {
                temp = temp + (char) Integer.valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
            }
            return temp;
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (IOTBLEProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        returnValue);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            if (IOTBLEProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }
    };
}
