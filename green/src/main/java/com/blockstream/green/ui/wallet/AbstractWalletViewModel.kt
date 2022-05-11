package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.SubAccount
import com.blockstream.gdk.params.UpdateSubAccountParams
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import mu.KLogging
import java.util.concurrent.TimeUnit


abstract class AbstractWalletViewModel constructor(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    val countly: Countly,
    var wallet: Wallet,
) : AppViewModel() {

    sealed class WalletEvent: AppEvent {
        object RenameWallet : WalletEvent()
        object DeleteWallet : WalletEvent()
        object RenameAccount : WalletEvent()
        object AckMessage : WalletEvent()

        class Logout(val reason: LogoutReason) : WalletEvent()
    }

    enum class LogoutReason {
        USER_ACTION, DISCONNECTED, TIMEOUT, DEVICE_DISCONNECTED
    }

    val session = sessionManager.getWalletSession(wallet)

    private val walletLiveData: MutableLiveData<Wallet> = MutableLiveData(wallet)
    fun getWalletLiveData(): LiveData<Wallet> = walletLiveData

    private val subAccountLiveData: MutableLiveData<SubAccount> = MutableLiveData()
    fun getSubAccountLiveData(): LiveData<SubAccount> = subAccountLiveData

    private val subAccountsLiveData: MutableLiveData<List<SubAccount>> = MutableLiveData()
    fun getSubAccountsLiveData(): LiveData<List<SubAccount>> = subAccountsLiveData

    // Logout events, can be expanded in the future
    val onReconnectEvent = MutableLiveData<ConsumableEvent<Long>>()

    private var reconnectTimer: Disposable? = null

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

        session
            .getSubAccountsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                subAccountsLiveData.value = it
            }.addTo(disposables)

        // Only on Login Screen
        if (session.isConnected) {

            session.activeAccountData?.let {
                subAccountLiveData.value = session.activeAccountData
            } ?: run {
                session.observable {
                    it.getActiveSubAccount()
                }.subscribe({
                    subAccountLiveData.value = it
                }, {
                    it.printStackTrace()
                }).addTo(disposables)
            }

            session
                .getNetworkEventObservable()
                .async()
                .subscribeBy(
                    onNext = { event ->
                        // Dispose previous timer
                        reconnectTimer?.dispose()

                        if(event.isConnected){
                            onReconnectEvent.value = ConsumableEvent(-1)
                        } else {
                            reconnectTimer = Observable.interval(1, TimeUnit.SECONDS)
                                .take(event.waitInSeconds+ 1)
                                .map {
                                    event.waitInSeconds - it
                                }
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy(
                                    onNext = {
                                        onReconnectEvent.value = ConsumableEvent(it)
                                    }
                                ).addTo(disposables)
                        }
                    }
                )
                .addTo(disposables)
        }
    }

    open fun walletUpdated() {

    }

    open fun selectSubAccount(account: SubAccount) {
        subAccountLiveData.value = account

        wallet.activeAccount = account.pointer

        if(!wallet.isHardware) {
            wallet.observable {
                walletRepository.updateWalletSync(wallet)
            }.subscribeBy()
        }

        session.setActiveAccount(account.pointer)
    }

    fun deleteWallet() {
        deleteWallet(wallet, sessionManager, walletRepository, countly)
    }

    fun renameSubAccount(index: Long, name: String) {
        if (name.isBlank()) return

        session.observable {
            it.updateSubAccount(UpdateSubAccountParams(
                name = name,
                subaccount = index
            ))
            it.getSubAccount(index)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                if(subAccountLiveData.value?.pointer == it.pointer) {
                    subAccountLiveData.value = it
                }
                onEvent.postValue(ConsumableEvent(WalletEvent.RenameAccount))

                // Update the subaccounts list
                session.updateSubAccountsAndBalances()

                countly.renameAccount(session, it)
            }
        )
    }

    fun updateSubAccountVisibility(subAccount: SubAccount, isHidden: Boolean, callback: (() -> Unit)? = null) {
        session.observable {
            it.updateSubAccount(UpdateSubAccountParams(
                subaccount = subAccount.pointer,
                hidden = isHidden
            ))
            it.getSubAccount(subAccount.pointer)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
                callback?.invoke()
            },
            onSuccess = {
                if(subAccountLiveData.value?.pointer == it.pointer) {
                    subAccountLiveData.value = it
                }

                // Update the subaccounts list
                session.updateSubAccounts()

                callback?.invoke()
            }
        )
    }

    fun renameWallet(name: String) {
        if (name.isBlank()) return

        wallet.observable {
            wallet.name = name.trim()
            walletRepository.updateWalletSync(wallet)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(WalletEvent.RenameWallet))
                countly.renameWallet()
            }
        )
    }

    fun ackSystemMessage(message : String){
        session.observable {
            session.ackSystemMessage(message)
                .resolve(hardwareWalletResolver = DeviceResolver(session))
            session.updateSystemMessage()
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                onEvent.postValue(ConsumableEvent(WalletEvent.AckMessage))
            },
            onError = {
                onError.postValue(ConsumableEvent(it))
            }
        )
    }

    fun logout(reason: LogoutReason) {
        session.disconnectAsync()
        onEvent.postValue(ConsumableEvent(WalletEvent.Logout(reason)))
    }

    companion object : KLogging()
}