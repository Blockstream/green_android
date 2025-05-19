package com.blockstream.common.models.overview

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_security
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.wallet.LoginCredentials
import com.blockstream.common.devices.jadeDevice
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.usecases.SetBiometricsUseCase
import com.blockstream.green.utils.Loggable
import com.blockstream.jade.firmware.JadeFirmwareManager
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject


abstract class SecurityViewModelAbstract(
    greenWallet: GreenWallet
) : GreenViewModel(greenWalletOrNull = greenWallet) {

    override fun screenName(): String = "Security"

    abstract val isHardware: Boolean
    abstract val isJade: Boolean

    @NativeCoroutinesState
    abstract val credentials: StateFlow<List<Pair<CredentialType, LoginCredentials?>>>

    @NativeCoroutinesState
    abstract val showRecoveryConfirmation: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showGenuineCheck: StateFlow<Boolean>

    internal val credentialTypes =
        listOf(CredentialType.BIOMETRICS_MNEMONIC, CredentialType.PIN_PINDATA)

    fun genuineCheck() {
        postEvent(NavigateDestinations.JadeGenuineCheck(greenWalletOrNull = greenWallet))
    }

    fun firmwareUpdate() {
        doAsync({
            val firmwareManager = JadeFirmwareManager(
                firmwareInteraction = this,
                httpRequestHandler = sessionManager.httpRequestHandler,
            )

            firmwareManager.checkFirmware(jade = session.device!!.jadeDevice()!!.jadeApi!!)
        })
    }
}

class SecurityViewModel(greenWallet: GreenWallet) :
    SecurityViewModelAbstract(greenWallet = greenWallet) {

    private val setBiometricsUseCase: SetBiometricsUseCase by inject()

    override val isHardware: Boolean
        get() = greenWalletOrNull?.isHardware == true

    override val isJade: Boolean
        get() = sessionOrNull?.device?.isJade == true

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

    override val showGenuineCheck: StateFlow<Boolean> = session.ifConnected {
        isHwWatchOnly.map {
            session.device?.jadeDevice()?.supportsGenuineCheck() ?: false
        }
    }?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false) ?: MutableStateFlow(
        false
    )

    class LocalEvents {
        data object EnableBiometrics : Event
        data object DisableBiometrics : Event
        data object EnablePin : Event
        data object DisablePin : Event
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

                    val biometricsCipherProvider = viewModelScope.coroutineScope.async(
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

    override val isJade: Boolean
        get() = isHardware

    override val showRecoveryConfirmation: StateFlow<Boolean> = MutableStateFlow(true)
    override val showGenuineCheck: StateFlow<Boolean> = MutableStateFlow(false)

    override val credentials: StateFlow<List<Pair<CredentialType, LoginCredentials?>>> =
        MutableStateFlow(
            viewModelScope,
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