package com.blockstream.compose.models.login

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_bip39_passphrase_login
import blockstream_green.common.generated.resources.id_emergency_recovery_phrase
import blockstream_green.common.generated.resources.id_help
import blockstream_green.common.generated.resources.id_if_for_any_reason_you_cant
import blockstream_green.common.generated.resources.id_invalid_pin_you_have_1d
import blockstream_green.common.generated.resources.id_last_attempt_if_failed_you_will
import blockstream_green.common.generated.resources.id_remove_wallet
import blockstream_green.common.generated.resources.id_rename_wallet
import blockstream_green.common.generated.resources.id_show_recovery_phrase
import blockstream_green.common.generated.resources.key
import blockstream_green.common.generated.resources.password
import blockstream_green.common.generated.resources.question
import blockstream_green.common.generated.resources.text_aa
import blockstream_green.common.generated.resources.trash
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewLoginCredentials
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.SetupDevelopmentEnv
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.Urls
import com.blockstream.data.banner.Banner
import com.blockstream.data.crypto.KeystoreInvalidatedException
import com.blockstream.data.crypto.PlatformCipher
import com.blockstream.data.data.ApplicationSettings
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.DataState
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.LogoutReason
import com.blockstream.data.data.MultipleWatchOnlyCredentials
import com.blockstream.data.data.Redact
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.data.SupportData
import com.blockstream.data.data.WalletExtras
import com.blockstream.data.data.WatchOnlyCredentials
import com.blockstream.data.data.data
import com.blockstream.data.data.isEmpty
import com.blockstream.data.database.wallet.LoginCredentials
import com.blockstream.data.extensions.biometrics
import com.blockstream.data.extensions.boltzMnemonic
import com.blockstream.data.extensions.hwWatchOnlyCredentials
import com.blockstream.data.extensions.isConnectionError
import com.blockstream.data.extensions.isNotAuthorized
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.lightningMnemonic
import com.blockstream.data.extensions.logException
import com.blockstream.data.extensions.mnemonic
import com.blockstream.data.extensions.passwordPinData
import com.blockstream.data.extensions.pinPinData
import com.blockstream.data.extensions.richWatchOnly
import com.blockstream.data.extensions.watchOnlyCredentials
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Credentials
import com.blockstream.data.gdk.data.TorEvent
import com.blockstream.data.gdk.device.DeviceResolver
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.managers.DeviceManager
import com.blockstream.data.usecases.EnableHardwareWatchOnlyUseCase
import com.blockstream.domain.lightning.LightningNodeIdUseCase
import com.blockstream.domain.wallet.SaveDerivedBoltzMnemonicUseCase
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import kotlin.io.encoding.Base64

abstract class LoginViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "Login"
    abstract val bip39Passphrase: MutableStateFlow<String>
    abstract val watchOnlyUsername: MutableStateFlow<String>
    abstract val watchOnlyPassword: MutableStateFlow<String>
    abstract val isEmergencyRecoveryPhrase: MutableStateFlow<Boolean>
    abstract val error: StateFlow<String?>
    abstract val tor: StateFlow<TorEvent>
    abstract val applicationSettings: StateFlow<ApplicationSettings>
    abstract val isWatchOnlyLoginEnabled: StateFlow<Boolean>
    abstract val showWatchOnlyUsername: StateFlow<Boolean>
    abstract val showWatchOnlyPassword: StateFlow<Boolean>
    abstract val biometricsCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val richWatchOnlyCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val watchOnlyCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val hwWatchOnlyCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val pinCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val mnemonicCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val passwordCredentials: StateFlow<DataState<LoginCredentials>>
    abstract val lightningMnemonic: StateFlow<DataState<LoginCredentials>>
    abstract val showRestoreWithRecovery: StateFlow<Boolean>
}

class LoginViewModel constructor(
    greenWallet: GreenWallet,
    deviceId: String?,
    val autoLoginWallet: Boolean,
    val isWatchOnlyUpgrade: Boolean,
    val isNewWallet: Boolean = false
) : LoginViewModelAbstract(greenWallet = greenWallet) {
    private val deviceManager: DeviceManager by inject()

    override val isLoginRequired: Boolean = false

    private val saveDerivedBoltzMnemonicUseCase: SaveDerivedBoltzMnemonicUseCase by inject()
    private val lightningNodeIdUseCase: LightningNodeIdUseCase by inject()
    private val enableHardwareWatchOnlyUseCase: EnableHardwareWatchOnlyUseCase by inject()
    private var hwWatchOnlyChoiceDeferred: CompletableDeferred<Boolean>? = null
    private val setupDevelopmentEnv = SetupDevelopmentEnv() // Only for dev env
    override val bip39Passphrase = MutableStateFlow("")
    override val watchOnlyUsername: MutableStateFlow<String> = MutableStateFlow(greenWallet.watchOnlyUsername ?: "")
    override val watchOnlyPassword = MutableStateFlow("")
    override val isEmergencyRecoveryPhrase = MutableStateFlow(false)
    override val tor = sessionManager.torProxyProgress
    override val applicationSettings = settingsManager.appSettingsStateFlow

    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()

    private val isBip39Login
        get() = bip39Passphrase.value.trim().isNotBlank()

    private var _initialAction = MutableStateFlow(false)

    private val _biometricsCredentials: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    private val _watchOnlyCredentials: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    private val _hwWatchOnlyCredentials: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    private val _richWatchOnlyCredentials: MutableStateFlow<DataState<LoginCredentials>> =
        MutableStateFlow(DataState.Loading)
    private val _pinCredentials: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    private val _mnemonicCredentials: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    private val _passwordCredentials: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    private val _lightningMnemonic: MutableStateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Loading)
    override val biometricsCredentials: StateFlow<DataState<LoginCredentials>> = _biometricsCredentials
    override val richWatchOnlyCredentials: StateFlow<DataState<LoginCredentials>> = _richWatchOnlyCredentials
    override val watchOnlyCredentials: StateFlow<DataState<LoginCredentials>> = _watchOnlyCredentials
    override val hwWatchOnlyCredentials: StateFlow<DataState<LoginCredentials>> = _hwWatchOnlyCredentials
    override val pinCredentials: StateFlow<DataState<LoginCredentials>> = _pinCredentials
    override val mnemonicCredentials: StateFlow<DataState<LoginCredentials>> = _mnemonicCredentials
    override val passwordCredentials: StateFlow<DataState<LoginCredentials>> = _passwordCredentials
    override val lightningMnemonic: StateFlow<DataState<LoginCredentials>> = _lightningMnemonic

    override val showRestoreWithRecovery = combine(
        onProgress,
        pinCredentials,
        passwordCredentials,
        biometricsCredentials,
        mnemonicCredentials
    ) { onProgress, pinCredentials, passwordCredentials, biometricsCredentials, mnemonicCredentials ->
        pinCredentials.isEmpty() && !greenWallet.isWatchOnly && !greenWallet.isHardware && passwordCredentials.isEmpty() && biometricsCredentials.isEmpty() && mnemonicCredentials.isEmpty() && !onProgress
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    override val isWatchOnlyLoginEnabled: StateFlow<Boolean> = combine(
        watchOnlyPassword,
        watchOnlyCredentials,
        onProgress,
        hwWatchOnlyCredentials
    ) { watchOnlyPassword, watchOnlyCredentials, onProgress, hwWatchOnlyCredentials ->
        (hwWatchOnlyCredentials.isNotEmpty() || watchOnlyPassword.isNotBlank() || greenWallet.isWatchOnlySingleSig || (!watchOnlyCredentials.isEmpty() && !_initialAction.value)) && !onProgress
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    override val showWatchOnlyUsername = combine(
        _initialAction, watchOnlyCredentials, hwWatchOnlyCredentials
    ) { initialAction, watchOnlyCredentials, hwWatchOnlyCredentials ->
        (watchOnlyCredentials.isEmpty() || (initialAction && greenWallet.isWatchOnlyMultisig)) && hwWatchOnlyCredentials.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    override val showWatchOnlyPassword = combine(
        _initialAction,
        watchOnlyCredentials,
        showWatchOnlyUsername,
        hwWatchOnlyCredentials
    ) { initialAction, watchOnlyCredentials, showWatchOnlyUsername, hwWatchOnlyCredentials ->
        (watchOnlyCredentials.isEmpty() || (initialAction && greenWallet.isWatchOnlyMultisig) || showWatchOnlyUsername) && hwWatchOnlyCredentials.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    class LocalEvents {
        data class LoginWithPin(val pin: String) : Event, Redact
        data class LoginWithBiometrics(val cipher: PlatformCipher, val loginCredentials: LoginCredentials) : Event,
            Redact

        data class LoginWithBiometricsV3(val cipher: PlatformCipher, val loginCredentials: LoginCredentials) : Event,
            Redact

        object LoginWatchOnly : Event
        object Login : Event
        object LoginWithDevice : Event
        object ClickRestoreWithRecovery : Event
        object ClickBiometrics : Event
        object ClickHelp : Events.OpenBrowser(Urls.HELP_MNEMONIC_BACKUP)
        data class Bip39Passphrase(val passphrase: String, val alwaysAsk: Boolean?) : Event, Redact
        data class EmergencyRecovery(val isEmergencyRecovery: Boolean) : Event
        data class DeleteLoginCredentials(val loginCredentials: LoginCredentials) : Event
        data class Authenticated(val isAuthenticated: Boolean) : Event
    }

    class LocalSideEffects {
        data class LaunchBiometrics(val loginCredentials: LoginCredentials) : SideEffect
        data class LaunchUserPresence(val loginCredentials: LoginCredentials) : SideEffect
    }

    override val session: GdkSession by lazy {
        if (isWatchOnlyUpgrade) {
            sessionManager.getOnBoardingSession()
        } else {
            super.session
        }
    }

    init {
        deviceOrNull = deviceId?.let {
            deviceManager.getDevice(it) ?: run {
                postSideEffect(SideEffects.ErrorDialog(Exception("Device wasn't found")))
                postSideEffect(SideEffects.Logout(LogoutReason.DEVICE_DISCONNECTED))
                null
            }
        }

        if (session.isConnected) {
            navigate(greenWallet)
        } else {
            if (greenWallet.askForBip39Passphrase) {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Bip39Passphrase(
                            greenWallet = greenWallet,
                            passphrase = bip39Passphrase.value
                        )
                    )
                )
            }

            deviceOrNull?.let {
                loginWithDevice()
            }

            var handleFirstTime = true
            // Beware as this will fire new values if eg. you change a login credential
            database.getLoginCredentialsFlow(greenWallet.id).onEach {
                _biometricsCredentials.value = DataState.successOrEmpty(it.biometrics)
                _watchOnlyCredentials.value = DataState.successOrEmpty(it.watchOnlyCredentials)
                _hwWatchOnlyCredentials.value = DataState.successOrEmpty(it.hwWatchOnlyCredentials)
                _richWatchOnlyCredentials.value = DataState.successOrEmpty(it.richWatchOnly)
                _pinCredentials.value = DataState.successOrEmpty(it.pinPinData)
                _mnemonicCredentials.value = DataState.successOrEmpty(it.mnemonic)
                _passwordCredentials.value = DataState.successOrEmpty(it.passwordPinData)
                _lightningMnemonic.value = DataState.successOrEmpty(it.lightningMnemonic)

                if (handleFirstTime) {
                    if ((autoLoginWallet) && !_initialAction.value && !greenWallet.askForBip39Passphrase) {
                        if (autoLoginWallet) {
                            biometricsCredentials.data()?.also { biometricsCredentials ->
                                postSideEffect(
                                    LocalSideEffects.LaunchBiometrics(
                                        biometricsCredentials
                                    )
                                )
                            }

                            loginHwWatchOnlyCredentials(it.hwWatchOnlyCredentials)

                            it.watchOnlyCredentials?.also {
                                if (it.credential_type == CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS) {
                                    postSideEffect(LocalSideEffects.LaunchUserPresence(it))
                                }
                            }
                        }
                    }
                    handleFirstTime = false
                }

            }.launchIn(this)

            combine(pinCredentials, passwordCredentials) { pinCredentials, passwordCredentials ->
                pinCredentials.data() ?: passwordCredentials.data()
            }.onEach { dataState ->
                _error.value = dataState?.let {
                    if (it.counter > 0) {
                        if (it.counter == 2L) {
                            getString(Res.string.id_last_attempt_if_failed_you_will)
                        } else {
                            getString(Res.string.id_invalid_pin_you_have_1d, 3 - it.counter)
                        }
                    } else {
                        null
                    }
                }
            }.launchIn(this)
        }

        val check1 = !greenWallet.isHardware
        val check2 = check1 && !greenWallet.isWatchOnly

        combine(greenWalletFlow.filterNotNull(), pinCredentials) { it, _ ->
            _navData.value = NavData(
                title = it.name,
                actions = listOfNotNull(
                    NavAction(
                        title = getString(Res.string.id_help),
                        icon = Res.drawable.question,
                        isMenuEntry = true,

                        ) {
                        postEvent(LocalEvents.ClickHelp)
                    }.takeIf { check2 && pinCredentials.isEmpty() && passwordCredentials.isEmpty() },
                    NavAction(
                        title = getString(Res.string.id_bip39_passphrase_login),
                        icon = Res.drawable.password,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.Bip39Passphrase(
                                    greenWallet = greenWallet,
                                    passphrase = bip39Passphrase.value
                                )
                            )
                        )
                    }.takeIf { check2 && (pinCredentials.value.isNotEmpty() || passwordCredentials.value.isNotEmpty()) },
                    NavAction(
                        title = getString(Res.string.id_show_recovery_phrase),
                        icon = Res.drawable.key,
                        isMenuEntry = true,
                    ) {
                        postEvent(LocalEvents.EmergencyRecovery(true))
                    }.takeIf { check2 && (pinCredentials.value.isNotEmpty() || passwordCredentials.value.isNotEmpty()) },
                    NavAction(
                        title = getString(Res.string.id_rename_wallet),
                        icon = Res.drawable.text_aa,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.RenameWallet(it)))
                    }.takeIf { check1 },

                    NavAction(
                        title = getString(Res.string.id_remove_wallet),
                        icon = Res.drawable.trash,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.DeleteWallet(it)))
                    }.takeIf { check1 },

                    )
            )

        }.launchIn(this)
        
        // Try auto-login for development builds
        setupDevelopmentEnv.tryAutoLogin(this)

        bootstrap()
    }

    private fun loginHwWatchOnlyCredentials(loginCredentials: LoginCredentials?) = loginCredentials
        ?.takeIf { it.credential_type == CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS }
        // Trigger only if device is null
        ?.takeIf { deviceOrNull == null }?.also {
            if (greenKeystore.canUseBiometrics()) {
                postSideEffect(LocalSideEffects.LaunchUserPresence(it))
            } else {
                loginWatchOnlyWithLoginCredentials(loginCredentials = it)
            }
        }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is LocalEvents.Authenticated -> {
                (watchOnlyCredentials.data() ?: hwWatchOnlyCredentials.data())?.also {
                    loginWatchOnlyWithLoginCredentials(it)
                }
            }

            is LocalEvents.LoginWithDevice -> {
                loginWithDevice()
            }

            is LocalEvents.LoginWithPin -> {
                (pinCredentials.value.data() ?: passwordCredentials.value.data())?.also {
                    loginWithPinOrMnemonic(pin = event.pin, loginCredentials = it)
                } ?: run {
                    postSideEffect(SideEffects.ErrorDialog(Exception("No Pin Credentials or Password Credentials")))
                }
            }

            is LocalEvents.LoginWithBiometrics -> {
                loginWithBiometrics(cipher = event.cipher, loginCredentials = event.loginCredentials)
            }

            is LocalEvents.LoginWithBiometricsV3 -> {
                loginWithBiometricsV3(cipher = event.cipher, loginCredentials = event.loginCredentials)
            }

            is LocalEvents.DeleteLoginCredentials -> {
                deleteLoginCredentials(loginCredentials = event.loginCredentials)
            }

            is LocalEvents.EmergencyRecovery -> {
                bip39Passphrase.value = ""
                isEmergencyRecoveryPhrase.value = event.isEmergencyRecovery
                if (event.isEmergencyRecovery) {
                    postSideEffect(
                        SideEffects.Dialog(
                            StringHolder.create(Res.string.id_emergency_recovery_phrase),
                            StringHolder.create(Res.string.id_if_for_any_reason_you_cant)
                        )
                    )
                }
            }

            is LocalEvents.Bip39Passphrase -> {
                setBip39Passphrase(event.passphrase, event.alwaysAsk)
            }

            is LocalEvents.ClickBiometrics -> {
                _biometricsCredentials.data()?.also {
                    postSideEffect(LocalSideEffects.LaunchBiometrics(it))
                } ?: _hwWatchOnlyCredentials.data()?.also {
                    postSideEffect(LocalSideEffects.LaunchUserPresence(it))
                }
            }

            is LocalEvents.ClickRestoreWithRecovery -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.EnterRecoveryPhrase(
                            setupArgs = SetupArgs.restoreMnemonic(greenWallet)
                        )
                    )
                )
            }

            is LocalEvents.LoginWatchOnly -> {
                (richWatchOnlyCredentials.data() ?: watchOnlyCredentials.data() ?: hwWatchOnlyCredentials.data()).also {
                    if (it?.credential_type == CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS) {
                        loginHwWatchOnlyCredentials(it)
                    } else if (it?.credential_type == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS) {
                        postSideEffect(LocalSideEffects.LaunchBiometrics(it))
                    } else if (it?.credential_type == CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS) {
                        postSideEffect(LocalSideEffects.LaunchUserPresence(it))
                    } else {
                        if (!_initialAction.value && it != null) {
                            loginWatchOnlyWithLoginCredentials(it)
                        } else {
                            watchOnlyLogin()
                        }
                    }
                }
            }

            is LocalEvents.Login -> {
                mnemonicCredentials.data()?.also {
                    loginWithPinOrMnemonic(loginCredentials = it)
                }
            }
        }
    }

    private fun setBip39Passphrase(passphrase: String, alwaysAsk: Boolean?) {
        bip39Passphrase.value = passphrase.trim()
        if (alwaysAsk != null) {
            viewModelScope.launch(context = logException(countly)) {
                greenWallet.also {
                    it.askForBip39Passphrase = alwaysAsk
                    database.updateWallet(it)
                }
            }
        }

        // and is always ask
        if (!_initialAction.value) {
            biometricsCredentials.data()?.let { biometricsCredentials ->
                postSideEffect(LocalSideEffects.LaunchBiometrics(biometricsCredentials))
            }
        }
    }

    private fun emergencyRecoveryPhrase(
        pin: String? = null,
        mnemonic: String? = null,
        loginCredentials: LoginCredentials
    ) {
        doAsync({
            pin?.let {
                session.emergencyRestoreOfRecoveryPhrase(
                    wallet = greenWallet,
                    pin = it,
                    loginCredentials = loginCredentials
                ).also {
                    session.disconnect()
                }
            } ?: mnemonic?.let {
                Credentials(mnemonic = it)
            } ?: throw Exception("Couldn't restore recovery phrase")
        }, onSuccess = { credentials ->
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.RecoveryPhrase(SetupArgs(credentials = credentials))))
            isEmergencyRecoveryPhrase.value = false
        }, onError = {
            if (it.isNotAuthorized()) {
                viewModelScope.launch {
                    database.replaceLoginCredential(loginCredentials.copy(counter = loginCredentials.counter + 1))
                }
            } else {
                postSideEffect(SideEffects.ErrorSnackbar(it))
            }

            countly.failedWalletLogin(session, it)
        })
    }

    private fun loginWithDevice() {
        deviceOrNull?.gdkHardwareWallet?.also { gdkHardwareWallet ->
            login {

                // Do a database query as the StateFlow is not yet initialized
                val derivedLightningMnemonic = database.getLoginCredential(
                    id = greenWallet.id,
                    credentialType = CredentialType.LIGHTNING_MNEMONIC
                )?.lightningMnemonic(greenKeystore) {
                    postSideEffect(SideEffects.ErrorSnackbar(it))
                }

                val derivedBoltzMnemonic = database.getLoginCredential(
                    id = greenWallet.id,
                    credentialType = CredentialType.BOLTZ_MNEMONIC
                )?.boltzMnemonic(greenKeystore) {
                    postSideEffect(SideEffects.ErrorSnackbar(it))
                }

                session.loginWithDevice(
                    wallet = greenWallet,
                    device = device,
                    hardwareWalletResolver = DeviceResolver(gdkHardwareWallet, this),
                    derivedLightningMnemonic = derivedLightningMnemonic,
                    derivedBoltzMnemonic = derivedBoltzMnemonic,
                    isSmartDiscovery = !isWatchOnlyUpgrade,
                    hwInteraction = this
                )

                if (!isWatchOnlyUpgrade) {
                    val existingCredentials = database.getLoginCredential(
                        id = greenWallet.id,
                        credentialType = CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
                    )

                    val allAccountsSinglesig = session.allAccounts.value.all { it.isSinglesig }

                    val shouldShowPrompt = isNewWallet
                            && device.isJade
                            && existingCredentials == null
                            && greenWallet.extras?.hwWatchOnlyEnabled == null
                            && allAccountsSinglesig
                            && greenKeystore.isBiometricEnrolled()

                    val shouldEnableWatchOnly = if (shouldShowPrompt) {
                        CompletableDeferred<Boolean>().also { deferred ->
                            hwWatchOnlyChoiceDeferred = deferred
                            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.HwWatchOnlyChoice))
                        }.await()
                    } else {
                        (greenWallet.extras?.hwWatchOnlyEnabled ?: greenKeystore.isBiometricEnrolled()) && allAccountsSinglesig
                    }

                    if (greenWallet.extras?.hwWatchOnlyEnabled == null) {
                        greenWallet.extras = (greenWallet.extras ?: WalletExtras()).copy(hwWatchOnlyEnabled = shouldEnableWatchOnly)
                        database.updateWallet(greenWallet)
                    }

                    if (shouldEnableWatchOnly) {
                        enableHardwareWatchOnlyUseCase(greenWallet = greenWallet, session = session)
                    }
                }
            }
        } ?: run {
            postSideEffect(SideEffects.Logout(LogoutReason.DEVICE_DISCONNECTED))
        }
    }

    private fun loginWithPinOrMnemonic(pin: String? = null, mnemonic: String? = null, loginCredentials: LoginCredentials) {
        if (isEmergencyRecoveryPhrase.value) {
            emergencyRecoveryPhrase(pin = pin, mnemonic = mnemonic, loginCredentials = loginCredentials)
            return
        }

        // If Wallet Hash ID is empty (from migration) do a one-time wallet restore to make a real account discovery
        val isRestore = greenWallet.xPubHashId.isEmpty()

        login(loginCredentials) {
            // if bip39 passphrase, don't initialize the session as we need to re-connect || initializeSession = bip39Passphrase.isNullOrBlank())
            session.loginWithWallet(
                wallet = greenWallet,
                pin = pin,
                mnemonic = mnemonic
                    ?: if (loginCredentials.credential_type == CredentialType.KEYSTORE_MNEMONIC) loginCredentials.mnemonic(
                        greenKeystore
                    ) else null,
                loginCredentials = loginCredentials,
                isRestore = isRestore,
                isLightningEnabled = !isBip39Login && walletSettingsManager.isLightningEnabled(walletId = greenWallet.id),
                initializeSession = !isBip39Login
            )
        }
    }

    private fun loginWatchOnlyWithLoginCredentials(loginCredentials: LoginCredentials) {
        _initialAction.value = true

        loginCredentials.encrypted_data?.let { encryptedData ->
            login(loginCredentials, isWatchOnly = true, updateWatchOnlyPassword = false) {

                if (loginCredentials.credential_type == CredentialType.RICH_WATCH_ONLY) {
                    loginCredentials.richWatchOnly(greenKeystore)?.also {
                        session.loginRichWatchOnly(wallet = greenWallet, loginCredentials = loginCredentials, richWatchOnly = it)
                    }
                } else {
                    val watchOnlyCredentials = (greenKeystore.decryptData(encryptedData).let {
                        when (loginCredentials.credential_type) {
                            CredentialType.KEYSTORE_PASSWORD -> {
                                MultipleWatchOnlyCredentials.fromWatchOnlyCredentials(
                                    network = loginCredentials.network,
                                    watchOnlyCredentials = WatchOnlyCredentials(
                                        username = greenWallet.watchOnlyUsername ?: "",
                                        password = it.decodeToString()
                                    )
                                )
                            }

                            CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS -> {
                                MultipleWatchOnlyCredentials.fromByteArray(it)
                            }

                            else -> {
                                MultipleWatchOnlyCredentials.fromWatchOnlyCredentials(
                                    network = loginCredentials.network,
                                    watchOnlyCredentials = WatchOnlyCredentials.fromByteArray(it)
                                )
                            }
                        }
                    })

                    session.loginWatchOnly(
                        wallet = greenWallet,
                        loginCredentials = loginCredentials,
                        watchOnlyCredentials = watchOnlyCredentials
                    )
                }
            }
        }
    }

    private fun loginWatchOnlyWithWatchOnlyCredentials(loginCredentials: LoginCredentials, watchOnlyCredentials: WatchOnlyCredentials) {
        login(loginCredentials = loginCredentials, isWatchOnly = true, updateWatchOnlyPassword = false) {
            session.loginWatchOnly(
                wallet = greenWallet,
                loginCredentials = loginCredentials,
                watchOnlyCredentials = MultipleWatchOnlyCredentials.fromWatchOnlyCredentials(
                    network = loginCredentials.network,
                    watchOnlyCredentials = watchOnlyCredentials.let {
                        greenWallet.watchOnlyUsername?.takeIf { it.isNotBlank() }?.let { username ->
                            it.copy(username = username)
                        } ?: it
                    }
                )
            )
        }
    }

    private fun loginWithBiometrics(cipher: PlatformCipher, loginCredentials: LoginCredentials) {
        _initialAction.value = true

        loginCredentials.encrypted_data?.let { encryptedData ->
            try {
                greenKeystore.decryptData(cipher, encryptedData)
            } catch (e: Exception) {
                if (e is KeystoreInvalidatedException) {
                    deleteLoginCredentials(loginCredentials = loginCredentials)
                }
                countly.recordException(e)
                postSideEffect(SideEffects.ErrorDialog(e))
                null
            }
        }?.also { decryptedData ->
            when (loginCredentials.credential_type) {
                CredentialType.BIOMETRICS_PINDATA -> {
                    loginWithPinOrMnemonic(pin = decryptedData.decodeToString(), loginCredentials = loginCredentials)
                }

                CredentialType.BIOMETRICS_MNEMONIC -> {
                    loginWithPinOrMnemonic(mnemonic = decryptedData.decodeToString(), loginCredentials = loginCredentials)
                }

                else -> {
                    loginWatchOnlyWithWatchOnlyCredentials(
                        loginCredentials,
                        WatchOnlyCredentials.fromByteArray(decryptedData)
                    )
                }
            }
        }
    }

    private fun loginWithBiometricsV3(cipher: PlatformCipher, loginCredentials: LoginCredentials) {
        _initialAction.value = true

        loginCredentials.encrypted_data?.also { encryptedData ->
            try {
                val decrypted = greenKeystore.decryptData(cipher, encryptedData)
                // Migrated from v3
                val pin = Base64.encode(decrypted).substring(0, 15)
                loginWithPinOrMnemonic(pin = pin, loginCredentials = loginCredentials)
            } catch (e: Exception) {
                countly.recordException(e)
                postSideEffect(SideEffects.ErrorDialog(e))
            }
        }
    }

    private fun watchOnlyLogin() {
        _initialAction.value = true

        login(loginCredentials = null, isWatchOnly = true, updateWatchOnlyPassword = !greenWallet.isWatchOnlySingleSig) {
            session.loginWatchOnly(
                wallet = greenWallet,
                watchOnlyCredentials = MultipleWatchOnlyCredentials.fromWatchOnlyCredentials(
                    network = greenWallet.activeNetwork,
                    watchOnlyCredentials = WatchOnlyCredentials(
                        username = watchOnlyUsername.value,
                        password = watchOnlyPassword.value
                    )
                )
            )
        }
    }

    private fun login(
        loginCredentials: LoginCredentials? = null,
        isWatchOnly: Boolean = false,
        updateWatchOnlyPassword: Boolean = false,
        logInMethod: suspend (GdkSession) -> Unit
    ) {

        doAsync({
            countly.loginWalletStart()

            logInMethod.invoke(session)

            // Enable Swaps for old wallets
            if (!greenWallet.isHardware && !isBip39Login && database.getLoginCredential(
                    id = greenWallet.id,
                    credentialType = CredentialType.BOLTZ_MNEMONIC
                ) == null
            ) {
                saveDerivedBoltzMnemonicUseCase(session = session, wallet = greenWallet)
            }

            // Migrate - add walletHashId
            if (greenWallet.xPubHashId != session.xPubHashId && session.xPubHashId.isNotBlank()) {
                greenWallet.also {
                    it.xPubHashId = session.xPubHashId ?: ""
                    if (!it.isEphemeral) {
                        database.updateWallet(it)
                    }
                }
            }

            // Change active account if necessary (account archived)
            if (greenWallet.activeNetwork != (session.activeAccount.value?.networkId ?: "") ||
                greenWallet.activeAccount != (session.activeAccount.value?.pointer ?: 0)
            ) {
                greenWallet.also {
                    it.activeNetwork = session.activeAccount.value?.networkId ?: session.defaultNetwork.id
                    it.activeAccount = session.activeAccount.value?.pointer ?: 0

                    if (!it.isEphemeral) {
                        database.updateWallet(it)
                    }
                }
            }

            // Reset counter
            loginCredentials?.also {
                database.replaceLoginCredential(it.copy(counter = 0))
            }

            // Update watch-only password if needed
            if (updateWatchOnlyPassword && watchOnlyCredentials.data()?.credential_type != CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS) {
                watchOnlyCredentials.data()?.let {
                    // Delete deprecated credential type
                    if (it.credential_type == CredentialType.KEYSTORE_PASSWORD) {
                        database.deleteLoginCredentials(it)
                    }

                    // Upgrade from old KEYSTORE_PASSWORD if required
                    database.replaceLoginCredential(
                        it.copy(
                            credential_type = CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS,
                            encrypted_data = greenKeystore.encryptData(
                                WatchOnlyCredentials(password = watchOnlyPassword.value).toJson().encodeToByteArray()
                            )
                        )
                    )
                }
            }

            if (isBip39Login) {
                val network = session.defaultNetwork

                val mnemonic = session.getCredentials().mnemonic

                // Disconnect as no longer needed
                session.disconnectAsync(LogoutReason.USER_ACTION)

                var ephemeralWallet = GreenWallet.createEphemeralWallet(
                    ephemeralId = sessionManager.getNextEphemeralId(),
                    name = greenWallet.name,
                    networkId = network.id,
                    isHardware = false
                )

                // Create an ephemeral session
                val ephemeralSession = sessionManager.getWalletSessionOrCreate(ephemeralWallet)

                val loginData = ephemeralSession.loginWithMnemonic(
                    isTestnet = greenWallet.isTestnet,
                    loginCredentialsParams = LoginCredentialsParams(
                        mnemonic = mnemonic,
                        bip39Passphrase = bip39Passphrase.value.trim()
                    ),
                    initializeSession = true,
                    isSmartDiscovery = true,
                    isCreate = false,
                    isRestore = false
                )

                // Check if there is already a BIP39 ephemeral wallet
                val pair = sessionManager.getEphemeralWalletSession(loginData.xpubHashId)?.let {
                    // Disconnect the no longer needed session
                    ephemeralSession.disconnectAsync()

                    // Return the previous connected BIP39 ephemeral session
                    it.ephemeralWallet!! to it
                } ?: run {
                    ephemeralWallet = ephemeralWallet.copy(wallet = ephemeralWallet.wallet.copy(xpub_hash_id = loginData.xpubHashId))

                    // Return the ephemeral wallet
                    ephemeralWallet to ephemeralSession
                }

                pair
            } else {
                // Return the wallet
                val pair = greenWallet to session

                pair
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = { pair ->
            countly.loginWalletEnd(
                wallet = pair.first,
                session = pair.second,
                loginCredentials = loginCredentials
            )

            lightningNodeIdUseCase.invoke(wallet = pair.first, session = session)

            if (isWatchOnlyUpgrade) {
                sessionManager.getWalletSessionOrNull(greenWallet)?.also { woSession ->
                    logger.d { "Upgrade wo session to full" }
                    sessionManager.upgradeOnBoardingSessionToFullSession(woSession = woSession, device = device)
                }
            }

            navigate(pair.first)
        }, onError = {

            if (deviceOrNull == null) {
                if (it.isNotAuthorized()) {
                    loginCredentials?.also { loginCredentials ->

                        viewModelScope.launch {
                            database.replaceLoginCredential(loginCredentials.copy(counter = loginCredentials.counter + 1))
                        }
                    }

                    if (isWatchOnly) {
                        postSideEffect(SideEffects.ErrorSnackbar(it))
                    }

                } else if (isBip39Login && it.message == "id_login_failed") {
                    // On Multisig & BIP39 Passphrase login, instead of registering a new wallet, show error "Wallet not found"
                    // Jade users restoring can still login
                    postSideEffect(SideEffects.ErrorSnackbar(Exception("id_wallet_not_found")))
                } else {
                    val supportData = SupportData.create(throwable = it, session = session)

                    if (it.isConnectionError()) {
                        postSideEffect(
                            SideEffects.ErrorSnackbar(
                                error = it,
                                supportData = supportData
                            )
                        )
                    } else {
                        postSideEffect(
                            SideEffects.ErrorDialog(
                                error = it,
                                supportData = supportData
                            )
                        )
                    }
                }
            } else {
                val supportData = SupportData.create(throwable = it, session = session)
                postSideEffect(SideEffects.NavigateBack(error = it, supportData = supportData))
            }

            countly.failedWalletLogin(session, it)
        })
    }

    private fun deleteLoginCredentials(loginCredentials: LoginCredentials) {
        applicationScope.launch(context = logException(countly)) {
            database.deleteLoginCredentials(loginCredentials)
        }
    }

    fun onHwWatchOnlyChoice(enableBiometrics: Boolean) {
        hwWatchOnlyChoiceDeferred?.complete(enableBiometrics)
        hwWatchOnlyChoiceDeferred = null
    }

    fun navigate(greenWallet: GreenWallet) {
        if (isWatchOnlyUpgrade) {
            postSideEffect(SideEffects.NavigateBack())
        } else {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(greenWallet)))
        }
    }

    companion object : Loggable()
}

class LoginViewModelPreview(
    greenWallet: GreenWallet,
    withPinCredentials: Boolean = false,
    withPasswprdCredentials: Boolean = false,
    withDevice: Boolean = false,
    isWatchOnly: Boolean = false,
) : LoginViewModelAbstract(greenWallet = greenWallet) {
    override val bip39Passphrase: MutableStateFlow<String> = MutableStateFlow("")
    override val watchOnlyUsername: MutableStateFlow<String> = MutableStateFlow(if (isWatchOnly) "username" else "")
    override val watchOnlyPassword: MutableStateFlow<String> = MutableStateFlow(if (isWatchOnly) "password" else "")
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val isEmergencyRecoveryPhrase: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tor: StateFlow<TorEvent> = MutableStateFlow(TorEvent(progress = 0))
    override val applicationSettings: StateFlow<ApplicationSettings> = MutableStateFlow(ApplicationSettings())
    override val isWatchOnlyLoginEnabled: StateFlow<Boolean> = MutableStateFlow(isWatchOnly)
    override val showWatchOnlyUsername: StateFlow<Boolean> = MutableStateFlow(false)
    override val showWatchOnlyPassword: StateFlow<Boolean> = MutableStateFlow(false)
    override val biometricsCredentials: StateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Empty)
    override val richWatchOnlyCredentials: StateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Empty)
    override val watchOnlyCredentials: StateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Empty)
    override val hwWatchOnlyCredentials: StateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Empty)
    override val pinCredentials: StateFlow<DataState<LoginCredentials>> =
        MutableStateFlow(if (withPinCredentials) DataState.Success(previewLoginCredentials()) else DataState.Empty)
    override val mnemonicCredentials: StateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Empty)
    override val passwordCredentials: StateFlow<DataState<LoginCredentials>> =
        MutableStateFlow(if (withPasswprdCredentials) DataState.Success(previewLoginCredentials()) else DataState.Empty)
    override val lightningMnemonic: StateFlow<DataState<LoginCredentials>> = MutableStateFlow(DataState.Empty)
    override val showRestoreWithRecovery = MutableStateFlow(false)

    init {
        banner.value = Banner.preview3
    }

    companion object {
        fun preview(): LoginViewModelPreview {
            return LoginViewModelPreview(
                greenWallet = previewWallet()
            )
        }

        fun previewWithPin(): LoginViewModelPreview {
            return LoginViewModelPreview(
                greenWallet = previewWallet(),
                withPinCredentials = true
            )
        }

        fun previewWithPassword(): LoginViewModelPreview {
            return LoginViewModelPreview(
                greenWallet = previewWallet(),
                withPasswprdCredentials = true
            )
        }

        fun previewWatchOnly(): LoginViewModelPreview {
            return LoginViewModelPreview(
                greenWallet = previewWallet(isWatchOnly = true),
                isWatchOnly = true
            )
        }

        fun previewWithDevice(): LoginViewModelPreview {
            return LoginViewModelPreview(
                greenWallet = previewWallet(isHardware = true),
                withDevice = true
            )
        }
    }
}