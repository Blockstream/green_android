package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.green.data.Countly
import com.blockstream.green.data.GdkEvent
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class SetPinViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted val onboardingOptions: OnboardingOptions,
    @Assisted("mnemonic") val mnemonic: String?,
    @Assisted("password") val password: String?,
    @Assisted restoreWallet: Wallet?
) : OnboardingViewModel(sessionManager, walletRepository, countly, restoreWallet) {

    val isPinVerified = MutableLiveData(false)

    init {
        if(onboardingOptions.isRestoreFlow){
            checkRecoveryPhrase(onboardingOptions.isTestnet == true, mnemonic ?: "", password, GdkEvent.Success)
        }
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            onboardingOptions: OnboardingOptions,
            @Assisted("mnemonic") mnemonic: String,
            @Assisted("password") password: String?,
            restoreWallet: Wallet?
        ): SetPinViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            onboardingOptions: OnboardingOptions,
            mnemonic: String,
            password: String?,
            restoreWallet: Wallet?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(onboardingOptions, mnemonic, password, restoreWallet) as T
            }
        }
    }
}