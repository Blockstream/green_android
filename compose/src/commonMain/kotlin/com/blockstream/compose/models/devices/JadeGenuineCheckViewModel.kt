package com.blockstream.compose.models.devices

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_your_device_was_disconnected
import com.blockstream.data.SupportType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SupportData
import com.blockstream.data.devices.DeviceState
import com.blockstream.data.devices.JadeDevice
import com.blockstream.data.gdk.events.JadeGenuineCheck
import com.blockstream.data.gdk.params.RsaVerifyParams
import com.blockstream.data.utils.randomChars
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewGreenDevice
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.utils.Loggable
import com.blockstream.jade.data.JadeError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class JadeGenuineCheckViewModelAbstract(greenWalletOrNull: GreenWallet?) :
    AbstractDeviceViewModel(greenWalletOrNull = greenWalletOrNull) {
    override fun screenName(): String = "JadeGenuineCheck"
    abstract val genuineState: StateFlow<JadeGenuineCheckViewModel.GenuineState>
}

class JadeGenuineCheckViewModel constructor(greenWalletOrNull: GreenWallet?, deviceId: String?) :
    JadeGenuineCheckViewModelAbstract(greenWalletOrNull) {

    private val _isGenuine = MutableStateFlow<GenuineState>(GenuineState.CHECKING)
    override val genuineState: StateFlow<GenuineState> = _isGenuine

    private val jadeDevice
        get() = device as JadeDevice

    enum class GenuineState {
        CHECKING, GENUINE, NOT_GENUINE, CANCELLED
    }

    class LocalEvents {
        object Cancel : Event
        object Retry : Event
        object ContinueAsDIY : Event
        object ContactSupport : Event
    }

    init {
        disconnectDeviceOnCleared = false

        deviceOrNull = sessionOrNull?.device ?: deviceManager.getDevice(deviceId)

        if (deviceOrNull == null) {
            postSideEffect(SideEffects.NavigateBack())
        } else {
            device.deviceState.onEach {
                // Device went offline
                if (it == DeviceState.DISCONNECTED) {
                    postSideEffect(SideEffects.Snackbar(StringHolder(stringResource = Res.string.id_your_device_was_disconnected)))
                    postSideEffect(SideEffects.NavigateBack())
                }
            }.launchIn(this)
        }

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(this)

        genuineCheck()

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is Events.Continue -> {
                postSideEffect(SideEffects.Success())
            }

            is LocalEvents.Retry -> {
                genuineCheck()
            }

            is LocalEvents.Cancel -> {
                postSideEffect(SideEffects.NavigateBack())
            }

            is LocalEvents.ContinueAsDIY -> {
                jadeGenuineEvent()
                postSideEffect(SideEffects.Success())
            }

            is LocalEvents.ContactSupport -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Support(
                            type = SupportType.INCIDENT,
                            supportData = SupportData(
                                subject = "Jade Genuine check failed",
                                zendeskHardwareWallet = deviceOrNull?.deviceModel?.zendeskValue
                            ),
                            greenWalletOrNull = greenWalletOrNull
                        )
                    )
                )
            }
        }
    }

    private suspend fun jadeGenuineEvent() {
        if (settingsManager.appSettings.rememberHardwareDevices) {
            jadeDevice.jadeApi?.getVersionInfo()?.efuseMac?.also {
                database.insertEvent(JadeGenuineCheck(jadeId = it).sha256())
            }
        }
    }

    private fun genuineCheck() {
        doAsync({
            jadeDevice.let {
                _isGenuine.value = GenuineState.CHECKING

                val challenge = randomChars(32).encodeToByteArray()
                val attestation = it.jadeApi!!.signAttestation(challenge = challenge)

                val verifyChallenge = sessionManager.httpRequestHandler.rsaVerify(
                    RsaVerifyParams(
                        pem = attestation.pubkeyPem,
                        challenge = challenge.toHexString(),
                        signature = attestation.signature.toHexString()
                    )
                )
                val verifyPubKey = sessionManager.httpRequestHandler.rsaVerify(
                    RsaVerifyParams(
                        pem = RsaVerifyParams.VerifyingAuthorityPubKey,
                        challenge = attestation.pubkeyPem.encodeToByteArray().toHexString(),
                        signature = attestation.extSignature.toHexString()
                    )
                )

                if (verifyChallenge.result == true && verifyPubKey.result == true) {
                    jadeGenuineEvent()
                    true
                } else {
                    throw Exception(verifyChallenge.error ?: verifyPubKey.error)
                }
            }

        }, onSuccess = {
            _isGenuine.value = GenuineState.GENUINE
        }, onError = {
            _isGenuine.value = if ((it as? JadeError)?.code == JadeError.CBOR_RPC_USER_CANCELLED) {
                GenuineState.CANCELLED
            } else {
                GenuineState.NOT_GENUINE
            }
        })
    }

    companion object : Loggable()
}

class JadeGenuineCheckViewModelPreview : JadeGenuineCheckViewModelAbstract(previewWallet()) {
    override val genuineState: MutableStateFlow<JadeGenuineCheckViewModel.GenuineState> =
        MutableStateFlow(JadeGenuineCheckViewModel.GenuineState.CHECKING)

    init {
        deviceOrNull = previewGreenDevice()

        onProgress.value = true

        viewModelScope.launch {
            delay(3_000)
            onProgress.value = false
            genuineState.value = JadeGenuineCheckViewModel.GenuineState.NOT_GENUINE
            delay(3_000)
            genuineState.value = JadeGenuineCheckViewModel.GenuineState.CANCELLED
        }
    }

    companion object {
        fun preview() =
            JadeGenuineCheckViewModelPreview()
    }
}