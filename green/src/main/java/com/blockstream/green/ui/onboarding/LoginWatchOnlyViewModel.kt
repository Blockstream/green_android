package com.blockstream.green.ui.onboarding

import androidx.lifecycle.*
import com.blockstream.green.data.Countly
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.extensions.boolean
import com.blockstream.green.extensions.string
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class LoginWatchOnlyViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    private val appKeystore: AppKeystore,
    @Assisted val onboardingOptions: OnboardingOptions
) : OnboardingViewModel(sessionManager, walletRepository, countly, null) {

    var username = MutableLiveData("")
    var password = MutableLiveData("")
    var extenedPublicKey = MutableLiveData("")
    val isRememberMe = MutableLiveData(true)

    val isLoginEnabled: LiveData<Boolean> by lazy {
        MediatorLiveData<Boolean>().apply {
            val block = { _: Any? ->
                value = if (onboardingOptions.isSinglesig == true) {
                    !extenedPublicKey.value.isNullOrBlank()
                } else {
                    !username.value.isNullOrBlank() && !password.value.isNullOrBlank() && !onProgress.value!!
                }
            }
            if (onboardingOptions.isSinglesig == true) {
                addSource(extenedPublicKey, block)
            } else {
                addSource(username, block)
                addSource(password, block)
            }

            addSource(onProgress, block)
        }
    }

    fun createNewWatchOnlyWallet() {
        createNewWatchOnlyWallet(appKeystore = appKeystore, options = onboardingOptions, username = username.string(), password = password.string(), savePassword = isRememberMe.boolean())
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(onboardingOptions: OnboardingOptions): LoginWatchOnlyViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            onboardingOptions: OnboardingOptions
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(onboardingOptions) as T
            }
        }
    }
}