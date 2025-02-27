package com.blockstream.common.models.devices

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_your_device_was_disconnected
import com.blockstream.common.SupportType
import com.blockstream.common.data.SupportData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.DeviceState
import com.blockstream.common.devices.JadeDevice
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewGreenDevice
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.events.JadeGenuineCheck
import com.blockstream.common.gdk.params.RsaVerifyParams
import com.blockstream.common.models.devices.JadeGenuineCheckViewModel.GenuineState
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.StringHolder
import com.blockstream.common.utils.randomChars
import com.blockstream.jade.data.JadeError
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach

abstract class JadeGenuineCheckViewModelAbstract(greenWallet: GreenWallet?) : AbstractDeviceViewModel(greenWallet = greenWallet) {
    override fun screenName(): String = "JadeGenuineCheck"

    @NativeCoroutinesState
    abstract val genuineState: StateFlow<GenuineState>
}

class JadeGenuineCheckViewModel constructor(greenWallet: GreenWallet?, deviceId: String?) : JadeGenuineCheckViewModelAbstract(greenWallet) {

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

        if(deviceOrNull == null){
            postSideEffect(SideEffects.NavigateBack())
        }else {
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

        when(event){
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
                    SideEffects.NavigateTo(NavigateDestinations.Support(
                        type = SupportType.INCIDENT,
                        supportData = SupportData(
                            subject = "Jade Genuine check failed",
                            zendeskHardwareWallet = deviceOrNull?.deviceModel?.zendeskValue
                        )
                    ))
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

    private fun genuineCheck(){
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

    companion object: Loggable()
}

class JadeGenuineCheckViewModelPreview : JadeGenuineCheckViewModelAbstract(previewWallet()) {
    override val genuineState: MutableStateFlow<GenuineState> = MutableStateFlow(GenuineState.CHECKING)

    init {
        deviceOrNull = previewGreenDevice()

         onProgress.value = true

        viewModelScope.launch {
            delay(3_000)
            onProgress.value = false
            genuineState.value = GenuineState.NOT_GENUINE
            delay(3_000)
            genuineState.value = GenuineState.CANCELLED
        }
    }

    companion object {
        fun preview() =
            JadeGenuineCheckViewModelPreview()
    }
}