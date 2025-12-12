package com.blockstream.compose.models.overview

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_security
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.wallet.LoginCredentials
import com.blockstream.common.devices.DeviceModel
import com.blockstream.common.devices.jadeDevice
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.usecases.SetBiometricsUseCase
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
import com.blockstream.jade.firmware.JadeFirmwareManager
import com.blockstream.jade.firmware.JadeFirmwareManager.Companion.JADE_FW_VERSIONS_LATEST
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

@Serializable
enum class PendingAction {
    CONNECT, GENUINE_CHECK, FIRMWARE_UPDATE
}

abstract class SecurityViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "SecurityTab"

    abstract val isHardware: Boolean
    abstract val isJade: StateFlow<Boolean>
    abstract val credentials: StateFlow<List<Pair<CredentialType, LoginCredentials?>>>
    abstract val showRecoveryConfirmation: StateFlow<Boolean>
    abstract val showGenuineCheck: StateFlow<Boolean>

    internal val credentialTypes =
        listOf(CredentialType.BIOMETRICS_MNEMONIC, CredentialType.PIN_PINDATA)

    private var pendingAction: PendingAction? = null

    fun genuineCheck() {
        if (session.isHwWatchOnlyWithNoDevice) {
            connectDevice(pendingAction = PendingAction.GENUINE_CHECK)
        } else {
            postEvent(NavigateDestinations.JadeGenuineCheck(greenWalletOrNull = greenWallet))
        }
    }

    fun connectDevice(pendingAction: PendingAction = PendingAction.CONNECT) {
        this.pendingAction = pendingAction
        postEvent(
            NavigateDestinations.DeviceScan(
                greenWallet = greenWallet,
                isWatchOnlyDeviceConnect = true
            )
        )
    }

    fun firmwareUpdate(channel: String? = null) {
        if (isHwWatchOnly.value) {
            connectDevice(PendingAction.FIRMWARE_UPDATE)
        } else {
            if (appInfo.isDevelopmentOrDebug && channel == null) {
                postSideEffect(SecurityViewModel.LocalSideEffects.SelectFirmwareChannel())
            } else {
                doAsync({
                    val firmwareManager = JadeFirmwareManager(
                        firmwareInteraction = this,
                        httpRequestHandler = sessionManager.httpRequestHandler,
                        jadeFwVersionsFile = channel ?: JADE_FW_VERSIONS_LATEST,
                        forceFirmwareUpdate = true
                    )

                    firmwareManager.checkFirmware(jade = session.device!!.jadeDevice()!!.jadeApi!!) {
                        postSideEffect(SideEffects.Snackbar(StringHolder.create(it)))
                    }
                })
            }
        }
    }

    fun executePendingAction() {
        if (!isHwWatchOnly.value) {
            when (pendingAction) {
                PendingAction.GENUINE_CHECK -> genuineCheck()
                PendingAction.FIRMWARE_UPDATE -> firmwareUpdate()
                else -> null
            }
        }
    }
}

class SecurityViewModel(greenWallet: GreenWallet) :
    SecurityViewModelAbstract(greenWallet = greenWallet) {

    private val setBiometricsUseCase: SetBiometricsUseCase by inject()

    override val isHardware: Boolean
        get() = greenWalletOrNull?.isHardware == true

    override val isJade: StateFlow<Boolean> = session.isWatchOnly.map {
        (session.device?.isJade
            ?: greenWalletOrNull?.deviceIdentifiers?.any { it.model?.isJade == true }) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    override fun segmentation(): HashMap<String, Any> =
        countly.sessionSegmentation(session = session)

    override val credentials: StateFlow<List<Pair<CredentialType, LoginCredentials?>>> =
        database.getLoginCredentialsFlow(id = greenWallet.id).map { credentials ->
            credentialTypes.map { credentialType ->
                credentialType to credentials.find { it.credential_type == credentialType || (credentialType == CredentialType.BIOMETRICS_MNEMONIC && it.credential_type == CredentialType.BIOMETRICS_PINDATA) }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            credentialTypes.map { it to null }
        )

    override val showRecoveryConfirmation: StateFlow<Boolean> =
        greenWalletFlow.map { it?.isRecoveryConfirmed == false }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        )

    override val showGenuineCheck: StateFlow<Boolean> = session.isWatchOnly.map {
        (session.device?.jadeDevice()?.supportsGenuineCheck()
            ?: greenWalletOrNull?.deviceIdentifiers?.any { it.model == DeviceModel.BlockstreamJadePlus }) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    class LocalEvents {
        data object EnableBiometrics : Event
        data object DisableBiometrics : Event
        data object EnablePin : Event
        data object DisablePin : Event
    }

    class LocalSideEffects {
        data class SelectFirmwareChannel(
            val channels: List<String> = listOf(
                JadeFirmwareManager.JADE_FW_VERSIONS_BETA,
                JADE_FW_VERSIONS_LATEST,
                JadeFirmwareManager.JADE_FW_VERSIONS_PREVIOUS
            )
        ) : SideEffect
    }

    init {
        greenWalletFlow.filterNotNull().onEach {
            updateNavData(it)
        }.launchIn(this)

        viewModelScope.launch {
            updateNavData(greenWallet)
        }

        bootstrap()
    }

    private suspend fun updateNavData(greenWallet: GreenWallet) {
        _navData.value = NavData(
            title = getString(Res.string.id_security),
            walletName = greenWallet.name,
            showBadge = !greenWallet.isRecoveryConfirmed,
            showBottomNavigation = true
        )
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is LocalEvents.EnableBiometrics -> {
                doAsync({
                    if (!greenKeystore.canUseBiometrics()) {
                        throw Exception("Biometric authentication is not available")
                    }

                    val biometricsCipherProvider = viewModelScope.async(
                        start = CoroutineStart.LAZY
                    ) {
                        CompletableDeferred<PlatformCipher>().let {
                            biometricsPlatformCipher = it
                            postSideEffect(SideEffects.RequestBiometricsCipher)
                            it.await()
                        }
                    }

                    setBiometricsUseCase.invoke(
                        session = session,
                        cipher = biometricsCipherProvider.await(),
                        wallet = greenWallet
                    )
                })
            }

            is LocalEvents.DisableBiometrics -> {
                if (credentials.value.all { it.second != null }) {
                    doAsync({
                        database.deleteLoginCredentials(
                            walletId = greenWallet.id,
                            type = CredentialType.BIOMETRICS_PINDATA
                        )
                        database.deleteLoginCredentials(
                            walletId = greenWallet.id,
                            type = CredentialType.BIOMETRICS_MNEMONIC
                        )
                    }, onSuccess = {})
                } else {
                    postSideEffect(SideEffects.ErrorSnackbar(Exception("Cannot disable Biometrics when no alternative credentials are set")))
                }
            }

            is LocalEvents.EnablePin -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.ChangePin(greenWallet = greenWallet)))
            }

            is LocalEvents.DisablePin -> {
                if (credentials.value.all { it.second != null }) {
                    doAsync({
                        database.deleteLoginCredentials(
                            walletId = greenWallet.id,
                            type = CredentialType.PIN_PINDATA
                        )
                    }, onSuccess = {})
                } else {
                    postSideEffect(SideEffects.ErrorSnackbar(Exception("Cannot disable Pin when no alternative credentials are set")))
                }
            }
        }
    }
}

class SecurityViewModelPreview(override val isHardware: Boolean = false) :
    SecurityViewModelAbstract(greenWallet = previewWallet(isHardware = isHardware)) {

    override val isJade: StateFlow<Boolean> = MutableStateFlow(isHardware)
    override val showRecoveryConfirmation: StateFlow<Boolean> = MutableStateFlow(true)
    override val showGenuineCheck: StateFlow<Boolean> = MutableStateFlow(false)

    override val credentials: StateFlow<List<Pair<CredentialType, LoginCredentials?>>> =
        MutableStateFlow(
            listOf(
                CredentialType.BIOMETRICS_MNEMONIC to LoginCredentials(
                    "",
                    CredentialType.PIN_PINDATA,
                    "",
                    null,
                    null,
                    null,
                    0L
                ),
                CredentialType.PIN_PINDATA to null
            )
        )

    companion object : Loggable() {
        fun preview(isHardware: Boolean = false) = SecurityViewModelPreview(isHardware = isHardware)
    }
}