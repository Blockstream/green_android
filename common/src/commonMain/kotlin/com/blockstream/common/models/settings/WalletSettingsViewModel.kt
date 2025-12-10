package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_12_months_51840_blocks
import blockstream_green.common.generated.resources.id_15_months_65535_blocks
import blockstream_green.common.generated.resources.id_2fa_expiry
import blockstream_green.common.generated.resources.id_2fa_methods
import blockstream_green.common.generated.resources.id_2fa_reset_in_progress
import blockstream_green.common.generated.resources.id_2fa_threshold
import blockstream_green.common.generated.resources.id_6_months_25920_blocks
import blockstream_green.common.generated.resources.id_about
import blockstream_green.common.generated.resources.id_account_settings
import blockstream_green.common.generated.resources.id_another_2fa_method_is_already
import blockstream_green.common.generated.resources.id_confirm_via_2fa_that_you
import blockstream_green.common.generated.resources.id_copied_to_clipboard
import blockstream_green.common.generated.resources.id_creating_your_s_account
import blockstream_green.common.generated.resources.id_general
import blockstream_green.common.generated.resources.id_if_you_have_some_coins_on_the
import blockstream_green.common.generated.resources.id_learn_more
import blockstream_green.common.generated.resources.id_recovery_tool
import blockstream_green.common.generated.resources.id_recovery_transactions
import blockstream_green.common.generated.resources.id_request_twofactor_reset
import blockstream_green.common.generated.resources.id_security
import blockstream_green.common.generated.resources.id_security_change
import blockstream_green.common.generated.resources.id_set_twofactor_threshold
import blockstream_green.common.generated.resources.id_settings
import blockstream_green.common.generated.resources.id_wallet_settings
import blockstream_green.common.generated.resources.id_your_2fa_expires_so_that_if_you
import blockstream_green.common.generated.resources.id_your_wallet_is_locked_for_a
import com.blockstream.common.BTC_UNIT
import com.blockstream.common.Urls
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EnrichedAsset
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.Redact
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.data.WalletSetting
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.biometricsMnemonic
import com.blockstream.common.extensions.biometricsPinData
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.indexOfOrNull
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.SettingsNotification
import com.blockstream.common.gdk.data.TwoFactorConfig
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.gdk.params.CsvParams
import com.blockstream.common.gdk.selectTwoFactorMethod
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.usecases.SetBiometricsUseCase
import com.blockstream.common.usecases.SetPinUseCase
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.UserInput
import com.blockstream.common.utils.toAmountLook
import com.blockstream.domain.account.CreateAccountUseCase
import com.blockstream.domain.boltz.IsSwapsEnabledUseCase
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

@Serializable
enum class WalletSettingsSection {
    General, TwoFactor, RecoveryTransactions, ChangePin;
}

abstract class WalletSettingsViewModelAbstract(
    greenWallet: GreenWallet,
    val section: WalletSettingsSection,
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = when (section) {
        WalletSettingsSection.ChangePin -> "WalletSettingsChangePIN"
        WalletSettingsSection.RecoveryTransactions -> "WalletSettingsRecoveryTransactions"
        WalletSettingsSection.TwoFactor -> "WalletSettings2FA"
        else -> "WalletSettingsTab"
    }

    @NativeCoroutinesState
    abstract val items: StateFlow<List<WalletSetting>>
}

class WalletSettingsViewModel(
    greenWallet: GreenWallet,
    section: WalletSettingsSection,
    private val network: Network? = null
) : WalletSettingsViewModelAbstract(greenWallet = greenWallet, section = section) {
    private val createAccountUseCase: CreateAccountUseCase by inject()
    private val setBiometricsUseCase: SetBiometricsUseCase by inject()
    private val setPinUseCase: SetPinUseCase by inject()
    private val isSwapsEnabledUseCase: IsSwapsEnabledUseCase by inject()

    private val _items = MutableStateFlow(listOf<WalletSetting>())
    override val items = _items.asStateFlow()

    private val _hasBiometrics = MutableStateFlow(false)

    internal val _accountTypeBeingCreated: MutableStateFlow<AccountTypeLook?> = MutableStateFlow(null)

    class LocalEvents {
        object DenominationExchangeRate : Events.EventSideEffect(sideEffect = SideEffects.OpenDenominationExchangeRate)
        object WatchOnly : Event
        object ChangePin : Event
        object SetupEmailRecovery : Event
        object RequestRecoveryTransactions : Event
        object RecoveryTransactionEmails : Event
        object LoginWithBiometrics : Event
        object TwoFactorAuthentication : Event
        object PgpKey : Event
        object AutologoutTimeout : Event
        data class SetAutologoutTimeout(val minutes: Int) : Event
        object TwoFactorThreshold : Event
        data class SetTwoFactorThreshold(val value: String) : Event
        data class SetPgp(val key: String?) : Event
        data class SetPin(val pin: String) : Event, Redact
        data class SetCsvTime(val csvTime: Int) : Event
        data class Toggle2FA(val method: TwoFactorMethod) : Event
        data class Enable2FA(val method: TwoFactorMethod) : Event
        data class Disable2FA(val method: TwoFactorMethod, val authenticateMethod: TwoFactorMethod) : Event
        data object RecoveryPhrase : Event
        data object SupportId : Event

        data class CopyAmpId(val account: Account? = null) : Event
        data class ChooseAccountType(val accountType: AccountType) : Event
        data object DisableLightning : Event
        data class CreateAccount(val accountType: AccountType, val asset: EnrichedAsset? = null) : Event
        data class CreateLightningAccount(val lightningMnemonic: String) : Event, Redact
        object CreateNewAccount : Event
    }

    class LocalSideEffects {
        data class OpenAutoLogoutTimeout(val minutes: Int) : SideEffect
        data class OpenPgpKey(val pgp: String) : SideEffect
        data class OpenTwoFactorThershold(val threshold: String) : SideEffect
        data class Disable2FA(
            val title: String,
            val message: String,
            val method: TwoFactorMethod,
            val availableMethods: List<TwoFactorMethod>,
            val network: Network
        ) : SideEffect

        object LaunchBiometrics : SideEffect
        data class CopyAmpId(val accounts: List<Account>) : SideEffect

        class ArchivedAccountDialog(event: Event) : SideEffects.SideEffectEvent(event) {
            constructor(sideEffect: SideEffect) : this(Events.EventSideEffect(sideEffect))
        }

        class ExperimentalFeaturesDialog(event: Event) : SideEffects.SideEffectEvent(event) {
            constructor(sideEffect: SideEffect) : this(Events.EventSideEffect(sideEffect))
        }
    }

    init {
        combine(greenWalletFlow.filterNotNull(), _accountTypeBeingCreated) { greenWallet, accountTypeBeingCreated ->
            updateNavData(greenWallet, accountTypeBeingCreated == null)
        }.launchIn(this)

        viewModelScope.launch {
            updateNavData(greenWallet, true)
        }

        _accountTypeBeingCreated.filterNotNull().onEach {
            onProgressDescription.value = getString(Res.string.id_creating_your_s_account, it.accountType.toString())
        }.launchIn(this)

        session.ifConnected {
            database.getLoginCredentialsFlow(greenWallet.id).onEach {
                _hasBiometrics.value = it.biometricsPinData != null || it.biometricsMnemonic != null
            }.launchIn(viewModelScope.coroutineScope)

            combine(
                session.settings(network = network ?: session.defaultNetwork),
                network?.takeIf { it.isMultisig }?.let { session.twoFactorConfig(network) } ?: flowOf(null),
                walletSettingsManager.getWalletSettings(walletId = greenWallet.id), // Update on wallet settings changes
                session.allAccounts,
                _hasBiometrics,
            ) { settings, twoFactorConfig, _, _, _ ->
                _items.value = withContext(Dispatchers.IO) {
                    build(settings, twoFactorConfig)
                }
            }.launchIn(this)
        }

        bootstrap()
    }

    private suspend fun updateNavData(greenWallet: GreenWallet, isVisible: Boolean) {
        when (section) {
            WalletSettingsSection.RecoveryTransactions -> Res.string.id_recovery_transactions
            WalletSettingsSection.TwoFactor -> Res.string.id_2fa_methods
            else -> Res.string.id_settings
        }.also {
            _navData.value = NavData(
                title = getString(it),
                isVisible = isVisible,
                // subtitle = greenWallet.name.takeIf { section != WalletSettingsSection.General },
                walletName = greenWallet.name.takeIf { section == WalletSettingsSection.General },
                showBadge = !greenWallet.isRecoveryConfirmed && section == WalletSettingsSection.General,
                showBottomNavigation = section == WalletSettingsSection.General
            )
        }
    }

    private suspend fun build(settings: Settings?, twoFactorConfig: TwoFactorConfig?): List<WalletSetting> {
        val list = mutableListOf<WalletSetting>()

        if (section == WalletSettingsSection.TwoFactor && twoFactorConfig != null) {

            if (session.walletExistsAndIsUnlocked(network)) {
                list += listOf(WalletSetting.Text(getString(Res.string.id_2fa_methods)))

                list += twoFactorConfig.allMethods.map {
                    TwoFactorMethod.from(it)
                }.map { method ->
                    val config = twoFactorConfig.twoFactorMethodConfig(method)
                    WalletSetting.TwoFactorMethod(
                        method = method,
                        data = config.data,
                        enabled = config.enabled
                    )
                }

                network?.also {
                    list += listOf(
                        WalletSetting.Text(
                            title = getString(Res.string.id_request_twofactor_reset)
                        ),
                        WalletSetting.LostTwoFactor(network = it)
                    )
                }

                if (network?.isBitcoin == true) {
                    list += listOf(
                        WalletSetting.Text(
                            title = getString(Res.string.id_2fa_threshold)
                        ),

                        thresholdFromConfig(twoFactorConfig = twoFactorConfig, isForUserEdit = false).let { threshold ->
                            WalletSetting.TwoFactorThreshold(
                                subtitle = threshold
                            )
                        }
                    )
                }

                val showBuckets = (network?.csvBuckets?.size ?: 0) > 1

                list += listOf(
                    WalletSetting.Text(
                        title = getString(Res.string.id_2fa_expiry)
                    )
                )

                if (showBuckets) {
                    list += listOf(
                        Res.string.id_6_months_25920_blocks,
                        Res.string.id_12_months_51840_blocks,
                        Res.string.id_15_months_65535_blocks,
                    ).mapIndexed { index, titleRes ->
                        WalletSetting.TwoFactorBucket(
                            title = getString(titleRes),
                            subtitle = "",
                            enabled = index == network?.csvBuckets?.indexOfOrNull(settings?.csvTime),
                            bucket = network?.csvBuckets?.getOrNull(index) ?: 0
                        )
                    }
                }

                list += listOf(
                    WalletSetting.InfoAlert(
                        message = getString(Res.string.id_your_2fa_expires_so_that_if_you)
                    ),
                    WalletSetting.ButtonEvent(getString(Res.string.id_recovery_tool), Events.OpenBrowser(Urls.RECOVERY_TOOL), isPrimary = true)
                )

                if (network?.isBitcoin == true) {
                    list += listOf(WalletSetting.RequestRecovery(network))
                }

            } else {
                network?.let { network ->

                    session.getTwoFactorReset(network)?.also {
                        list += listOf(
                            WalletSetting.Text(
                                title = getString(Res.string.id_2fa_reset_in_progress),
                                message = getString(
                                    Res.string.id_your_wallet_is_locked_for_a,
                                    it.daysRemaining
                                )
                            ),
                            WalletSetting.ButtonEvent(
                                title = getString(Res.string.id_learn_more),
                                event = NavigateDestinations.TwoFactorReset(
                                    greenWallet = greenWallet,
                                    network = network,
                                    twoFactorReset = sessionOrNull?.twoFactorReset(network)?.value
                                )
                            )
                        )
                    }
                }
            }

        } else if (section == WalletSettingsSection.RecoveryTransactions && twoFactorConfig != null) {

            list += listOfNotNull(
                WalletSetting.Text(message = getString(Res.string.id_if_you_have_some_coins_on_the)),
                WalletSetting.LearnMore(event = Events.OpenBrowser(url = Urls.HELP_NLOCKTIMES)),
            )

            list += if (twoFactorConfig.email.confirmed) {
                listOf(
                    WalletSetting.RecoveryTransactionEmails(enabled = settings?.notifications?.emailIncoming == true),
                    WalletSetting.RequestRecoveryTransactions,
                )
            } else {
                listOf(WalletSetting.SetupEmailRecovery)
            }

        } else if (section == WalletSettingsSection.General) {

            if (settings != null) {

                // Wallet Settings Section
                list += WalletSetting.Text(getString(Res.string.id_wallet_settings))
                if (!greenWallet.isEphemeral) {
                    list += WalletSetting.RenameWallet(walletName = greenWallet.name)
                }
                list += listOf(
                    WalletSetting.DenominationExchangeRate(
                        unit = settings.networkUnit(session),
                        currency = settings.pricing.currency,
                        exchange = settings.pricing.exchange
                    ),
                    WalletSetting.AutoLogoutTimeout(settings.altimeout),
                    WalletSetting.Logout
                )

                if (!session.isWatchOnlyValue) {
                    // Account Settings Section
                    list += WalletSetting.Text(getString(Res.string.id_account_settings))
                    list += listOfNotNull(
                        WalletSetting.Lightning(enabled = session.hasLightning)
                            .takeIf { settingsManager.appSettings.experimentalFeatures || session.hasLightning },
                        WalletSetting.CreateAmpAccount.takeIf { session.accounts.value.find { it.type == AccountType.AMP_ACCOUNT } == null },
                        WalletSetting.CopyAmpId.takeIf { session.accounts.value.any { it.type == AccountType.AMP_ACCOUNT } },
                    )

                    val hasMultisig = session.activeBitcoinMultisig != null || session.activeLiquidMultisig != null

                    if (hasMultisig) {
                        list += listOf(WalletSetting.TwoFactorAuthentication)
                        session.activeMultisig.firstOrNull()?.also {
                            list += listOf(WalletSetting.PgpKey(enabled = session.getSettings(it)?.pgp.isNotBlank()))
                        }
                    }

                    list += listOf(
                        WalletSetting.ArchivedAccounts,
                        WalletSetting.CreateNewAccount
                    )
                }
            }

            // About Section
            list += WalletSetting.Text(getString(Res.string.id_about))
            list += listOf(
                WalletSetting.Version(appInfo.versionFlavorDebug),
                WalletSetting.SupportId,
                WalletSetting.GetSupport
            )
        }

        return list
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.CopyAmpId -> {
                if (event.account == null) {
                    postSideEffect(LocalSideEffects.CopyAmpId(session.accounts.value.filter { it.type == AccountType.AMP_ACCOUNT }))
                } else {
                    postSideEffect(SideEffects.CopyToClipboard(event.account.receivingId))
                }
            }

            is LocalEvents.ChooseAccountType -> {
                chooseAccountType(event.accountType)
            }

            is LocalEvents.DisableLightning -> {
                doAsync({
                    if (session.hasLightning) {
                        session.lightningAccount.also {
                            removeAccount(it)
                        }
                    }
                })
            }

            is LocalEvents.CreateAccount -> {
                createAccount(
                    accountType = event.accountType,
                    accountName = event.accountType.toString(),
                    network = networkForAccountType(event.accountType, event.asset),
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

            is LocalEvents.WatchOnly -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnly(greenWallet = greenWallet)))
            }

            is LocalEvents.CreateNewAccount -> {
                postSideEffect(SideEffects.NavigateTo(
                    NavigateDestinations.ChooseAccountType(greenWallet = greenWallet)
                ))
            }

            is LocalEvents.SetupEmailRecovery -> {
                network?.also {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.TwoFactorSetup(
                                greenWallet = greenWallet,
                                method = TwoFactorMethod.EMAIL,
                                action = TwoFactorSetupAction.SETUP_EMAIL,
                                network = it
                            )
                        )
                    )
                }
            }

            is LocalEvents.RequestRecoveryTransactions -> {
                sendNlocktimes()
            }

            is LocalEvents.RecoveryTransactionEmails -> {
                toggleRecoveryTransactionsEmails()
            }

            is LocalEvents.ChangePin -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.ChangePin(greenWallet = greenWallet)))
            }

            is Events.ProvideCipher -> {
                event.platformCipher?.also {
                    enableBiometrics(it)
                }
            }

            is LocalEvents.LoginWithBiometrics -> {

                if (_hasBiometrics.value) {
                    // Remove Biometrics
                    applicationScope.launch(context = logException(countly)) {
                        database.deleteLoginCredentials(
                            greenWallet.id,
                            CredentialType.BIOMETRICS_PINDATA
                        )
                        database.deleteLoginCredentials(
                            greenWallet.id,
                            CredentialType.BIOMETRICS_MNEMONIC
                        )
                    }
                } else if (greenKeystore.canUseBiometrics()) {
                    postSideEffect(LocalSideEffects.LaunchBiometrics)
                }
            }

            is LocalEvents.TwoFactorAuthentication -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.TwoFactorAuthentication(greenWallet = greenWallet)
                    )
                )
            }

            is LocalEvents.AutologoutTimeout -> {
                postSideEffect(
                    LocalSideEffects.OpenAutoLogoutTimeout(
                        session.getSettings()?.altimeout ?: 1
                    )
                )
            }

            is LocalEvents.TwoFactorThreshold -> {
                viewModelScope.launch {
                    network?.also { network ->
                        session.twoFactorConfig(network).value?.let {
                            postSideEffect(
                                LocalSideEffects.OpenTwoFactorThershold(
                                    thresholdFromConfig(twoFactorConfig = it, isForUserEdit = true)
                                )
                            )
                        }
                    }
                }
            }

            is LocalEvents.PgpKey -> {
                session.activeMultisig.firstOrNull()?.let { session.getSettings(it) }?.also {
                    postSideEffect(LocalSideEffects.OpenPgpKey(it.pgp ?: ""))
                }
            }

            is LocalEvents.SetAutologoutTimeout -> {
                session.getSettings()?.also {
                    saveGlobalSettings(it.copy(altimeout = event.minutes))
                }
            }

            is LocalEvents.SetTwoFactorThreshold -> {
                setLimits(event.value)
            }

            is LocalEvents.SetPgp -> {
                savePgp(event.key)
            }

            is LocalEvents.SetPin -> {
                setPin(event.pin)
            }

            is LocalEvents.SetCsvTime -> {
                setCsvTime(event.csvTime)
            }

            is LocalEvents.Disable2FA -> {
                disable2FA(event.method, event.authenticateMethod)
            }

            is LocalEvents.RecoveryPhrase -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoveryIntro(
                            setupArgs = SetupArgs(
                                greenWallet = greenWallet,
                                isShowRecovery = true
                            )
                        )
                    )
                )
            }

            is LocalEvents.SupportId -> {
                postSideEffect(SideEffects.CopyToClipboard(value = session.supportId()))
                postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_copied_to_clipboard)))
            }

            is LocalEvents.Toggle2FA -> {
                network?.let { session.twoFactorConfig(it) }?.value?.also { twoFactorConfig ->

                    if (twoFactorConfig.twoFactorMethodConfig(event.method).enabled) {

                        viewModelScope.launch {

                            val methods = twoFactorConfig
                                .enabledMethods.map {
                                    TwoFactorMethod.from(it)
                                }.let { methods ->
                                    if (methods.size == 1) {
                                        methods
                                    } else {
                                        methods.filter { it != event.method }
                                    }
                                }

                            postSideEffect(
                                LocalSideEffects.Disable2FA(
                                    title = getString(Res.string.id_security_change),
                                    message = getString(if (twoFactorConfig.enabledMethods.size == 1) Res.string.id_confirm_via_2fa_that_you else Res.string.id_another_2fa_method_is_already),
                                    method = event.method,
                                    availableMethods = methods,
                                    network = network
                                )
                            )
                        }

                    } else {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.TwoFactorSetup(
                                    greenWallet = greenWallet,
                                    method = event.method,
                                    action = TwoFactorSetupAction.SETUP,
                                    network = network,
                                    isSmsBackup = false
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createAccount(
        accountType: AccountType,
        accountName: String,
        network: Network,
        mnemonic: String? = null,
        xpub: String? = null
    ) {
        doAsync({
            createAccountUseCase(
                session = session,
                wallet = greenWallet,
                accountType = accountType,
                accountName = accountName,
                network = network,
                mnemonic = mnemonic,
                xpub = xpub,
                hwInteraction = this
            )
        }, preAction = {
            onProgress.value = true
            _accountTypeBeingCreated.value = AccountTypeLook(accountType)
        }, postAction = {
            onProgress.value = false
            _accountTypeBeingCreated.value = null
        }, onSuccess = {

        })
    }

    private fun chooseAccountType(accountType: AccountType, asset: EnrichedAsset? = null) = tryCatchNull {
        val network = networkForAccountType(accountType, asset)

        var sideEffect: SideEffect? = null
        var event: Event? = null

        if (accountType == AccountType.TWO_OF_THREE) {
            sideEffect = SideEffects.NavigateTo(
                NavigateDestinations.AddAccount2of3(
                    SetupArgs(
                        greenWallet = greenWallet,
                        assetId = asset?.assetId,
                        network = network,
                        accountType = AccountType.TWO_OF_THREE,
                        popTo = null
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

    private fun networkForAccountType(accountType: AccountType, asset: EnrichedAsset?): Network {
        return when (accountType) {
            AccountType.BIP44_LEGACY,
            AccountType.BIP49_SEGWIT_WRAPPED,
            AccountType.BIP84_SEGWIT,
            AccountType.BIP86_TAPROOT -> {
                when {
                    asset == null || asset.isBitcoin -> session.bitcoinSinglesig!!
                    asset.isLiquidNetwork(session) -> session.liquidSinglesig!!
                    else -> throw Exception("Network not found")
                }
            }

            AccountType.STANDARD -> when {
                asset == null || asset.isBitcoin -> session.bitcoinMultisig!!
                asset.isLiquidNetwork(session) -> session.liquidMultisig!!
                else -> throw Exception("Network not found")
            }

            AccountType.AMP_ACCOUNT -> session.liquidMultisig!!
            AccountType.TWO_OF_THREE -> session.bitcoinMultisig!!
            AccountType.LIGHTNING -> session.lightning!!
            AccountType.UNKNOWN -> throw Exception("Network not found")
        }
    }

    private fun isAccountAlreadyArchived(network: Network, accountType: AccountType): Boolean {
        return session.allAccounts.value.find {
            it.hidden && it.network == network && it.type == accountType && (network.isMultisig || it.hasHistory(session))
        } != null
    }

    private fun saveGlobalSettings(newSettings: Settings) {
        doAsync({
            session.changeGlobalSettings(newSettings)
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    private fun savePgp(pgp: String?) {
        doAsync({
            pgp?.trim().also { pgpTrimmed ->
                session.activeMultisig.forEach {
                    session.getSettings(it)?.also { settings ->
                        session.changeSettings(it, settings.copy(pgp = pgpTrimmed))
                        session.updateSettings(it)
                    }
                }
                // Update global settings
                session.updateSettings()
            }
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    private suspend fun toggleRecoveryTransactionsEmails() {
        network?.also {
            session.getSettings(it)?.also { settings ->
                settings.notifications?.let { notifications ->
                    val toggled = !notifications.emailIncoming
                    session.changeSettings(
                        network = it, settings = settings.copy(
                            notifications = SettingsNotification(
                                emailIncoming = toggled,
                                emailOutgoing = toggled
                            )
                        )
                    )
                    session.updateSettings(it)
                }
            }
        }
    }

    private fun sendNlocktimes() {
        network?.also {
            doAsync({
                session.sendNlocktimes(it)
            }, onSuccess = {
                // Message
            })
        }
    }

    private fun enableBiometrics(cipher: PlatformCipher) {
        doAsync({
            setBiometricsUseCase.invoke(session = session, cipher = cipher, wallet = greenWallet)
        }, onSuccess = {

        })
    }

    private fun setLimits(value: String) {
        if (network == null) return

        doAsync({
            val input = UserInput.parseUserInputSafe(
                session = session,
                assetId = network.policyAsset,
                input = value.trim().takeIf { it.isNotBlank() } ?: "0",
            )

            session.twoFactorChangeLimits(network, input.toLimit(), this)
        }, onSuccess = {

        })
    }

    private fun disable2FA(
        method: TwoFactorMethod,
        authenticateMethod: TwoFactorMethod
    ) {
        if (network == null) return
        doAsync({
            session
                .changeSettingsTwoFactor(
                    network = network,
                    method = method.gdkType,
                    methodConfig = TwoFactorMethodConfig(enabled = false),
                    twoFactorResolver = this.selectTwoFactorMethod(authenticateMethod.gdkType)
                )
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    private fun setCsvTime(csvTime: Int) {
        if (network == null) return

        doAsync({
            session.setCsvTime(network = network, value = CsvParams(csvTime), twoFactorResolver = this)
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    private fun setPin(pin: String) {
        doAsync({
            setPinUseCase(
                session = session,
                pin = pin,
                wallet = greenWallet
            )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    private suspend fun thresholdFromConfig(
        twoFactorConfig: TwoFactorConfig,
        isForUserEdit: Boolean
    ): String {
        return twoFactorConfig.limits.let { limits ->
            if (limits.satoshi == 0L && !limits.isFiat) {
                if (isForUserEdit) "" else getString(Res.string.id_set_twofactor_threshold)
            } else if (limits.isFiat) {
                // GDK 0.0.58.post1 - GA_get_twofactor_config: Fiat pricing limits no longer return corresponding
                // converted BTC amounts. When "is_fiat" is true, the caller should convert
                // the amount themselves using GA_convert_amount if desired.
                // Do not allow users to set limits in fiat currency
                if (isForUserEdit) {
                    ""
                } else {
                    "${limits.fiat} ${limits.fiatCurrency}"
                }
            } else {
                twoFactorConfig.limits.toAmountLook(
                    session = session,
                    withUnit = !isForUserEdit
                ) ?: ""
            }
        }
    }
}

class WalletSettingsViewModelPreview(
    greenWallet: GreenWallet,
    section: WalletSettingsSection = WalletSettingsSection.General
) :
    WalletSettingsViewModelAbstract(greenWallet = greenWallet, section = section) {

    override val items: StateFlow<List<WalletSetting>> = MutableStateFlow(runBlocking {
        if (section == WalletSettingsSection.RecoveryTransactions) {
            listOf(
                WalletSetting.Text(message = getString(Res.string.id_if_you_have_some_coins_on_the)),
                WalletSetting.LearnMore(event = Events.OpenBrowser(url = Urls.HELP_NLOCKTIMES)),
                WalletSetting.RecoveryTransactionEmails(enabled = true),
                WalletSetting.RequestRecoveryTransactions,
                WalletSetting.SetupEmailRecovery
            )
        } else if (section == WalletSettingsSection.TwoFactor) {
            listOf(
                WalletSetting.Text(getString(Res.string.id_2fa_methods)),
            )
        } else {
            listOf(
                WalletSetting.Logout,
                WalletSetting.Text(getString(Res.string.id_general)),
                WalletSetting.DenominationExchangeRate(
                    unit = BTC_UNIT,
                    currency = "USD",
                    exchange = "BITFINEX"
                ),
                WalletSetting.WatchOnly,
                WalletSetting.Text(getString(Res.string.id_security)),
                WalletSetting.TwoFactorAuthentication,
                WalletSetting.PgpKey(enabled = false),
                WalletSetting.AutoLogoutTimeout(5),
                WalletSetting.Text(getString(Res.string.id_about)),
                WalletSetting.SupportId,
                WalletSetting.Version("1.0.0"),
            )
        }
    }
    )

    companion object {
        fun preview() = WalletSettingsViewModelPreview(previewWallet(isHardware = false))
        fun previewTwoFactor() = WalletSettingsViewModelPreview(
            previewWallet(isHardware = false),
            section = WalletSettingsSection.TwoFactor
        )

        fun previewRecovery() = WalletSettingsViewModelPreview(
            previewWallet(isHardware = false),
            section = WalletSettingsSection.RecoveryTransactions
        )
    }
}
