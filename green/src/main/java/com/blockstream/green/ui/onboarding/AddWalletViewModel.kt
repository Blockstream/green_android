package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*

class AddWalletViewModel @AssistedInject constructor(
    val deviceManager: DeviceManager,
    val walletRepository: WalletRepository,
    @Assisted deviceId: Int
) : AppViewModel() {
    val termsChecked = MutableLiveData(false)
    val device = MutableLiveData(deviceManager.getDevice(deviceId))
    val isHardware = MutableLiveData(device.value != null)

    init {
        // If you have already agreed, check by default
        viewModelScope.launch(Dispatchers.IO) {
            termsChecked.postValue(walletRepository.walletsExistsSuspend())
        }
    }


    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(deviceId: Int): AddWalletViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            deviceId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(deviceId) as T
            }
        }
    }
}