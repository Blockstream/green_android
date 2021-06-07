package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.Device
import com.blockstream.gdk.data.NetworkEvent
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.NavGraphDirections
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.gdk.async
import com.blockstream.green.gdk.observable
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.ui.WalletFragment
import com.blockstream.green.utils.ConsumableEvent
import com.greenaddress.Bridge
import com.greenaddress.greenapi.HWWallet
import com.greenaddress.greenapi.HWWalletBridge
import com.greenaddress.greenbits.wallets.HardwareCodeResolver
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : AppViewModel(), HWWalletBridge {

    enum class Event {
        RENAME_WALLET, DELETE_WALLET, RENAME_ACCOUNT
    }

    val session = sessionManager.getWalletSession(wallet)

    private val walletLiveData: MutableLiveData<Wallet> = MutableLiveData(wallet)
    fun getWalletLiveData(): LiveData<Wallet> = walletLiveData

    private val subAccountLiveData: MutableLiveData<SubAccount> = MutableLiveData()
    fun getSubAccountLiveData(): LiveData<SubAccount> = subAccountLiveData

    val onDeviceInteractionEvent = MutableLiveData<ConsumableEvent<Device>>()

    // Logout events, can be expanded in the future
    val onNavigationEvent = MutableLiveData<ConsumableEvent<Boolean>>()
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

        // Only on Login Screen
        if (session.isConnected) {

            session.observable {
                it.getSubAccount(wallet.activeAccount).result<SubAccount>()
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
                            if (session.hasDevice) {
                                onReconnectEvent.value = ConsumableEvent(0)

                                session.observable { session ->
                                    session.loginWithDevice(
                                        session.network,
                                        registerUser = false,
                                        connectSession = false,
                                        hwWallet = session.hwWallet!!,
                                        hardwareCodeResolver = HardwareCodeResolver(this)
                                    )
                                }.subscribeBy(
                                    onError = {
                                        it.printStackTrace()
                                        logger().info { "Logout from Device reconnection failure" }
                                        logout()
                                    },
                                    onSuccess = {
                                        logger().info { "Device login was successful" }
                                    }
                                )

                            } else {
                                logger().info { "Logout from network event" }
                                logout()
                            }
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
        wallet.observable {
            walletRepository.updateWalletSync(wallet)
        }.subscribeBy()
    }

    fun deleteWallet() {
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

    fun renameSubAccount(index: Long, name: String) {
        if (name.isBlank()) return

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

    override fun interactionRequest(hw: HWWallet?) {
        hw?.let {
            onDeviceInteractionEvent.postValue(ConsumableEvent(it.device))
        }
    }

    fun logout() {
        session.disconnectAsync()
        onNavigationEvent.postValue(ConsumableEvent(true))
    }

    // The following two methods are not needed
    // it will be remove in the next iteration on simplifying
    // hardware wallet interfaces
    override fun pinMatrixRequest(hw: HWWallet?): String {
        TODO("Not yet implemented")
    }

    override fun passphraseRequest(hw: HWWallet?): String {
        TODO("Not yet implemented")
    }

    companion object : KLogging()
}