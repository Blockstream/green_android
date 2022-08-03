package com.blockstream.green.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

class DeviceScanViewModel @AssistedInject constructor(
    val deviceManager: DeviceManager,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    var onSuccess: (() -> Unit)? = null

    init {
        deviceManager
            .getDevices()
            .subscribeBy(
                onError = { e ->
                    e.printStackTrace()
                },
                onNext = { devices ->
                    devices.firstOrNull { device ->
                        // wallet.deviceIdentifiers?.any {  it.uniqueIdentifier == device.uniqueIdentifier} == true
                        false
                    }?.also {
                        onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
                    }
                }
            ).addTo(disposables)
    }

    fun askForPermissionOrBond(device: Device) {
        onSuccess = {
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(device)))
        }

        device.askForPermissionOrBond(onSuccess!!) {
            onError.postValue(ConsumableEvent(it))
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): DeviceScanViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}