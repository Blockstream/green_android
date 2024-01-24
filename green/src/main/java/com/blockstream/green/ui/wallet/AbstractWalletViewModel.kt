package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.WalletExtras
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.data.AppEvent
import com.blockstream.green.ui.AppViewModelAndroid
import com.rickclephas.kmm.viewmodel.coroutineScope
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
    wallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null
) : AppViewModelAndroid(greenWalletOrNull = wallet, accountAssetOrNull = accountAssetOrNull) {

    public sealed class WalletEvent: AppEvent {
        object RenameAccount : WalletEvent()
        object AckMessage : WalletEvent()
    }

    // val session = sessionManager.getWalletSession(wallet)

    protected val _walletLiveData: MutableLiveData<GreenWallet> = MutableLiveData(wallet)
    val walletLiveData: LiveData<GreenWallet> get()  = _walletLiveData
    val wallet get() = _walletLiveData.value!!

    val accountsFlow: StateFlow<List<Account>> get() = session.accounts
    val accounts: List<Account> get() = accountsFlow.value

    // Logout events, can be expanded in the future
    val onReconnectEvent = MutableLiveData<ConsumableEvent<Long>>()

    private var reconnectTimerJob: Job? = null

    init {
        // Listen to wallet updates from Database
        if(!wallet.isEphemeral) {
            database.getWalletFlow(wallet.id).onEach {
                _walletLiveData.value = it
            }.launchIn(viewModelScope.coroutineScope)
        }

        // Only on Login Screen
        if (session.isConnected) {

            // TODO SUPPORT MULTIPLE NETWORKS
            session
                .networkEvents(session.defaultNetwork).filterNotNull()
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
                        }.launchIn(viewModelScope.coroutineScope)
                    }
                }
                .launchIn(viewModelScope.coroutineScope)
        }
    }

    fun setActiveAccount(account: Account) {
        session.setActiveAccount(account)

        wallet.activeNetwork = account.networkId
        wallet.activeAccount = account.pointer

        if(!wallet.isEphemeral) {
            viewModelScope.coroutineScope.launch(context = logException(countly)){
                database.updateWallet(wallet)
            }
        }
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
                setActiveAccount(session.activeAccount.value!!)
            }else{
                // Make it active
                setActiveAccount(account)
            }
        })
    }

    fun removeAccount(account: Account, callback: (() -> Unit)? = null){
        if(account.isLightning) {
            doUserAction({
                database.deleteLoginCredentials(wallet.id, CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS)
                database.deleteLoginCredentials(wallet.id, CredentialType.LIGHTNING_MNEMONIC)
                session.removeAccount(account)
            }, onSuccess = {
                callback?.invoke()

                // Update active account from Session if it was archived
                setActiveAccount(session.activeAccount.value!!)
            })
        }
    }

    fun ackSystemMessage(network: Network, message : String){
        doUserAction({
            session.ackSystemMessage(network, message)
            session.updateSystemMessage()
        }, onSuccess = {
            onEvent.postValue(ConsumableEvent(WalletEvent.AckMessage))
        })
    }

    open fun saveGlobalSettings(newSettings: Settings, onSuccess: (() -> Unit)? = null) {
        doUserAction({
            session.changeGlobalSettings(newSettings)
            if(!wallet.isEphemeral){
                greenWallet.also {
                    // Pass settings to Lightning Shortcut
                    sessionManager.getWalletSessionOrNull(it.lightningShortcutWallet())?.also { lightningSession ->
                        lightningSession.changeGlobalSettings(newSettings)
                    }

                    it.extras = WalletExtras(settings = newSettings.forWalletExtras())

                    database.updateWallet(it)
                }
            }
        }, onSuccess = {
            onSuccess?.invoke()
        })
    }

    @Deprecated("Send  the event")
    fun logout(reason: LogoutReason) {
        postEvent(Events.Logout(reason = reason))
    }

    companion object : KLogging()
}