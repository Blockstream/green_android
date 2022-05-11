package com.blockstream.green.ui.onboarding

import androidx.lifecycle.*
import com.blockstream.gdk.data.Network
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.data.Countly
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

class LoginWatchOnlyViewModel @AssistedInject constructor(
    private val walletRepository: WalletRepository,
    private val sessionManager: SessionManager,
    private val appKeystore: AppKeystore,
    private val countly: Countly,
    @Assisted val onboardingOptions: OnboardingOptions
) : AppViewModel() {

    var username = MutableLiveData("")
    var password = MutableLiveData("")
    var extenedPublicKey = MutableLiveData("")
    val isRememberMe = MutableLiveData(true)
    val newWallet: MutableLiveData<Wallet> = MutableLiveData()

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

    fun login() {
        onboardingOptions.network?.let { network: Network ->
            val session: GreenSession = sessionManager.getOnBoardingSession()
            session.observable {
                val loginData =
                    it.loginWatchOnly(network, username.value ?: "", password.value ?: "")

                val wallet = Wallet(
                    walletHashId = loginData.walletHashId,
                    name = network.productName,
                    network = session.network.id,
                    isRecoveryPhraseConfirmed = true,
                    watchOnlyUsername = username.value
                )

                wallet.id = walletRepository.addWallet(wallet)

                if (isRememberMe.value == true) {
                    val encryptedData = appKeystore.encryptData(password.value!!.toByteArray())
                    walletRepository.addLoginCredentialsSync(
                        LoginCredentials(
                            walletId = wallet.id,
                            credentialType = CredentialType.KEYSTORE,
                            encryptedData = encryptedData
                        )
                    )
                }

                sessionManager.upgradeOnBoardingSessionToWallet(wallet)
                countly.restoreWatchOnlyWallet(session)
                wallet
            }.doOnSubscribe {
                onProgress.postValue(true)
            }.doOnTerminate {
                onProgress.postValue(false)
            }.subscribeBy(
                onError = {
                    onError.postValue(ConsumableEvent(it))
                },
                onSuccess = {
                    newWallet.postValue(it)
                }
            ).addTo(disposables)
        }
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