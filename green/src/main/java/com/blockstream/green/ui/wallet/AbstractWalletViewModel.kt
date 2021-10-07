package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
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
    var wallet: Wallet,
) : AppViewModel() {

    enum class Event {
        RENAME_WALLET, DELETE_WALLET, RENAME_ACCOUNT, ACK_MESSAGE
    }

    enum class NavigationEvent {
        USER_ACTION, DISCONNECTED, TIMEOUT, DEVICE_DISCONNECTED
    }

    val session = sessionManager.getWalletSession(wallet)

    private val walletLiveData: MutableLiveData<Wallet> = MutableLiveData(wallet)
    fun getWalletLiveData(): LiveData<Wallet> = walletLiveData

    private val subAccountLiveData: MutableLiveData<SubAccount> = MutableLiveData()
    fun getSubAccountLiveData(): LiveData<SubAccount> = subAccountLiveData

    // Logout events, can be expanded in the future
    val onNavigationEvent = MutableLiveData<ConsumableEvent<NavigationEvent>>()
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


        session.device?.deviceState?.observe(viewLifecycleOwner){
            // Device went offline
            if(it == com.blockstream.green.devices.Device.DeviceState.DISCONNECTED){
                logout(NavigationEvent.DEVICE_DISCONNECTED)
            }
        }

        // Only on Login Screen
        if (session.isConnected) {

            session.observable {
                it.getSubAccount(session.activeAccount).result<SubAccount>(hardwareWalletResolver = HardwareCodeResolver(session.hwWallet))
            }.subscribe({
                subAccountLiveData.value = it
            }, {
                it.printStackTrace()
            }).addTo(disposables)


            session
                .getNetworkEventObservable()
                .async()
                .subscribeBy(
                    onNext = { event ->

                        // Dispose previous timer
                        reconnectTimer?.dispose()

                        if(event.connected && event.loginRequired != true){
                            onReconnectEvent.value = ConsumableEvent(-1)
                        }else if (event.loginRequired == true) {
                            logger.info { "Trying to re-establish connection" }
                            session.observable { session ->
                                session.reLogin()
                            }.subscribeBy(
                                onError = {
                                    it.printStackTrace()
                                    logger.info { "Re-login failed..." }
                                },
                                onSuccess = {
                                    logger.info { "Re-login was successful" }
                                }
                            )
                        } else {
                            reconnectTimer = Observable.interval(1, TimeUnit.SECONDS)
                                .take((event.waiting ?: 0) + 1)
                                .map {
                                    (event.waiting ?: 0) - it
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

        if(!wallet.isHardwareEmulated) {
            wallet.observable {
                walletRepository.updateWalletSync(wallet)
            }.subscribeBy()
        }

        session.setActiveAccount(account.pointer)
    }

    fun deleteWallet() {
        wallet.observable {
            sessionManager.destroyWalletSession(wallet)
            walletRepository.deleteWallet(wallet)
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                onEvent.postValue(ConsumableEvent(Event.DELETE_WALLET))
            }
        )
    }

    fun renameSubAccount(index: Long, name: String) {
        if (name.isBlank()) return

        session.observable {
            it.renameSubAccount(index, name)
            it.getSubAccount(index).result<SubAccount>(hardwareWalletResolver = HardwareCodeResolver(session.hwWallet))
        }.subscribeBy(
            onError = {
                onError.postValue(ConsumableEvent(it))
            },
            onSuccess = {
                subAccountLiveData.value = it
                onEvent.postValue(ConsumableEvent(Event.RENAME_ACCOUNT))
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
                onEvent.postValue(ConsumableEvent(Event.RENAME_WALLET))
            }
        )
    }

    fun ackSystemMessage(message : String){
        session.observable {
            session.ackSystemMessage(message)
                .resolve(hardwareWalletResolver = HardwareCodeResolver(session.hwWallet))
            session.updateSystemMessage()
        }.doOnSubscribe {
            onProgress.postValue(true)
        }.doOnTerminate {
            onProgress.postValue(false)
        }.subscribeBy(
            onSuccess = {
                onEvent.postValue(ConsumableEvent(Event.ACK_MESSAGE))
            },
            onError = {
                onError.postValue(ConsumableEvent(it))
            }
        )
    }

    fun logout(navigationEvent: NavigationEvent) {
        session.disconnectAsync()
        onNavigationEvent.postValue(ConsumableEvent(navigationEvent))
    }

    companion object : KLogging()
}