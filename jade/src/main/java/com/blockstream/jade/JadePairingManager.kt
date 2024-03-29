package com.blockstream.jade

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.polidea.rxandroidble3.RxBleDevice
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import mu.KLogging

// Solution based on propasal from @ craigzour
// https://gist.github.com/craigzour/edf7f3bd8bef4b162887b4244e27dc1f

class PairingFailedException : RuntimeException()

internal object JadePairingManager : KLogging(){

    /**
     * @throws PairingFailedException
     */
    @SuppressLint("MissingPermission")
    fun pairWithDevice(context: Context, rxBleDevice: RxBleDevice): Single<Boolean> {
        return Single.create { single ->
            when (rxBleDevice.bluetoothDevice.bondState) {
                BluetoothDevice.BOND_BONDED -> single.onSuccess(false)
                else -> {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val deviceBeingPaired: BluetoothDevice? =
                                IntentCompat.getParcelableExtra(
                                    intent,
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java
                                )
                            val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)

                            // Jade BLE devices using RPA is better to use the unique device name rather than the changing mac address
                            if (deviceBeingPaired?.name == rxBleDevice.bluetoothDevice.name) {
                                when (state) {
                                    BluetoothDevice.BOND_BONDED -> single.onSuccess(true)
                                    BluetoothDevice.BOND_NONE -> single.tryOnError(
                                        PairingFailedException()
                                    )
                                }
                            }
                        }
                    }

                    single.setDisposable(Disposable.fromAction { context.unregisterReceiver(receiver) })
                    ContextCompat.registerReceiver(context, receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
                }
            }
        }
    }
}