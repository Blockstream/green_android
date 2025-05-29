package com.blockstream.common.models.add

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_new_account
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Redact
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.AssetBalance
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.navigation.PopTo
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

abstract class ChooseAccountTypeViewModelAbstract(
    greenWallet: GreenWallet, assetId: String?, popTo: PopTo?, val allowAssetSelection: Boolean = true,
) : AddAccountViewModelAbstract(greenWallet = greenWallet, assetId = assetId, popTo = popTo) {
    override fun screenName(): String = "AddAccountChooseType"

    @NativeCoroutinesState
    abstract val asset: MutableStateFlow<AssetBalance>

    @NativeCoroutinesState
    abstract val accountTypes: StateFlow<List<AccountTypeLook>>

    @NativeCoroutinesState
    abstract val accountTypeBeingCreated: StateFlow<AccountTypeLook?>

    @NativeCoroutinesState
    abstract val isShowingAdvancedOptions: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val hasAdvancedOptions: MutableStateFlow<Boolean>
}

class ChooseAccountTypeViewModel(greenWallet: GreenWallet, initAsset: AssetBalance?, popTo: PopTo?, allowAssetSelection: Boolean) :
    ChooseAccountTypeViewModelAbstract(
        greenWallet = greenWallet,
        assetId = initAsset?.assetId,
        popTo = popTo,
        allowAssetSelection = allowAssetSelection
    ) {
    override val asset: MutableStateFlow<AssetBalance> =
        MutableStateFlow(initAsset ?: EnrichedAsset.Empty.let { AssetBalance.create(it) })

    private val _accountTypes: MutableStateFlow<List<AccountTypeLook>> = MutableStateFlow(listOf())
    override val accountTypes: StateFlow<List<AccountTypeLook>> = _accountTypes.asStateFlow()
    override val accountTypeBeingCreated: StateFlow<AccountTypeLook?> =
        _accountTypeBeingCreated.asStateFlow()
    override val isShowingAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val allAccountTypes = MutableStateFlow<List<AccountTypeLook>>(listOf())
    private val defaultAccountTypes = MutableStateFlow<List<AccountTypeLook>>(listOf())

    class LocalEvents {
        data class SetAsset(val asset: AssetBalance) : Event
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

    override fun assetId(): String = asset.value.assetId

    init {
        bootstrap()

        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_add_new_account))
        }

        session.ifConnected {

            viewModelScope.launch {
                asset.value = initAsset ?: AssetBalance.create(assetId = BTC_POLICY_ASSET, session = session)
            }

            asset.onEach { asset ->
                val list = mutableListOf<AccountTypeLook>()

                val isBitcoin = asset.asset.isBitcoin

                if (asset.asset.isAmp) {
                    list += AccountTypeLook(AccountType.AMP_ACCOUNT)
                } else if (asset.asset.isLightning) {
                    if (session.supportsLightning() && settingsManager.isLightningEnabled() && !session.isTestnet) {
                        list += AccountTypeLook(
                            AccountType.LIGHTNING,
                            canBeAdded = !session.hasLightning
                        )
                    }
                } else {
                    // Check if singlesig networks are available in this session
                    if ((isBitcoin && session.bitcoinSinglesig != null) || (!isBitcoin && session.liquidSinglesig != null)) {
                        list += listOf(
                            AccountType.BIP84_SEGWIT,
                            AccountType.BIP49_SEGWIT_WRAPPED
                        ).map { AccountTypeLook(it) }
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
                    it.accountType == AccountType.BIP84_SEGWIT || it.accountType == AccountType.STANDARD || it.accountType == AccountType.LIGHTNING || (it.accountType == AccountType.AMP_ACCOUNT && asset.asset.isAmp)
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

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(viewModelScope.coroutineScope)
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is Events.Continue -> {
                _pendingSideEffect?.also {
                    postSideEffect(it)
                }
            }

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
                    network = networkForAccountType(event.accountType, asset.value.asset),
                )
            }

            is LocalEvents.CreateLightningAccount -> {
                createAccount(
                    accountType = AccountType.LIGHTNING,
                    accountName = AccountType.LIGHTNING.toString(),
                    network = networkForAccountType(AccountType.LIGHTNING, EnrichedAsset.Empty),
                    mnemonic = event.lightningMnemonic,
                )
            }
        }
    }

    private fun chooseAccountType(accountType: AccountType) = tryCatchNull {
        val network = networkForAccountType(accountType, asset.value.asset)

        var sideEffect: SideEffect? = null
        var event: Event? = null

        if (accountType == AccountType.TWO_OF_THREE) {
            sideEffect = SideEffects.NavigateTo(
                NavigateDestinations.AddAccount2of3(
                    SetupArgs(
                        greenWallet = greenWallet,
                        assetId = asset.value.assetId,
                        network = network,
                        accountType = AccountType.TWO_OF_THREE,
                        popTo = popTo
                    )
                )
            )
        } else {
            if (accountType.isLightning()) {
                sideEffect = if (session.isHardwareWallet) {
                    LocalSideEffects.ExperimentalFeaturesDialog(
                        SideEffects.NavigateTo(
                            NavigateDestinations.JadeQR(
                                greenWalletOrNull = greenWalletOrNull,
                                operation = JadeQrOperation.LightningMnemonicExport,
                                deviceModel = DeviceModel.BlockstreamGeneric
                            )
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
            it.hidden && it.network == network && it.type == accountType && (network.isMultisig || it.hasHistory(session))
        } != null
    }
}

class ChooseAccountTypeViewModelPreview(greenWallet: GreenWallet) :
    ChooseAccountTypeViewModelAbstract(greenWallet, assetId = null, popTo = null) {

    override val asset: MutableStateFlow<AssetBalance> = MutableStateFlow(EnrichedAsset.Empty.let { AssetBalance.create(it) })
    override val accountTypes: StateFlow<List<AccountTypeLook>> = MutableStateFlow(
        listOf(
            AccountTypeLook(AccountType.BIP49_SEGWIT_WRAPPED, true),
            AccountTypeLook(AccountType.BIP84_SEGWIT, true),
            AccountTypeLook(AccountType.BIP86_TAPROOT, true),
            AccountTypeLook(AccountType.LIGHTNING, true),
            AccountTypeLook(AccountType.LIGHTNING, false),
            AccountTypeLook(AccountType.STANDARD, true),
            AccountTypeLook(AccountType.AMP_ACCOUNT, true)
        )
    )
    override val accountTypeBeingCreated: StateFlow<AccountTypeLook?> = MutableStateFlow(null)
    override val isShowingAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasAdvancedOptions: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = ChooseAccountTypeViewModelPreview(
            greenWallet = previewWallet(isHardware = false)
        )
    }
}
