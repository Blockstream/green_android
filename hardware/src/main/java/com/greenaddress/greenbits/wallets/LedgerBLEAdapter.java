package com.greenaddress.greenbits.wallets;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.blockstream.ExtensionsKt;
import com.blockstream.hardware.BuildConfig;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.LedgerDeviceBLE;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.MutableStateFlow;

public class LedgerBLEAdapter {
    private MutableStateFlow<Boolean> disconectEvent = ExtensionsKt.createDisconnectEvent();

    public interface OnConnectedListener {
        void onConnected(final BTChipTransport transport, final boolean hasScreen, final MutableStateFlow<Boolean> disconnectEvent);
    }

    public interface OnErrorListener {
        void onError(final BTChipTransport transport);
    }

    public static void connectLedgerBLE(final Context context, final BluetoothDevice btDevice, final OnConnectedListener onConnected, final OnErrorListener onError) {
        // LedgerBLEAdapter will connect the device and call the listener when the connection is established
        final LedgerBLEAdapter adapter = new LedgerBLEAdapter(context, btDevice, onConnected, onError);
    }

    // The underlying ledger device
    private final LedgerDeviceBLE ledgerDevice;

    @SuppressLint("MissingPermission")
    private LedgerBLEAdapter(final Context context, final BluetoothDevice btDevice, final OnConnectedListener onConnected, final OnErrorListener onError) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // Adapter callback to route callbacks from the BLE stack to the LedgerDeviceBLE handler
        final BluetoothGattCallback gattAdapterCallback = new BluetoothGattCallback() {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                ledgerDevice.getGattCallback().onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                ledgerDevice.getGattCallback().onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                ledgerDevice.getGattCallback().onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                ledgerDevice.getGattCallback().onConnectionStateChange(gatt, status, newState);

                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    executorService.execute(() -> {
                        try {
                            // Connect, and callback to login activity listener when connected
                            final int delay = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N ? 1000 : 0;
                            android.os.SystemClock.sleep(delay);
                            ledgerDevice.connect();
                            onConnected.onConnected(ledgerDevice, true, disconectEvent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            onError.onError(ledgerDevice);
                        }
                    });
                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("LedgerBLEAdapter", "Send BLE disconnect event");
                    disconectEvent.setValue(true);

                    // Disconnect, clean up BLE stack resources
                    gatt.close();
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                ledgerDevice.getGattCallback().onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                ledgerDevice.getGattCallback().onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                ledgerDevice.getGattCallback().onMtuChanged(gatt, mtu, status);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                ledgerDevice.getGattCallback().onServicesDiscovered(gatt, status);
            }
        };

        // Connect to GATT, passing the adapter callback (above)
        final BluetoothGatt connection = btDevice.connectGatt(context, false, gattAdapterCallback);

        // Create underlying Ledger device
        this.ledgerDevice = new LedgerDeviceBLE(connection);
        if (BuildConfig.DEBUG) {
            this.ledgerDevice.setDebug(true);
        }
    }
}
