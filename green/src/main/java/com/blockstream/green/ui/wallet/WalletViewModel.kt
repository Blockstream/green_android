package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.database.WalletRepository
import com.blockstream.gdk.data.NetworkEvent
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.database.Wallet
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy


open class WalletViewModel constructor(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    var wallet: Wallet,
) : AppViewModel() {

    enum class Event{
        RENAME_WALLET, DELETE_WALLET, RENAME_ACCOUNT
    }

    val session = sessionManager.getWalletSession(wallet)

    private val walletLiveData: MutableLiveData<Wallet> = MutableLiveData(wallet)
    fun getWalletLiveData(): LiveData<Wallet> = walletLiveData

    private val subAccountLiveData: MutableLiveData<SubAccount> = MutableLiveData()
    fun getSubAccountLiveData(): LiveData<SubAccount> = subAccountLiveData

    val onNetworkEvent = MutableLiveData<NetworkEvent>()

    init {
        // Listen wallet updates from Database
        walletRepository
            .getWalletObservable(wallet.id)
            .async()
            .subscribe {
                wallet = it
                walletLiveData.value = wallet
                walletUpdated()
            }.addTo(disposables)

        // Only on Login Screen
        if(session.isConnected()) {

            session.observable {
                it.getSubAccount(wallet.activeAccount).result<SubAccount>()
            }.subscribe({
                subAccountLiveData.value = it
            }, {
                it.printStackTrace()
            })


            session
                .getNetworkEventObservable()
                .async()
                .subscribe(onNetworkEvent::setValue).addTo(disposables)
        }
    }

    open fun walletUpdated(){

    }

    open fun selectSubAccount(account: SubAccount) {
        subAccountLiveData.value = account

        wallet.activeAccount = account.pointer
        wallet.observable {
            walletRepository.updateWalletSync(wallet)
        }.subscribeBy()
    }

    fun deleteWallet(){
        wallet.observable {
            sessionManager.destroyWalletSession(wallet)
            walletRepository.deleteWallet(wallet)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(Event.DELETE_WALLET))
            }
        )
    }

    fun renameSubAccount(index: Long, name: String){
        if(name.isBlank()) return

        session.observable {
            it.renameSubAccount(index, name)
            it.getSubAccount(index).result<SubAccount>()
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                subAccountLiveData.value = it
                onEvent.value = ConsumableEvent(Event.RENAME_ACCOUNT)
            }
        )
    }

    fun renameWallet(name: String){
        if(name.isBlank()) return

        wallet.observable {
            wallet.name = name.trim()
            walletRepository.updateWalletSync(wallet)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(Event.RENAME_WALLET))
            }
        )
    }
}