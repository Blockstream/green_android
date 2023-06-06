package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blockstream.green.data.Countly
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.logException
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.ui.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupNewWalletViewModel @Inject constructor(
    countly: Countly,
    settingsManager: SettingsManager,
    walletRepository: WalletRepository
) : AppViewModel(countly) {
    val termsChecked = MutableLiveData(false)

    init {
        // If you have already agreed, check by default
        viewModelScope.launch(context = logException(countly)) {
            termsChecked.postValue(settingsManager.isDeviceTermsAccepted() || walletRepository.walletsExists())
        }
    }
}