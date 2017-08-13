/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.prodigy.litevervelife;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.prodigy.litevervelife.EXTERNAL.Setting;
import com.example.prodigy.litevervelife.HelpClasses.IService;
import com.example.prodigy.litevervelife.HelpClasses.WorkWithServiceGatt;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.example.prodigy.litevervelife.HelpClasses.BroadcastReceivers.intentKeyBattery;
import static com.example.prodigy.litevervelife.HelpClasses.BroadcastReceivers.intentKeyDeviceName;
import static com.example.prodigy.litevervelife.HelpClasses.BroadcastReceivers.intentKeySound;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeServiceMaster extends Service implements IService {
    private final static String TAG = BluetoothLeServiceMaster.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    public static BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;

    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED_MASTER =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED_MASTER";
    public final static String ACTION_GATT_DISCONNECTED_MASTER =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_MASTER";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_MASTER =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_MASTER";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"); //id пульса из примера

    public final static String UPDATE_BATTERY =
            "com.example.bluetooth.le.Update_Battery_MASTER";
    public final static String UPDATE_SOUND =
            "com.example.bluetooth.le.Update_Sound_MASTER";
    public final static String UPDATE_DEVICE_NAME =
            "com.example.bluetooth.le.Update_Device_Name_MASTER";

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED_MASTER;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Coinnected to GATT server.");
                // Attempts to discover services after successful connection.
               Log.i(TAG, "Attempting to start service discovery:" +     mBluetoothGatt.discoverServices());





            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED_MASTER;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
               broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_MASTER);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override  //асинхронное получение характеристик // synchronized
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            try {
                if (WorkWithServiceGatt.battery.equals(characteristic.getUuid().toString())) {
                    String battery = Arrays.toString(characteristic.getValue()).replace("[","").replace("]","");
                    Intent intent = new Intent(UPDATE_BATTERY);
                    intent.putExtra(intentKeyBattery, battery);
                    sendBroadcast(intent);
                } else if (WorkWithServiceGatt.nameDevice.equals(characteristic.getUuid().toString())) {
                    byte[] value = characteristic.getValue();

                    Intent intent = new Intent(UPDATE_DEVICE_NAME);
                    intent.putExtra(intentKeyDeviceName, new String(value));
                    sendBroadcast(intent);
                } else if (WorkWithServiceGatt.soundEQ.equals(characteristic.getUuid().toString())) {
                    Setting setting = new Setting.SettingsBitFieldSet().byteToBundle(characteristic.getValue());

                    Intent intent = new Intent(UPDATE_SOUND);
                    intent.putExtra(intentKeySound, setting);
                    sendBroadcast(intent);
                }

            } catch (Throwable e) {
                Log.w(TAG, "ОШИБКАААААААААА" );
                e.printStackTrace();
            }
            Log.w(TAG, "КОНЕЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦЦ");

        }

        @Override  //ОТПРАВКА ДАННЫХ НА наушники (тупо парситься ответ, то что отправили в идеале и статус успешный ли)
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            try {
                if (WorkWithServiceGatt.soundEQ.equals(characteristic.getUuid().toString())) {
                    new Setting.SettingsBitFieldSet().byteToBundle(characteristic.getValue());
                }

            } catch (Throwable e) {
                Log.w(TAG, "ОШИБКАААААААААА");
                e.printStackTrace();
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };








    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeServiceMaster getService() {
            return BluetoothLeServiceMaster.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        /*
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return false;
            } else {
                return false;
            }
            */

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        if (mBluetoothGatt.connect()) {
            mConnectionState = STATE_CONNECTING;
            return true;
        }

        Log.d(TAG, "Trying to createNewGattTaskSchedule a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void sendKALLAAAA(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);
    }

    public void sendKALL(){

        mBluetoothGatt.requestMtu(120);
        mBluetoothGatt.requestConnectionPriority(1);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public synchronized void readCharacteristic(BluetoothGattCharacteristic characteristic) {
   // BluetoothGattCharacteristic characteristic
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        boolean dfdf = mBluetoothGatt.readCharacteristic(characteristic);
        String dfdgfgfgf ="";

    } //ПОтом читаем в методе onCharacteristicRead ответ асинхронный потому тут VOID!

    /**
     * Запись настроек
     *
     * @param characteristic The characteristic to read from.
     */
    public void WriteCharacteristic(BluetoothGattCharacteristic characteristic) {
        // BluetoothGattCharacteristic characteristic
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

       boolean dfdf = mBluetoothGatt.writeCharacteristic(characteristic);
        String dfdgfgfgf ="";
    } //ПОтом читаем в методе onCharacteristicRead ответ асинхронный потому тут VOID!



    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void setCharacteristicNotification(String UUIDdd) {
        if (mBluetoothGatt != null) {
            BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString("80000d20-3c4f-44be-b5f3-e302e1f59da9"));
            if (bluetoothGattService != null) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(UUIDdd));
                if (bluetoothGattCharacteristic != null) {

                    mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                }
            }
        }
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }



    @Override
    public void getGatt(String characteristic) {
        if (mBluetoothGatt != null) {
            BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString("80000d20-3c4f-44be-b5f3-e302e1f59da9"));
            if (bluetoothGattService != null) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(characteristic));
                if (bluetoothGattCharacteristic != null)
                    this.readCharacteristic(bluetoothGattCharacteristic);
            }
        }
    }

    @Override
    public void setGatt(String characteristic, byte[] settings) {
        if (mBluetoothGatt != null) {
            BluetoothGattService bluetoothGattService = mBluetoothGatt.getService(UUID.fromString("80000d20-3c4f-44be-b5f3-e302e1f59da9"));
            if (bluetoothGattService != null) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(characteristic));
                if (bluetoothGattCharacteristic != null) {
                    bluetoothGattCharacteristic.setValue(settings);
                    this.WriteCharacteristic(bluetoothGattCharacteristic);
                }
            }
        }
    }
}
