package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blockstream.green.data.Countly
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.logException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class AddWalletViewModel @AssistedInject constructor(
    val deviceManager: DeviceManager,
    val walletRepository: WalletRepository,
    val countly: Countly,
    @Assisted deviceId: String?
) : AppViewModel() {
    val termsChecked = MutableLiveData(false)
    val device = MutableLiveData(deviceManager.getDevice(deviceId))
    val isDeviceOnboarding = MutableLiveData(deviceId != null)

    init {
        // If you have already agreed, check by default
        viewModelScope.launch(context = logException(countly)) {
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