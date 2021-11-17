package com.blockstream.green.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.data.Transactions
import com.blockstream.gdk.params.TransactionParams
import com.blockstream.green.data.OnboardingOptions
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy

class ScanWalletViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted val onboardingOptions: OnboardingOptions,
    @Assisted mnemonic: String
) : OnboardingViewModel(sessionManager, walletRepository, null) {
    val multiSig = MutableLiveData<Boolean?>(null) // true = can be added , false -> already in app , null -> not available
    val singleSig = MutableLiveData<Boolean?>(null)

    init {
        scan(onboardingOptions.networkType!!, mnemonic, "")
    }

    // Multisig (wallet_hash_id, alreadyExists) / Singleisg (wallet_hash_id, alreadyExists)
    fun scan(networkType: String , mnemonic: String, mnemonicPassword: String) {
        // skip if we already scanning
        if(onProgress.value == true) return

        session.observable {
            val multisig: Pair<String, Boolean>? = try{
                val multiSigNetwork = session.networks.getNetworkByType(networkType, isElectrum = false)
                val multisigLoginData = it.loginWithMnemonic(multiSigNetwork, mnemonic, mnemonicPassword)
                Pair(multisigLoginData.walletHashId, !walletRepository.walletsExistsSync(multisigLoginData.walletHashId, false))
            }catch (e: Exception){
                null
            }

            val singlesig: Pair<String, Boolean>? = try{
                val singleSigNetwork = session.networks.getNetworkByType(networkType, isElectrum = true)
                val singleSigLoginData = it.loginWithMnemonic(singleSigNetwork, mnemonic, mnemonicPassword)

                it.getSubAccounts().subaccounts.find { subaccount ->
                    it.getTransactions(TransactionParams(subaccount = subaccount.pointer)).result<Transactions>(
                        hardwareWalletResolver = HardwareCodeResolver(it.hwWallet)
                    ).transactions.isNotEmpty()
                }.let {  subAccountWithTransactions ->
                    if(subAccountWithTransactions != null){
                        Pair(singleSigLoginData.walletHashId, !walletRepository.walletsExistsSync(singleSigLoginData.walletHashId, false))
                    }else{
                        null
                    }
                }
            }catch (e: Exception){
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
                multiSig.value = it.first?.second
                singleSig.value = it.second?.second
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