package com.blockstream.compose.models.add

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_add_new_account
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.looks.AccountTypeLook
import com.blockstream.compose.models.jade.JadeQrOperation
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.PopTo
import com.blockstream.data.data.Redact
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.data.gdk.data.Network
import com.blockstream.domain.account.HasHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class ChooseAccountTypeViewModelAbstract(
    greenWallet: GreenWallet, assetId: String?, popTo: PopTo?, val allowAssetSelection: Boolean = true,
) : AddAccountViewModelAbstract(greenWallet = greenWallet, assetId = assetId, popTo = popTo) {
    override fun screenName(): String = "AddAccountChooseType"
    abstract val asset: MutableStateFlow<AssetBalance>
    abstract val accountTypes: StateFlow<List<AccountTypeLook>>
    abstract val accountTypeBeingCreated: StateFlow<AccountTypeLook?>
    abstract val isShowingAdvancedOptions: MutableStateFlow<Boolean>
    abstract val hasAdvancedOptions: MutableStateFlow<Boolean>
}

class ChooseAccountTypeViewModel(greenWallet: GreenWallet, initAsset: AssetBalance?, popTo: PopTo?, allowAssetSelection: Boolean) :
    ChooseAccountTypeViewModelAbstract(
        greenWallet = greenWallet,
        assetId = initAsset?.assetId,
        popTo = popTo,
        allowAssetSelection = allowAssetSelection
    ) {
    private val hasHistoryUseCase: HasHistoryUseCase by inject()

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
        data class ChooseAccountType(val accountType: AccountType, val actionConfirmed: Boolean = false) : Event
        data class CreateAccount(val accountType: AccountType) : Event
        data class CreateLightningAccount(val lightningMnemonic: String) : Event, Redact
    }

    class LocalSideEffects {
        class ArchivedAccountDialog(event: Event) : SideEffects.SideEffectEvent(event) {
            constructor(sideEffect: SideEffect) : this(Events.EventSideEffect(sideEffect))
        }

        class JadeWoDisableDialog(event: Event) : SideEffects.SideEffectEvent(event) {
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
                val isLiquid = asset.asset.isLiquidNetwork(session)

                if (asset.asset.isAmp && canCreateAmp2Account()) {
                    list += AccountTypeLook(
                        AccountType.AMP2_ACCOUNT,
                        canBeAdded = !session.hasAmpAccount
                    )
                } else if (asset.asset.isAmpLegacy) {
                    list += AccountTypeLook(
                        AccountType.AMP_LEGACY_ACCOUNT,
                        canBeAdded = !session.hasAmpLegacyAccount
                    )
                } else if (asset.asset.isLightning) {
                    if (session.supportsLightning() && settingsManager.isLightningAvailable() && !session.isTestnet) {
                        list += AccountTypeLook(
                            AccountType.LIGHTNING, canBeAdded = !session.hasLightning
                        )
                    }
                } else {
                    // Check if singlesig networks are available in this session
                    val hasSinglesig = (isBitcoin && session.bitcoinSinglesig != null) || (!isBitcoin && session.liquidSinglesig != null)
                    if (hasSinglesig) {
                        list += AccountTypeLook(AccountType.BIP84_SEGWIT)
                    }

                    // Check if multisig networks are available in this session
                    if (isBitcoin && session.bitcoinMultisig != null) {
                        list += AccountTypeLook(AccountType.TWO_OF_THREE)
                    }

                    if ((isBitcoin && session.bitcoinMultisig != null) || (isLiquid && session.liquidMultisig != null)) {
                        list += AccountTypeLook(AccountType.STANDARD)
                    }

                    if (hasSinglesig) {
                        list += AccountTypeLook(AccountType.BIP49_SEGWIT_WRAPPED)
                    }
                }

                defaultAccountTypes.value = list.filter {
                    it.accountType == AccountType.BIP84_SEGWIT || it.accountType == AccountType.LIGHTNING || it.accountType == AccountType.STANDARD || it.accountType == AccountType.TWO_OF_THREE || it.accountType == AccountType.AMP2_ACCOUNT || (it.accountType == AccountType.AMP_LEGACY_ACCOUNT && asset.asset.isAmpLegacy)
                }

                allAccountTypes.value = list

                hasAdvancedOptions.value =
                    allAccountTypes.value.size != defaultAccountTypes.value.size
            }.launchIn(viewModelScope)

            combine(allAccountTypes, isShowingAdvancedOptions) { _, showAdvanced ->
                showAdvanced
            }.onEach {
                _accountTypes.value = if (it) {
                    allAccountTypes.value
                } else {
                    defaultAccountTypes.value
                }
            }.launchIn(viewModelScope)
        }

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(viewModelScope)
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
                chooseAccountType(accountType = event.accountType, actionConfirmed = event.actionConfirmed)
            }

            is LocalEvents.CreateAccount -> {
                createAccount(
                    accountType = event.accountType,
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

    private suspend fun chooseAccountType(accountType: AccountType, actionConfirmed: Boolean = false) = tryCatch {

        if (accountType.isMutlisig() && !actionConfirmed && database.getLoginCredential(
                id = greenWallet.id,
                credentialType = CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
            ) != null
        ) {
            // Show Jade WO Disable dialog
            postSideEffect(
                LocalSideEffects.JadeWoDisableDialog(
                    LocalEvents.ChooseAccountType(
                        accountType = accountType,
                        actionConfirmed = true
                    )
                )
            )
        } else {

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
                        SideEffects.NavigateTo(
                            NavigateDestinations.JadeQR(
                                greenWalletOrNull = greenWalletOrNull,
                                operation = JadeQrOperation.LightningMnemonicExport,
                                deviceModel = DeviceModel.BlockstreamGeneric
                            )
                        )
                    } else {
                        SideEffects.NavigateTo(
                            NavigateDestinations.LightningOnboarding(greenWallet = greenWallet)
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
    }

    private suspend fun isAccountAlreadyArchived(network: Network, accountType: AccountType): Boolean {
        return session.allAccounts.value.find {
            it.hidden && it.network == network && it.type == accountType && (network.isMultisig || hasHistoryUseCase.invoke(
                session = session,
                wallet = greenWallet,
                account = it
            ))
        } != null
    }

    private fun canCreateAmp2Account(): Boolean =
        session.isTestnet && session.liquidAmp2 != null
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
            AccountTypeLook(AccountType.AMP_LEGACY_ACCOUNT, true)
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
