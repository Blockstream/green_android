package com.blockstream.common.models.add

import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.views.AccountTypeLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


abstract class AddAccountViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    internal val _accountTypeBeingCreated: MutableStateFlow<AccountTypeLook?> = MutableStateFlow(null)

    class LocalEvents{
        object EnableLightningShortcut: Event
    }

    class LocalSideEffects{
        class LightningShortcutDialog(sideEffect: SideEffect): SideEffects.SideEffectEvent(Events.EventSideEffect(sideEffect))
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if(event is LocalEvents.EnableLightningShortcut){
            enableLightningShortcut()
        }
    }

    init {
        _accountTypeBeingCreated.filterNotNull().onEach {
            onProgressDescription.value = "id_creating_your_s_account|${it.accountType}"
        }.launchIn(viewModelScope.coroutineScope)
    }

    protected fun createAccount(
        accountType: AccountType,
        accountName: String,
        network: Network,
        mnemonic: String? = null,
        xpub: String? = null
    ) {
        _accountTypeBeingCreated.value = AccountTypeLook(accountType)

        doAsync({
            if (accountType.isLightning()) {
                val isEmptyWallet = session.accounts.value.isEmpty()

                session.initLightningIfNeeded(mnemonic = mnemonic)

                if (!greenWallet.isEphemeral && !session.isHardwareWallet) {
                    // Persist Lightning
                    session.lightningSdk.appGreenlightCredentials?.also {
                        val encryptedData =
                            greenKeystore.encryptData(it.toJson().encodeToByteArray())

                        val loginCredentials = createLoginCredentials(
                            walletId = greenWallet.id,
                            credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                            network = network.id,
                            encryptedData = encryptedData
                        )

                        database.replaceLoginCredential(loginCredentials)
                    }
                }

                // If wallet is new and LN is created, default to Satoshi
                if (isEmptyWallet) {
                    session.getSettings()?.also {
                        session.changeGlobalSettings(it.copy(unit = SATOSHI_UNIT))
                    }
                }

                return@doAsync session.lightningAccount
            }

            // Check if network needs initialization
            if (!session.hasActiveNetwork(network) && !session.failedNetworks.value.contains(network)) {
                session.initNetworkIfNeeded(network) { }
            }

            val accountsWithSameType =
                session.allAccounts.value.filter { it.type == accountType && it.network == network }.size

            val name = (accountName.cleanup() ?: accountType.gdkType).let { name ->
                "$name ${(accountsWithSameType + 1).takeIf { it > 1 } ?: ""}".trim()
            }

            val params = SubAccountParams(
                name = name,
                type = accountType,
                recoveryMnemonic = mnemonic,
                recoveryXpub = xpub,
            )

            // Find an archived account if exists, except if AccountType is 2of3 where a new key is used.
            val noHistoryArchivedAccount = if (accountType == AccountType.TWO_OF_THREE) {
                null
            } else {
                session.allAccounts.value.find {
                    it.hidden && it.network == network && it.type == accountType && !it.hasHistory(
                        session
                    )
                }
            }

            // Check if account unarchive is needed
            if (noHistoryArchivedAccount != null) {
                session.updateAccount(noHistoryArchivedAccount, false)
            } else {
                session.createAccount(
                    network = network,
                    params = params,
                    hardwareWalletResolver = DeviceResolver.createIfNeeded(
                        session.gdkHwWallet,
                        this
                    )
                )
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            if (it.isLightning && !greenWallet.isEphemeral) {
                postSideEffect(LocalSideEffects.LightningShortcutDialog(SideEffects.Navigate(it)))
            } else {
                postSideEffect(SideEffects.Navigate(it))
            }

            countly.createAccount(session, it)
        })
    }

    protected fun networkForAccountType(accountType: AccountType, asset: EnrichedAsset): Network {
        return when (accountType) {
            AccountType.BIP44_LEGACY,
            AccountType.BIP49_SEGWIT_WRAPPED,
            AccountType.BIP84_SEGWIT,
            AccountType.BIP86_TAPROOT -> {
                if (asset.isBitcoin) {
                    session.bitcoinSinglesig!!
                } else {
                    session.liquidSinglesig!!
                }
            }
            AccountType.STANDARD -> if (asset.isBitcoin) {
                session.bitcoinMultisig!!
            } else {
                session.liquidMultisig!!
            }
            AccountType.AMP_ACCOUNT -> session.liquidMultisig!!
            AccountType.TWO_OF_THREE -> session.bitcoinMultisig!!
            AccountType.LIGHTNING -> session.lightning!!
            AccountType.UNKNOWN -> throw Exception("Network not found")
        }
    }

    protected fun enableLightningShortcut() {
        applicationScope.launch(context = logException(countly)) {
            _enableLightningShortcut()
        }
    }
}