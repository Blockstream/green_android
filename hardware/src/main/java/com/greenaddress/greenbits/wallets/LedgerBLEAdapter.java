package com.greenaddress.greenbits.wallets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;

import com.blockstream.hardware.BuildConfig;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.LedgerDeviceBLE;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LedgerBLEAdapter {

    public interface OnConnectedListener {
        void onConnected(final BTChipTransport transport, final boolean hasScreen);
    }

    public interface OnErrorListener {
        void onError(final BTChipTransport transport);
    }

    public static void connectLedgerBLE(final Context context, final BluetoothDevice btDevice, final OnConnectedListener onConnected, final OnErrorListener onError) {
        // LedgerBLEAdapter will connect the device and call the listener when the connection is established
        final LedgerBLEAdapter adapter = new LedgerBLEAdapter(context, btDevice, onConnected, onError);
    }

    // The underlying ledger device
    private Disposable connectionDisposable;
    private final LedgerDeviceBLE ledgerDevice;

    private LedgerBLEAdapter(final Context context, final BluetoothDevice btDevice, final OnConnectedListener onConnected, final OnErrorListener onError) {

        // Adapter callback to route callbacks from the BLE stack to the LedgerDeviceBLE handler
        final BluetoothGattCallback gattAdapterCallback = new BluetoothGattCallback() {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onCharacteristicChanged(gatt, characteristic);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onCharacteristicRead(gatt, characteristic, status);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onCharacteristicWrite(gatt, characteristic, status);
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (ledgerDevice != null) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            // Connect, and callback to login activity listener when connected
                            dispose();
                            connectionDisposable = Observable.just(gatt)
                                    .observeOn(Schedulers.computation())
                                    //.doFinally(() -> dispose())
                                    .map(g -> {
                                        // Apparently older versions benefit from a delay here to reduce a race condition
                                        final int delay = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N ? 1000 : 0;
                                        android.os.SystemClock.sleep(delay);
                                        ledgerDevice.connect();
                                        return ledgerDevice;
                                    })
                                    .subscribe(device -> onConnected.onConnected(device, true),
                                            error -> { error.printStackTrace();
                                                       onError.onError(ledgerDevice);
                                    });

                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            // Disconnect, clean up BLE stack resources
                            gatt.close();
                        }
                    }

                    ledgerDevice.getGattCallback().onConnectionStateChange(gatt, status, newState);
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onDescriptorRead(gatt, descriptor, status);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onDescriptorWrite(gatt, descriptor, status);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onMtuChanged(gatt, mtu, status);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (ledgerDevice != null) {
                    ledgerDevice.getGattCallback().onServicesDiscovered(gatt, status);
                }
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

    private void dispose() {
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
            connectionDisposable = null;
        }
    }
}
