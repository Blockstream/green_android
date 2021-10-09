package com.blockstream.green.ui.wallet;

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blockstream.gdk.params.AddressParams
import com.blockstream.gdk.params.BalanceParams
import com.blockstream.gdk.params.CreateTransactionParams
import com.blockstream.green.data.AppEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.observable
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenapi.Session
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.kotlin.subscribeBy


class CreateTransactionViewModel @AssistedInject constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, wallet) {

    sealed class CreateTransactionEvent : AppEvent {
        object SelectAmount : CreateTransactionEvent()
        object SelectAsset : CreateTransactionEvent()
    }

    val address = MutableLiveData<String>()

    fun createTransaction() {
        session.observable {

            val unspentOutputs = it.getUnspentOutputs(BalanceParams(subaccount = wallet.activeAccount, confirmations = 0))

            val params = CreateTransactionParams(
                subaccount = wallet.activeAccount,
                addressees = listOf(AddressParams(address.value ?: "")),
                utxos = unspentOutputs.unspentOutputs,
            )

            it.createTransaction(params).let { tx ->
                tx.error?.let { error ->
                    if(error.isNotBlank() && error != "id_invalid_amount" && error != "id_no_amount_specified" && error != "id_insufficient_funds" && error != "Invalid AssetID"){
                        throw Exception(error)
                    }
                }

                val assetId = tx.addressees[0].assetId

                // Check if the specified asset in the uri exists in the wallet
                assetId?.let { assetId ->
                    val balance = it.getBalance(BalanceParams(
                        subaccount = wallet.activeAccount,
                        confirmations = 0
                    ))

                    if(!balance.containsKey(assetId)){
                        throw Exception("id_no_asset_in_this_account")
                    }
                }

                // Set pending transaction
                Session.getSession().pendingTransaction = tx.toObjectNode()

                if(session.isLiquid && assetId.isNullOrBlank()) CreateTransactionEvent.SelectAsset else CreateTransactionEvent.SelectAmount
            }

        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                onEvent.postValue(ConsumableEvent(it))
            },
            onError = {
                it.printStackTrace()
                onError.postValue(ConsumableEvent(it))
            }
        )
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): CreateTransactionViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}