package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.Network
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.extensions.logException
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KLogging
import kotlin.time.DurationUnit
import kotlin.time.toDuration


abstract class AbstractWalletViewModel constructor(
    val sessionManager: SessionManager,
    val walletRepository: WalletRepository,
    countly: Countly,
    wallet: Wallet
) : AppViewModel(countly) {

    public sealed class WalletEvent: AppEvent {
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

    protected val _walletLiveData: MutableLiveData<Wallet> = MutableLiveData(wallet)
    val walletLiveData: LiveData<Wallet> get()  = _walletLiveData
    val wallet get() = _walletLiveData.value!!

    val accountsFlow: StateFlow<List<Account>> get() = session.accountsFlow
    val accounts: List<Account> get() = accountsFlow.value

    // Logout events, can be expanded in the future
    val onReconnectEvent = MutableLiveData<ConsumableEvent<Long>>()

    private var reconnectTimerJob: Job? = null

    init {
        // Listen to wallet updates from Database
        if(!wallet.isEphemeral) {
            walletRepository
                .getWalletFlow(wallet.id)
                .onEach {
                    _walletLiveData.value = wallet
                }.launchIn(viewModelScope)
        }

        // Only on Login Screen
        if (session.isConnected) {

            // TODO SUPPORT MULTIPLE NETWORKS
            session
                .networkEventsFlow(session.defaultNetwork).filterNotNull()
                .onEach { event ->
                    // Cancel previous timer
                    reconnectTimerJob?.cancel()

                    if(event.isConnected){
                        onReconnectEvent.value = ConsumableEvent(-1)
                    } else {
                        reconnectTimerJob = (0..event.waitInSeconds).asFlow().map {
                            event.waitInSeconds - it
                        }.onEach {
                            onReconnectEvent.value = ConsumableEvent(it)
                            // Delay 1 sec
                            delay(1.toDuration(DurationUnit.SECONDS))
                        }.launchIn(viewModelScope)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun setActiveAccount(account: Account) {
        session.setActiveAccount(account)

        wallet.activeNetwork = account.networkId
        wallet.activeAccount = account.pointer

        if(!wallet.isHardware) {
            viewModelScope.launch(context = logException(countly)){
                walletRepository.updateWallet(wallet)
            }
        }
    }

    fun deleteWallet() {
        deleteWallet(wallet, sessionManager, walletRepository, countly)
    }

    fun renameWallet(name: String) {
        renameWallet(name, wallet, walletRepository, countly)
    }

    open fun renameAccount(account: Account, name: String, callback: ((Account) -> Unit)? = null) {
        if (name.isBlank()) return

        doUserAction({
            session.updateAccount(account, name)
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(WalletEvent.RenameAccount))
            countly.renameAccount(session, it)
            callback?.invoke(it)
        })
    }

    open fun updateAccountVisibility(account: Account, isHidden: Boolean, callback: ((Account) -> Unit)? = null) {
        doUserAction({
            session.updateAccount(account = account, isHidden = isHidden)
        }, onSuccess = {
            callback?.invoke(it)

            if(isHidden){
                // Update active account from Session if it was archived
                setActiveAccount(session.activeAccount)
            }else{
                // Make it active
                setActiveAccount(account)
            }
        })
    }

    fun ackSystemMessage(network: Network, message : String){
        doUserAction({
            session.ackSystemMessage(network, message)
            session.updateSystemMessage()
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(WalletEvent.AckMessage))
        })
    }

    fun logout(reason: LogoutReason) {
        session.disconnectAsync()
        onEvent.postValue(ConsumableEvent(WalletEvent.Logout(reason)))
    }

    companion object : KLogging()
}