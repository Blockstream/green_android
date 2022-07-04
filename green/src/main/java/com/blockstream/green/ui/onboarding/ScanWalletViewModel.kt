package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.params.SubAccountsParams
import com.blockstream.green.data.Countly
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.ConsumableEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy

class ScanWalletViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted val onboardingOptions: OnboardingOptions,
    @Assisted mnemonic: String
) : OnboardingViewModel(sessionManager, walletRepository, countly,null) {
    val multiSig = MutableLiveData<String?>(null) // "" = can be added , "wallet_name" -> already in app , null -> not available
    val singleSig = MutableLiveData<String?>(null)

    init {
        scan(onboardingOptions.networkType!!, mnemonic, "")
    }

    fun scan(networkType: String , mnemonic: String, mnemonicPassword: String) {
        // skip if we already scanning
        if(onProgress.value == true) return

        /*
            How wallet identification works with Boolean?
            null -> wallet not found
            "wallet_name" -> wallet already exists in app
            "" -> wallet can be added
         */

        session.observable {
            val multisig: String? = try{
                val multiSigNetwork = session.networks.getNetworkByType(networkType, isElectrum = false)
                val multisigLoginData = it.loginWithMnemonic(multiSigNetwork, mnemonic, mnemonicPassword, initializeSession = false)
                walletRepository.getWalletWithHashIdSync(multisigLoginData.walletHashId, false)?.name ?: ""
            }catch (e: Exception){
                null
            }

            val singlesig: String? = try{
                val singleSigNetwork = session.networks.getNetworkByType(networkType, isElectrum = true)
                val singleSigLoginData = it.loginWithMnemonic(singleSigNetwork, mnemonic, mnemonicPassword, initializeSession = false)

                it.getSubAccounts(params = SubAccountsParams(refresh = true)).subaccounts.find { subaccount ->
                    subaccount.bip44Discovered == true
                }.let { subAccountWithTransactions ->
                    if (subAccountWithTransactions != null) {
                        walletRepository.getWalletWithHashIdSync(singleSigLoginData.walletHashId, false)?.name ?: ""
                    } else {
                        null
                    }
                }.also {
                    session.disconnect()
                }

            }catch (e: Exception){
                e.printStackTrace()
                null
            }

            Pair(multisig, singlesig)
        }.doOnSubscribe {
            onProgress.value = true
        }.doOnTerminate {
            onProgress.value = false
        }.subscribeBy(
            onError = {
                onError.value = ConsumableEvent(it)
            },
            onSuccess = {
                multiSig.value = it.first
                singleSig.value = it.second
            }
        )
    }


    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            onboardingOptions: OnboardingOptions,
            mnemonic: String
        ): ScanWalletViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            onboardingOptions: OnboardingOptions,
            mnemonic: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(onboardingOptions, mnemonic) as T
            }
        }
    }
}