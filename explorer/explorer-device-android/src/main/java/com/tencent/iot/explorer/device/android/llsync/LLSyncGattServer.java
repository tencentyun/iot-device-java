package com.tencent.iot.explorer.device.android.llsync;

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
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.WindowManager;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.tencent.iot.explorer.device.android.llsync.LLSyncGattServerConstants.LLSyncGattServerType;
import com.tencent.iot.explorer.device.android.llsync.LLSyncGattServerConstants.LLSyncDeviceBindStatus;
import static android.content.Context.BLUETOOTH_SERVICE;
import static com.tencent.iot.explorer.device.android.utils.ConvertUtils.byte2Hex;
import static com.tencent.iot.explorer.device.android.utils.ConvertUtils.hexString2Decimal;
import static com.tencent.iot.explorer.device.android.utils.ConvertUtils.subBytes;

public class LLSyncGattServer {

    private static final String TAG = LLSyncGattServer.class.getSimpleName();

    private Context context;
    private String mProductId;
    private String mDeviceName;
    private String mMAC;
    private LLSyncGattServerType mSyncType;

    private LLSyncGattServerCallback mCallback;

    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();


    /**
     * Instantiates a new LLSync Ble Wifi Combo gatt server.
     *
     * @param context    context
     * @param productId  the product id
     * @param deviceName the device name
     * @param mac        mac address eg: "FF:FF:FF:FF:FF:FF"
     * @param callback   callback for operation result
     */
    public LLSyncGattServer(Context context, String productId, String deviceName, String mac, LLSyncGattServerCallback callback) {
        this.context = context;
        this.mProductId = productId;
        this.mDeviceName = deviceName;
        this.mMAC = mac;
        this.mSyncType = LLSyncGattServerType.BLE_WIFI_COMBO;
        this.mCallback = callback;
        setUp();
    }

    /**
     * Instantiates a new LLSync gatt server.
     *
     * @param context    context
     * @param productId  the product id
     * @param deviceName the device name
     * @param mac        mac address eg: "FF:FF:FF:FF:FF:FF"
     * @param syncType   llsync gatt server type  (ble only Or ble wifi combo)
     * @param callback   callback for operation result
     */
    public LLSyncGattServer(Context context, String productId, String deviceName, String mac, LLSyncGattServerType syncType, LLSyncGattServerCallback callback) {
        this.context = context;
        this.mProductId = productId;
        this.mDeviceName = deviceName;
        this.mMAC = mac;
        this.mSyncType = syncType;
        this.mCallback = callback;
        setUp();
    }

    private void setUp() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            // Devices with a display should not go to sleep
            if ( context instanceof Activity ) {
                ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                onFailure("context is not instanceif activity");
            }

            mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
            // We can't continue without proper Bluetooth support
            if (!checkBluetoothSupport(bluetoothAdapter)) {
                onFailure("device is not support bluetooth");
            }

            // Register for system Bluetooth events
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(mBluetoothReceiver, filter);
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth is currently disabled...enabling");
                bluetoothAdapter.enable();
            } else {
                Log.d(TAG, "Bluetooth enabled...starting services");
                startAdvertising(mSyncType, LLSyncDeviceBindStatus.UNBIND);
                startServer();
            }
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
        }
    }

    public void release() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
            if (bluetoothAdapter.isEnabled()) {
                stopServer();
                stopAdvertising();
            }

            context.unregisterReceiver(mBluetoothReceiver);
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
        }
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

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
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

            Log.w(TAG, "mBluetoothReceiver onReceive state: " + state);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising(mSyncType, LLSyncDeviceBindStatus.UNBIND);
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
     * and supports the LLSync Service.
     */
    private void startAdvertising(LLSyncGattServerType syncType, LLSyncDeviceBindStatus bindStatus) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
            bluetoothAdapter.setName(mDeviceName);
            mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (mBluetoothLeAdvertiser == null) {
                Log.w(TAG, "Failed to create advertiser");
                onFailure("Failed to create advertiser");
                return;
            }

            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build();

            byte[] manufacturerData;
            UUID serviceUuid;
            if (syncType == LLSyncGattServerType.BLE_WIFI_COMBO) {
                manufacturerData = LLSyncProfile.bleWifiComboAdvertisingData(mProductId, mMAC);
                serviceUuid = LLSyncProfile.LLSYNC_BLE_WIFI_COMBO_SERVICE_16bitUUID;
            } else { //LLSyncGattServerType.ONLY_BLE
                manufacturerData = LLSyncProfile.bleOnlyAdvertisingData(mProductId, mMAC, bindStatus);
                serviceUuid = LLSyncProfile.LLSYNC_BLE_ONLY_SERVICE_16bitUUID;
            }

            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(false)
                    .addServiceUuid(new ParcelUuid(serviceUuid))
                    .build();

            AdvertiseData scanResponse = new AdvertiseData.Builder()
                    .setIncludeTxPowerLevel(false)
                    .setIncludeDeviceName(false)
                    .addManufacturerData(0xFEE7, manufacturerData)
                    .build();

            mBluetoothLeAdvertiser
                    .startAdvertising(settings, data, scanResponse, mAdvertiseCallback);
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
        }
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (mBluetoothLeAdvertiser == null) return;

            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
        }

        // Devices with a display should not go to sleep clear
        if ( context instanceof Activity ) {
            ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            onFailure("context is not instanceif activity");
        }
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothGattServer = mBluetoothManager.openGattServer(context, mGattServerCallback);
            if (mBluetoothGattServer == null) {
                Log.w(TAG, "Unable to create GATT server");
                return;
            }

            if (mSyncType == LLSyncGattServerType.BLE_WIFI_COMBO) {
                mBluetoothGattServer.addService(LLSyncProfile.createBleWifiComboLLSyncService());
            } else { // BLE_ONLY
                mBluetoothGattServer.addService(LLSyncProfile.createBleOnlyLLSyncService());
            }
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
        }
    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (mBluetoothGattServer == null) return;

            mBluetoothGattServer.close();
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
        }
    }

    private BluetoothDevice e2device;
    private int e2requestId;

    public void noticeAppConnectWifiIsSuccess(boolean isSuccess) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BluetoothGattCharacteristic deviceinfoCharacteristic = mBluetoothGattServer
                    .getService(LLSyncProfile.LLSYNC_BLE_WIFI_COMBO_SERVICE_UUID)
                    .getCharacteristic(LLSyncProfile.LLSYNC_EVENT_CHARACTERISTIC_UUID);
            deviceinfoCharacteristic.setValue(LLSyncProfile.getWifiInfoSuccess(isSuccess));
            mBluetoothGattServer.notifyCharacteristicChanged(e2device, deviceinfoCharacteristic, false);
            Log.i(TAG, "notify getWifiInfoSuccess " + byte2Hex(LLSyncProfile.getWifiInfoSuccess(isSuccess)));
            mBluetoothGattServer.sendResponse(e2device,
                    e2requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null);
        }
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

    private void onLlsyncDeviceInfoCharacteristicWriteRequest (BluetoothDevice device, int requestId, byte[] value) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UUID uuid ;
            if (mSyncType == LLSyncGattServerType.BLE_WIFI_COMBO) {
                uuid = LLSyncProfile.LLSYNC_BLE_WIFI_COMBO_SERVICE_UUID;
            } else {
                uuid = LLSyncProfile.LLSYNC_BLE_ONLY_SERVICE_UUID;
            }
            BluetoothGattCharacteristic deviceinfoCharacteristic = mBluetoothGattServer
                    .getService(uuid)
                    .getCharacteristic(LLSyncProfile.LLSYNC_EVENT_CHARACTERISTIC_UUID);
            deviceinfoCharacteristic.setValue(value);
            mBluetoothGattServer.notifyCharacteristicChanged(device, deviceinfoCharacteristic, false);
            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null);
        } else {
            onFailure("bellow android 5.0 is not support bluetooth");
            Log.i(TAG, "bellow android 5.0 is not support bluetooth");
        }
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

        @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {

            // Invalid characteristic, llsync profile no characteristic is support read
            Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
        }

        private String ssid = "";
        private String pwd = "";

        @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                                                 boolean responseNeeded, int offset, byte[] value) {

            // llsync only device info characteristic support read
            if (LLSyncProfile.LLSYNC_DEVICE_INFO_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                Log.i(TAG, "Write Device Info " + byte2Hex(value));
                if (byte2Hex(value).equals("e0")) { //小程序APP通过LLDeviceInfo向设备下发信息获取指令。

                    byte[] sendAppValue = LLSyncProfile.getDeviceInfo(mDeviceName);
                    Log.i(TAG, "notify getDeviceInfo " + byte2Hex(sendAppValue));
                    onLlsyncDeviceInfoCharacteristicWriteRequest(device, requestId, sendAppValue);

                } else if (byte2Hex(value).startsWith("e1")) { //小程序APP通过LLDeviceInfo向设备下发设置wifi模式指令。

                    String wifiMode = byte2Hex(subBytes(value, 1, 1));
                    byte[] sendAppValue = LLSyncProfile.setWifiModeSuccess(wifiMode.equals("01"));
                    if (wifiMode.equals("01")) {
                        Log.i(TAG, "notify setWifiModeSuccess " + byte2Hex(sendAppValue));
                    } else {
                        Log.w(TAG, "E1 Invalid Write WifiMode : " + wifiMode + "; characteristic uuid: " + characteristic.getUuid());
                    }
                    onLlsyncDeviceInfoCharacteristicWriteRequest(device, requestId, sendAppValue);


                } else if (byte2Hex(value).equals("090000")) { //小程序APP通过LLDeviceInfo向设备下发设置mtu结果指令。

                    byte[] sendAppValue = LLSyncProfile.setMTUSuccess();
                    Log.i(TAG, "notify setMTUSuccess " + byte2Hex(sendAppValue));
                    onLlsyncDeviceInfoCharacteristicWriteRequest(device, requestId, sendAppValue);

                } else if (byte2Hex(value).startsWith("e2")) { //小程序APP通过LLDeviceInfo向设备下发wifi信息

                    String e2InfoLen = byte2Hex(subBytes(value, 1, 2));
                    String ssidLen = byte2Hex(subBytes(value, 3, 1));
                    String pwdLen = byte2Hex(subBytes(value, 4 + hexString2Decimal(ssidLen), 1));
                    try {
                        String ssid = new String(subBytes(value, 4, hexString2Decimal(ssidLen)), "utf-8");
                        this.ssid = ssid;
                        String pwd = new String(subBytes(value, 5 + hexString2Decimal(ssidLen), hexString2Decimal(pwdLen)), "utf-8");
                        this.pwd = pwd;
                        Log.i(TAG, "ssid: " + ssid + "; pwd: " + pwd);
                        e2device = device;
                        e2requestId = requestId;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //把收到的ssid和pwd回调到上层去连接wifi后调用连接成功失败的方法。
                    if (mCallback != null) {
                        mCallback.requestConnectWifi(ssid, pwd);
                    }


                } else if (byte2Hex(value).equals("e3")) { //小程序APP通过LLDeviceInfo向设备下发请求连接wifi指令。

                    byte[] sendAppValue = LLSyncProfile.connectWifiIsSuccess(this.ssid, true);
                    Log.i(TAG, "notify connectWifiIsSuccess " + byte2Hex(sendAppValue));
                    onLlsyncDeviceInfoCharacteristicWriteRequest(device, requestId, sendAppValue);

                } else if (byte2Hex(value).startsWith("e4")) { //小程序APP通过LLDeviceInfo向设备下发token信息

                    String tokenLen = byte2Hex(subBytes(value, 1, 2));

                    try {
                        String token = new String(subBytes(value, 3, hexString2Decimal(tokenLen)), "utf-8");

                        //把收到的token回调到上层去绑定用户和设备成功后调用绑定成功失败的方法。
                        if (mCallback != null) {
                            mCallback.requestAppBindToken(token);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    byte[] sendAppValue = LLSyncProfile.bindAppIsSuccess(true);
                    Log.i(TAG, "notify bindAppSuccess " + byte2Hex(sendAppValue));
                    onLlsyncDeviceInfoCharacteristicWriteRequest(device, requestId, sendAppValue);

                } else {
                    // Invalid characteristic
                    Log.w(TAG, "Invalid Characteristic Write: " + characteristic.getUuid());
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
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

        @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (LLSyncProfile.LLSYNC_CLIENT_CONFIG_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
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

        @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            Log.i(TAG, "Write Descriptor " + byte2Hex(value));
            if (LLSyncProfile.LLSYNC_CLIENT_CONFIG_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
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

    private void onFailure(String errorMessage) {
        if (mCallback != null) {
            mCallback.onFailure(errorMessage);
        }
    }
}
