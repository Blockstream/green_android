package com.blockstream.green.ui.onboarding

import androidx.lifecycle.*
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
    @Assisted val isMultisig: Boolean
) : AppViewModel() {

    var username = MutableLiveData("")
    var password = MutableLiveData("")
    var extenedPublicKey = MutableLiveData("")
    val isRememberMe = MutableLiveData(true)
    val isTestnet = MutableLiveData(false)
    val newWallet: MutableLiveData<Wallet> = MutableLiveData()

    val isLoginEnabled: LiveData<Boolean> by lazy {
        MediatorLiveData<Boolean>().apply {
            val block = { _: Any? ->
                value = if(isMultisig)
                    !username.value.isNullOrBlank() && !password.value.isNullOrBlank() && !onProgress.value!! else !extenedPublicKey.value.isNullOrBlank()
            }
            if(isMultisig) {
                addSource(username, block)
                addSource(password, block)
            }else{
                addSource(extenedPublicKey, block)
            }
            addSource(onProgress, block)
        }
    }


    fun login() {
        val session: GreenSession = sessionManager.getOnBoardingSession()

        session.observable {
            val network = if (isTestnet.value == true) it.networks.testnetGreen else it.networks.bitcoinGreen
            val loginData = it.loginWatchOnly(network, username.value ?: "", password.value ?: "")

            val wallet = Wallet(
                walletHashId = loginData.walletHashId,
                name = "Bitcoin Watch Only",
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

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(isMultisig: Boolean): LoginWatchOnlyViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            isMultisig: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(isMultisig) as T
            }
        }
    }
}