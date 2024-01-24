package com.blockstream.common.models.add

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Redact
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.views.AccountTypeLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


abstract class ChooseAccountTypeViewModelAbstract(
    greenWallet: GreenWallet
) : AddAccountViewModelAbstract(greenWallet = greenWallet) {
    override fun screenName(): String = "AddAccountChooseType"

    @NativeCoroutinesState
    abstract val asset: MutableStateFlow<EnrichedAsset>

    @NativeCoroutinesState
    abstract val accountTypes: StateFlow<List<AccountTypeLook>>

    @NativeCoroutinesState
    abstract val accountTypeBeingCreated: StateFlow<AccountTypeLook?>

    @NativeCoroutinesState
    abstract val isShowingAdvancedOptions: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val hasAdvancedOptions: MutableStateFlow<Boolean>
}

class ChooseAccountTypeViewModel(greenWallet: GreenWallet, initAsset: EnrichedAsset? = null) :
    ChooseAccountTypeViewModelAbstract(greenWallet = greenWallet) {
    override val asset: MutableStateFlow<EnrichedAsset> =
        MutableStateFlow(initAsset ?: session.ifConnected {
            EnrichedAsset.create(session = session, assetId = BTC_POLICY_ASSET)
        } ?: EnrichedAsset.Emtpy)

    private val _accountTypes: MutableStateFlow<List<AccountTypeLook>> = MutableStateFlow(listOf())
    override val accountTypes: StateFlow<List<AccountTypeLook>> = _accountTypes.asStateFlow()
    override val accountTypeBeingCreated: StateFlow<AccountTypeLook?> =
        _accountTypeBeingCreated.asStateFlow()
    override val isShowingAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val allAccountTypes = MutableStateFlow<List<AccountTypeLook>>(listOf())
    private val defaultAccountTypes = MutableStateFlow<List<AccountTypeLook>>(listOf())

    class LocalEvents {
        data class SetAsset(val asset: EnrichedAsset) : Event
        data class ChooseAccountType(val accountType: AccountType) : Event
        data class CreateAccount(val accountType: AccountType) : Event
        data class CreateLightningAccount(val lightningMnemonic: String) : Event, Redact
    }

    class LocalSideEffects {
        class ArchivedAccountDialog(event: Event) : SideEffects.SideEffectEvent(event) {
            constructor(sideEffect: SideEffect) : this(Events.EventSideEffect(sideEffect))
        }

        class ExperimentalFeaturesDialog(event: Event) : SideEffects.SideEffectEvent(event) {
            constructor(sideEffect: SideEffect) : this(Events.EventSideEffect(sideEffect))
        }
    }

    init {
        bootstrap()

        session.ifConnected {
            asset.onEach { asset ->
                val list = mutableListOf<AccountTypeLook>()

                val isBitcoin = asset.isBitcoin

                if (asset.isAmp) {
                    list += AccountTypeLook(AccountType.AMP_ACCOUNT)
                } else {
                    // Check if singlesig networks are available in this session
                    if ((isBitcoin && session.bitcoinSinglesig != null) || (!isBitcoin && session.liquidSinglesig != null)) {
                        list += listOf(
                            AccountType.BIP84_SEGWIT,
                            AccountType.BIP49_SEGWIT_WRAPPED
                        ).map { AccountTypeLook(it) }
                        if (isBitcoin && session.supportsLightning() && settingsManager.isLightningEnabled() && !session.isTestnet) {
                            list += AccountTypeLook(
                                AccountType.LIGHTNING,
                                canBeAdded = !session.hasLightning
                            )
                        }
                    }

                    // Check if multisig networks are available in this session
                    if ((isBitcoin && session.bitcoinMultisig != null) || (!isBitcoin && session.liquidMultisig != null)) {
                        list += AccountTypeLook(AccountType.STANDARD)

                        list += if (isBitcoin) {
                            AccountTypeLook(AccountType.TWO_OF_THREE)
                        } else {
                            AccountTypeLook(AccountType.AMP_ACCOUNT)
                        }
                    }
                }

                defaultAccountTypes.value = list.filter {
                    it.accountType == AccountType.BIP84_SEGWIT || it.accountType == AccountType.STANDARD || it.accountType == AccountType.LIGHTNING || (it.accountType == AccountType.AMP_ACCOUNT && asset.isAmp)
                }

                allAccountTypes.value = list

                hasAdvancedOptions.value =
                    allAccountTypes.value.size != defaultAccountTypes.value.size
            }.launchIn(viewModelScope.coroutineScope)

            combine(allAccountTypes, isShowingAdvancedOptions) { _, showAdvanced ->
                showAdvanced
            }.onEach {
                _accountTypes.value = if (it) {
                    allAccountTypes.value
                } else {
                    defaultAccountTypes.value
                }
            }.launchIn(viewModelScope.coroutineScope)
        }
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.SetAsset -> {
                asset.value = event.asset
            }

            is LocalEvents.ChooseAccountType -> {
                chooseAccountType(event.accountType)
            }

            is LocalEvents.CreateAccount -> {
                createAccount(
                    accountType = event.accountType,
                    accountName = event.accountType.toString(),
                    network = networkForAccountType(event.accountType, asset.value),
                )
            }

            is LocalEvents.CreateLightningAccount -> {
                createAccount(
                    accountType = AccountType.LIGHTNING,
                    accountName = AccountType.LIGHTNING.toString(),
                    network = networkForAccountType(AccountType.LIGHTNING, EnrichedAsset.Emtpy),
                    mnemonic = event.lightningMnemonic,
                )
            }
        }
    }

    private fun chooseAccountType(accountType: AccountType) {
        val network = networkForAccountType(accountType, asset.value)

        SideEffects.SideEffectEvent(LocalEvents.CreateAccount(accountType))

        var sideEffect: SideEffect? = null
        var event: Event? = null

        if (accountType == AccountType.TWO_OF_THREE) {
            sideEffect = SideEffects.NavigateTo(
                NavigateDestinations.AddAccount2of3(
                    SetupArgs(
                        greenWallet = greenWallet,
                        assetId = asset.value.assetId,
                        network = network,
                        accountType = AccountType.TWO_OF_THREE
                    )
                )
            )
        } else {
            if (accountType.isLightning()) {
                sideEffect = if (session.isHardwareWallet) {
                    LocalSideEffects.ExperimentalFeaturesDialog(
                        SideEffects.NavigateTo(
                            NavigateDestinations.ExportLightningKey
                        )
                    )
                } else {
                    LocalSideEffects.ExperimentalFeaturesDialog(
                        LocalEvents.CreateAccount(
                            accountType
                        )
                    )
                }
            } else {
                event = LocalEvents.CreateAccount(accountType)
            }
        }

        // Check if account is already archived
        if (isAccountAlreadyArchived(network, accountType)) {
            if (event != null) {
                postSideEffect(LocalSideEffects.ArchivedAccountDialog(event))
            }

            if (sideEffect != null) {
                postSideEffect(LocalSideEffects.ArchivedAccountDialog(sideEffect))
            }
        } else {
            if (event != null) {
                postEvent(event)
            } else if (sideEffect != null) {
                postSideEffect(sideEffect)
            }
        }
    }

    private fun isAccountAlreadyArchived(network: Network, accountType: AccountType): Boolean {
        return session.allAccounts.value.find {
            it.hidden && it.network == network && it.type == accountType && (network.isMultisig || it.hasHistory(
                session
            ))
        } != null
    }
}

class ChooseAccountTypeViewModelPreview(greenWallet: GreenWallet) :
    ChooseAccountTypeViewModelAbstract(greenWallet) {

    override val asset: MutableStateFlow<EnrichedAsset> = MutableStateFlow(EnrichedAsset.Emtpy)
    override val accountTypes: StateFlow<List<AccountTypeLook>> = MutableStateFlow(listOf())
    override val accountTypeBeingCreated: StateFlow<AccountTypeLook?> = MutableStateFlow(null)
    override val isShowingAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = ChooseAccountTypeViewModelPreview(
            greenWallet = previewWallet(isHardware = false)
        )
    }
}
