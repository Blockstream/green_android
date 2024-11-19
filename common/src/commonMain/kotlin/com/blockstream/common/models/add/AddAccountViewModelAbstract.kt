package com.blockstream.common.models.add

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_creating_your_s_account
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.toLoginCredentials
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.richWatchOnly
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString


abstract class AddAccountViewModelAbstract(greenWallet: GreenWallet, val assetId: String?, val popTo: PopTo?) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    internal val _accountTypeBeingCreated: MutableStateFlow<AccountTypeLook?> = MutableStateFlow(null)
    internal var _pendingSideEffect: SideEffect? = null

    open fun assetId() = assetId

    init {
        _accountTypeBeingCreated.filterNotNull().onEach {
            onProgressDescription.value = getString(Res.string.id_creating_your_s_account, it.accountType.toString())
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
                            network = network.id,
                            credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                            encryptedData = encryptedData
                        )

                        database.replaceLoginCredential(loginCredentials)
                    }
                }

                // Save Lightning mnemonic
                if(!greenWallet.isEphemeral) {
                    val encryptedData = withContext(context = Dispatchers.IO) {
                        (mnemonic ?: session.deriveLightningMnemonic()).let {
                            greenKeystore.encryptData(it.encodeToByteArray())
                        }
                    }

                    database.replaceLoginCredential(
                        createLoginCredentials(
                            walletId = greenWallet.id,
                            network = network.id,
                            credentialType = CredentialType.LIGHTNING_MNEMONIC,
                            encryptedData = encryptedData
                        )
                    )
                }


//                if (appInfo.isDevelopmentOrDebug) {
//                    logger.i { "Development/Debug feature setCloseToAddress" }
//                    session.accounts.value.filter { it.isBitcoin }.let { accounts ->
//                        accounts.find { it.type == AccountType.BIP84_SEGWIT }
//                            ?: accounts.find { it.type == AccountType.BIP49_SEGWIT_WRAPPED }
//                    }?.also {
//                        session.lightningSdk.setCloseToAddress(session.getReceiveAddress(it).address)
//                    }
//                }

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
                session.initNetworkIfNeeded(
                    network = network,
                    hardwareWalletResolver = DeviceResolver.createIfNeeded(session.gdkHwWallet, this)
                ) { }

                // Update rich watch only credentials if needed
                database.getLoginCredential(greenWallet.id, CredentialType.RICH_WATCH_ONLY)?.richWatchOnly(greenKeystore)?.also {
                    session.updateRichWatchOnly(it).toLoginCredentials(
                        session = session,
                        greenWallet = greenWallet,
                        greenKeystore = greenKeystore
                    ).also {
                        database.replaceLoginCredential(it)
                    }
                }
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

            val accountAsset = AccountAsset.fromAccountAsset(
                account = it,
                assetId = assetId() ?: it.network.policyAsset,
                session = session
            )

            // or setActiveAccount
            postEvent(Events.SetAccountAsset(accountAsset, setAsActive = true))
            postSideEffect(SideEffects.AccountCreated(accountAsset))

            val navigateToRoot = SideEffects.NavigateToRoot(popTo = popTo)

            if (it.isLightning && !greenWallet.isEphemeral) {
                postSideEffect(SideEffects.LightningShortcut)
                _pendingSideEffect = navigateToRoot
            } else {
                postSideEffect(navigateToRoot)
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
                when {
                    asset.isBitcoin -> session.bitcoinSinglesig!!
                    asset.isLiquidNetwork(session) -> session.liquidSinglesig!!
                    else -> throw Exception("Network not found")
                }
            }
            AccountType.STANDARD -> when {
                asset.isBitcoin -> session.bitcoinMultisig!!
                asset.isLiquidNetwork(session) -> session.liquidMultisig!!
                else -> throw Exception("Network not found")
            }
            AccountType.AMP_ACCOUNT -> session.liquidMultisig!!
            AccountType.TWO_OF_THREE -> session.bitcoinMultisig!!
            AccountType.LIGHTNING -> session.lightning!!
            AccountType.UNKNOWN -> throw Exception("Network not found")
        }
    }

    companion object: Loggable()
}