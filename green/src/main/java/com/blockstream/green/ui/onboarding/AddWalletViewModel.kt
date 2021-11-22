package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class AddWalletViewModel @AssistedInject constructor(
    val deviceManager: DeviceManager,
    val walletRepository: WalletRepository,
    @Assisted deviceId: String?
) : AppViewModel() {
    val termsChecked = MutableLiveData(false)
    val device = MutableLiveData(deviceManager.getDevice(deviceId))
    val isDeviceOnboarding = MutableLiveData(deviceId != null)

    init {
        // If you have already agreed, check by default
        viewModelScope.launch {
            termsChecked.postValue(walletRepository.walletsExistsSuspend())
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(deviceId: String?): AddWalletViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            deviceId: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(deviceId) as T
            }
        }
    }
}