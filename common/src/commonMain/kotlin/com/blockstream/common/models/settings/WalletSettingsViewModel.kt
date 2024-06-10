package com.blockstream.common.models.settings

import com.blockstream.common.BTC_UNIT
import com.blockstream.common.Urls
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.data.Redact
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.data.WalletExtras
import com.blockstream.common.data.WalletSetting
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.biometricsPinData
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.Settings
import com.blockstream.common.gdk.data.SettingsNotification
import com.blockstream.common.gdk.data.TwoFactorMethodConfig
import com.blockstream.common.gdk.params.CsvParams
import com.blockstream.common.gdk.params.EncryptWithPinParams
import com.blockstream.common.gdk.params.Limits
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.randomChars
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WalletSettingsSection {
    General, TwoFactor, RecoveryTransactions, ChangePin;
}

abstract class WalletSettingsViewModelAbstract(
    greenWallet: GreenWallet,
    private val section: WalletSettingsSection,
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = when(section) {
        WalletSettingsSection.ChangePin -> "WalletSettingsChangePIN"
        WalletSettingsSection.RecoveryTransactions -> "WalletSettingsRecoveryTransactions"
        WalletSettingsSection.TwoFactor -> "WalletSettings2FA"
        else -> "WalletSettings"
    }

    @NativeCoroutinesState
    abstract val items: StateFlow<List<WalletSetting>>

}

class WalletSettingsViewModel(
    greenWallet: GreenWallet,
    private val section: WalletSettingsSection,
    val network: Network? = null
) :
    WalletSettingsViewModelAbstract(greenWallet = greenWallet, section = section) {

    private val _items = MutableStateFlow(listOf<WalletSetting>())
    override val items = _items.asStateFlow()

    private val _hasBiometrics = MutableStateFlow(false)

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
        data class SetPgp(val key: String?) : Event
        data class SetPin(val pin: String) : Event, Redact
        data class SetCsvTime(val csvTime: Int, val twoFactorResolver: TwoFactorResolver): Event
        data class SetLimits(val limits: Limits, val twoFactorResolver: TwoFactorResolver): Event
        data class Disable2FA(val method: TwoFactorMethod, val twoFactorResolver: TwoFactorResolver): Event
        object RecoveryPhrase : Event
        object SupportId : Event
    }

    class LocalSideEffects {
        data class OpenAutologoutTimeout(val minutes: Int) : SideEffect
        data class OpenPgpKey(val pgp: String) : SideEffect
        object LaunchBiometrics : SideEffect
    }

    init {
        _navData.value = NavData(title = "id_settings", subtitle = greenWallet.name)

        session.ifConnected {
            database.getLoginCredentialsFlow(greenWallet.id).onEach {
                _hasBiometrics.value = it.biometricsPinData != null
            }.launchIn(viewModelScope.coroutineScope)

            combine(
                session.settings(network = network ?: session.defaultNetwork),
                network?.takeIf { it.isMultisig }?.let { session.twoFactorConfig(network) } ?: flowOf(null),
                session.allAccounts,
                _hasBiometrics
            ) { settings, _, _, _ ->
                settings
            }.onEach {
                _items.value = withContext(Dispatchers.IO) {
                    build(it)
                }
            }.launchIn(this)
        }

        bootstrap()
    }

    private fun build(settings: Settings?): List<WalletSetting> {
        val list = mutableListOf<WalletSetting>()

        if (network != null && section == WalletSettingsSection.RecoveryTransactions) {

            session.getTwoFactorConfig(network)?.also { twoFactorConfig ->
                list += listOfNotNull(
                    WalletSetting.Text(message = "id_if_you_have_some_coins_on_the"),
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
            }

        } else {

            list += WalletSetting.Logout

            if (settings != null) {
                if (greenWallet.isWatchOnly) {
                    list += listOfNotNull(
                        WalletSetting.Text("id_general"),
                        WalletSetting.DenominationExchangeRate(
                            unit = settings.networkUnit(session),
                            currency = settings.pricing.currency,
                            exchange = settings.pricing.exchange
                        ),
                        WalletSetting.Text("id_security"),
                        WalletSetting.AutologoutTimeout(settings.altimeout)
                    )
                } else {
                    list += listOf(
                        WalletSetting.Text("id_general"),
                        WalletSetting.DenominationExchangeRate(
                            unit = settings.networkUnit(session),
                            currency = settings.pricing.currency,
                            exchange = settings.pricing.exchange
                        ),
                    )

                    if (!session.isLightningShortcut) {
                        val hasMultisig =
                            session.activeBitcoinMultisig != null || session.activeLiquidMultisig != null
    
                        list += listOfNotNull(
                            WalletSetting.ArchivedAccounts(session.allAccounts.value.count { it.hidden }),
                            WalletSetting.WatchOnly,
                            WalletSetting.Text("id_security"),
                        )
    
                        if (!greenWallet.isEphemeral && !greenWallet.isHardware) {
                            list += listOf(
                                WalletSetting.ChangePin,
                                WalletSetting.LoginWithBiometrics(
                                    enabled = _hasBiometrics.value,
                                    canEnable = greenKeystore.canUseBiometrics()
                                )
                            )
                        }
    
                        if (hasMultisig) {
                            list += listOf(WalletSetting.TwoFactorAuthentication)
    
                            session.activeMultisig.firstOrNull()?.also {
                                list += listOf(WalletSetting.PgpKey(enabled = session.getSettings(it)?.pgp.isNotBlank()))
                            }
                        }
    
                        list += listOf(WalletSetting.AutologoutTimeout(settings.altimeout))
    
                        if (!session.isHardwareWallet) {
                            list += listOf(
                                WalletSetting.Text("id_recovery"),
                                WalletSetting.RecoveryPhrase
                            )
                        }
                    }

                }
            }

            list += listOf(
                WalletSetting.Text("id_about"),
                WalletSetting.Version(appInfo.versionFlavorDebug),
                WalletSetting.Support
            )
        }



        return list
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {

            is LocalEvents.WatchOnly -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnly(greenWallet)))
            }

            is LocalEvents.SetupEmailRecovery -> {
                network?.also {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.TwoFactorSetup(
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
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.ChangePin(greenWallet)))
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
                    }
                } else if (greenKeystore.canUseBiometrics()) {
                    postSideEffect(LocalSideEffects.LaunchBiometrics)
                }
            }

            is LocalEvents.TwoFactorAuthentication -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.TwoFactorAuthentication()
                    )
                )
            }

            is LocalEvents.AutologoutTimeout -> {
                postSideEffect(
                    LocalSideEffects.OpenAutologoutTimeout(
                        session.getSettings()?.altimeout ?: 1
                    )
                )
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

            is LocalEvents.SetPgp -> {
                savePgp(event.key)
            }

            is LocalEvents.SetPin -> {
                setPin(event.pin)
            }

            is LocalEvents.SetCsvTime -> {
                setCsvTime(event.csvTime, event.twoFactorResolver)
            }

            is LocalEvents.SetLimits -> {
                setLimits(event.limits, event.twoFactorResolver)
            }

            is LocalEvents.Disable2FA -> {
                disable2FA(event.method, event.twoFactorResolver)
            }

            is LocalEvents.RecoveryPhrase -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoveryIntro(
                            args = SetupArgs(
                                greenWallet = greenWallet,
                                isShowRecovery = true
                            )
                        )
                    )
                )
            }

            is LocalEvents.SupportId -> {
                postSideEffect(SideEffects.CopyToClipboard(value = session.supportId()))
                postSideEffect(SideEffects.Snackbar(text = "id_copied_to_clipboard"))
            }
        }
    }

    private fun saveGlobalSettings(newSettings: Settings) {
        doAsync({
            session.changeGlobalSettings(newSettings)
            if (!greenWallet.isEphemeral) {
                greenWallet.also {
                    // Pass settings to Lightning Shortcut
                    sessionManager.getWalletSessionOrNull(it.lightningShortcutWallet())
                        ?.also { lightningSession ->
                            lightningSession.changeGlobalSettings(newSettings)
                        }

                    it.extras = WalletExtras(settings = newSettings.forWalletExtras())

                    database.updateWallet(it)
                }
            }
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

    private fun toggleRecoveryTransactionsEmails() {
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
            val pin = randomChars(15)
            val credentials = session.getCredentials()
            val encryptWithPin =
                session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))

            val encryptedData = greenKeystore.encryptData(cipher, pin.encodeToByteArray())

            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = greenWallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.BIOMETRICS_PINDATA,
                    pinData = encryptWithPin.pinData,
                    encryptedData = encryptedData
                )
            )
        }, onSuccess = {

        })
    }

    private fun setLimits(limits: Limits, twoFactorResolver: TwoFactorResolver) {
        if (network == null) return
        doAsync({
            session.twoFactorChangeLimits(network, limits, twoFactorResolver)
        }, onSuccess = {

        })
    }

    private fun disable2FA(
        method: TwoFactorMethod,
        twoFactorResolver: TwoFactorResolver
    ) {
        if (network == null) return
        doAsync({
            session
                .changeSettingsTwoFactor(
                    network = network,
                    method = method.gdkType,
                    methodConfig = TwoFactorMethodConfig(enabled = false),
                    twoFactorResolver = twoFactorResolver
                )
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    private fun setCsvTime(csvTime: Int, twoFactorResolver: TwoFactorResolver) {
        if(network == null) return

        doAsync({
            session.setCsvTime(network = network, value = CsvParams(csvTime), twoFactorResolver = twoFactorResolver)
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    private fun setPin(pin: String){
        doAsync({
            val credentials = session.getCredentials()
            val encryptWithPin = session.encryptWithPin(null, EncryptWithPinParams(pin, credentials))

            // Replace PinData
            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = greenWallet.id,
                    network = encryptWithPin.network.id,
                    credentialType = CredentialType.PIN_PINDATA,
                    pinData = encryptWithPin.pinData
                )
            )

            // We only allow one credential type PIN / Password
            // Password comes from v2 and should be deleted when a user tries to change his
            // password to a pin
            database.deleteLoginCredentials(greenWallet.id, CredentialType.PASSWORD_PINDATA)
        }, onSuccess = {
            postSideEffect(SideEffects.Snackbar("id_you_have_successfully_changed"))
            postSideEffect(SideEffects.NavigateBack())
        })
    }
}

class WalletSettingsViewModelPreview(
    greenWallet: GreenWallet,
    section: WalletSettingsSection = WalletSettingsSection.General
) :
    WalletSettingsViewModelAbstract(greenWallet = greenWallet, section = section) {

    override val items: StateFlow<List<WalletSetting>> = MutableStateFlow(
        if (section == WalletSettingsSection.RecoveryTransactions) {
            listOf(
                WalletSetting.Text(message = "id_if_you_have_some_coins_on_the"),
                WalletSetting.LearnMore(event = Events.OpenBrowser(url = Urls.HELP_NLOCKTIMES)),
                WalletSetting.RecoveryTransactionEmails(enabled = true),
                WalletSetting.RequestRecoveryTransactions,
                WalletSetting.SetupEmailRecovery
            )
        } else {
            listOf(
                WalletSetting.Logout,
                WalletSetting.Text("id_general"),
                WalletSetting.DenominationExchangeRate(
                    unit = BTC_UNIT,
                    currency = "USD",
                    exchange = "BITFINEX"
                ),
                WalletSetting.ArchivedAccounts(2),
                WalletSetting.WatchOnly,
                WalletSetting.Text("id_security"),
                WalletSetting.ChangePin,
                WalletSetting.LoginWithBiometrics(enabled = true, canEnable = true),
                WalletSetting.TwoFactorAuthentication,
                WalletSetting.PgpKey(enabled = false),
                WalletSetting.AutologoutTimeout(5),
                WalletSetting.Text("id_recovery"),
                WalletSetting.RecoveryPhrase,
                WalletSetting.Text("id_about"),
                WalletSetting.Version("1.0.0"),
                WalletSetting.Support,
            )
        }
    )

    companion object {
        fun preview() = WalletSettingsViewModelPreview(previewWallet(isHardware = false))
        fun previewRecovery() = WalletSettingsViewModelPreview(
            previewWallet(isHardware = false),
            section = WalletSettingsSection.RecoveryTransactions
        )
    }
}