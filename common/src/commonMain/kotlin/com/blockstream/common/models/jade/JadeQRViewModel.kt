package com.blockstream.common.models.jade

import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.jade.HandshakeComplete
import com.blockstream.common.jade.HandshakeCompleteResponse
import com.blockstream.common.jade.HandshakeInit
import com.blockstream.common.jade.HandshakeInitResponse
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import com.blockstream.common.models.jade.JadeQRViewModel.Companion.ExportLightningScenario
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

data class StepInfo(
    val title: String = "id_scan_qr_on_jade",
    val message: String = "id_initiate_oracle_communication",
    val step: Int = 1,
    val isScan: Boolean = false
) {
    val stepMessage = "id_step_s|$step"
}

data class Scenario(val steps: List<StepInfo>, val showStepCounter: Boolean = true)

abstract class JadeQRViewModelAbstract(val isLightningMnemonicExport: Boolean = false, greenWalletOrNull: GreenWallet? = null) :
    AbstractScannerViewModel(isDecodeContinuous = true, greenWalletOrNull = greenWalletOrNull) {
    override fun screenName(): String = if (isLightningMnemonicExport) "ExportLightningKey" else "JadeQR"

    @NativeCoroutinesState
    abstract val stepInfo: StateFlow<StepInfo>

    @NativeCoroutinesState
    abstract val urPart: StateFlow<String?>

    abstract val scenario: Scenario
}

@OptIn(ExperimentalStdlibApi::class)
class JadeQRViewModel(
    isLightningMnemonicExport: Boolean = false,
    greenWalletOrNull: GreenWallet? = null
) : JadeQRViewModelAbstract(
    isLightningMnemonicExport = isLightningMnemonicExport,
    greenWalletOrNull = greenWalletOrNull
) {
    private var _urParts = MutableStateFlow<List<String>?>(null)

    private var _urPartIndex = 0
    private val _urPart: MutableStateFlow<String?> = MutableStateFlow(null)
    override val urPart: StateFlow<String?> = _urPart.asStateFlow()

    private var _step = 0

    override val scenario: Scenario = if(isLightningMnemonicExport) ExportLightningScenario else PinUnlockScenario

    private val _stepInfo: MutableStateFlow<StepInfo> = MutableStateFlow(scenario.steps.first())
    override val stepInfo: StateFlow<StepInfo> = _stepInfo.asStateFlow()

    private var _privateKey: ByteArray? = null

    private var _job: Job? = null

    class LocalEvents{
        object ClickTroubleshoot: Events.OpenBrowser(Urls.HELP_QR_PIN_UNLOCK)
    }
    class LocalSideEffects {
        object ScanQr : SideEffect
    }

    init {

        _navData.value = NavData(
            actions = listOf(NavAction(
                title = "Restart",
                icon = "arrows_counter_clockwise",
                isMenuEntry = false,
                onClick = {
                    restart()
                }
            ))
        )

        _urParts.onEach { parts ->
            _job?.cancel()

            _urPartIndex = 0

            _urPart.value = parts?.firstOrNull()

            _isValid.value = false

            _job = viewModelScope.coroutineScope.launch(context = logException(countly)) {
                // Rotate qr codes
                if (parts != null && parts.size > 1) {
                    while (isActive) {
                        if (_urPartIndex >= parts.size) {
                            _urPartIndex = 0
                            _isValid.value = true
                        }
                        _urPart.value = parts[_urPartIndex]
                        _urPartIndex++
                        delay(1000L)
                    }
                } else {
                    delay(3000L)
                    _isValid.value = true
                }
            }

        }.launchIn(this)

        restart()

        bootstrap()
    }

    private fun restart(){
        _step = 0
        _stepInfo.value = scenario.steps.first()

        if(isLightningMnemonicExport) {
            prepareBip8539Request()
        }
    }

    private fun prepareBip8539Request() {
        doAsync({
            session.jadeBip8539Request()
        }, onSuccess = {
            _privateKey = it.first
            _urParts.value = it.second.parts
        })
    }

    private fun nextStep() {
        _step++

        if (_step < scenario.steps.size) {
            _stepInfo.value = scenario.steps[_step]
        } else {
            // Complete
            // TODO
        }
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            _urParts.value = null
            nextStep()
        }
    }

    override fun setScanResult(scanResult: ScanResult) {
        resetScanner()

        if (isLightningMnemonicExport) {
            decryptLightningMnemonic(scanResult)
        } else {
            doAsync({

                scanResult.bcur?.result?.httpRequest?.also { request ->
                    val httpResponse = session.httpRequest(request.params)

                    httpResponse.jsonObject["body"]?.let {
                        if (request.isHandshakeInit) {
                            GreenJson.json.decodeFromJsonElement<HandshakeInit>(it)
                                .let { HandshakeInitResponse(params = it) }.toCborHex()
                        } else {
                            GreenJson.json.decodeFromJsonElement<HandshakeComplete>(it)
                                .let { HandshakeCompleteResponse(params = it) }.toCborHex()
                        }
                    }?.also {
                        _urParts.value = session.jadePinRequest(it).parts
                    }
                } ?: run {
                    throw Exception("QR code is not related to PIN Unlock")
                }

            }, onSuccess = {
                nextStep()
            })
        }
    }

    private fun decryptLightningMnemonic(scanResult: ScanResult) {
        doAsync({
            val lightningMnemonic = session.jadeBip8539Reply(
                privateKey = _privateKey!!,
                publicKey =  scanResult.bcur!!.publicΚey!!.hexToByteArray(),
                encrypted = scanResult.bcur.encrypted!!.hexToByteArray()
            )
            lightningMnemonic ?: throw Exception("id_decoding_error_try_again_by_scanning")
        }, onSuccess = { lightningMnemonic: String ->
            postSideEffect(SideEffects.Mnemonic(lightningMnemonic))
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    companion object : Loggable() {
        val ExportLightningScenario = Scenario(listOf(
            StepInfo(
                title = "id_scan_qr_with_jade",
                message = "id_jade_will_securely_create_and_transfer",
                step = 1,
                isScan = false
            ),
            StepInfo(
                title = "id_scan_qr_on_jade",
                message = "id_jade_will_securely_create_and_transfer",
                step = 1,
                isScan = true
            ),
        ), showStepCounter = false)

        val PinUnlockScenario = Scenario(listOf(
            StepInfo(
                title = "id_scan_qr_on_jade",
                message = "id_initiate_oracle_communication",
                step = 1,
                isScan = true
            ),
            StepInfo(
                title = "id_scan_qr_with_jade",
                message = "id_validate_pin_and_unlock",
                step = 2,
                isScan = false
            ),
            StepInfo(
                title = "id_scan_qr_on_jade",
                message = "id_initiate_oracle_communication",
                step = 3,
                isScan = true
            ),
            StepInfo(
                title = "id_scan_qr_with_jade",
                message = "id_validate_pin_and_unlock",
                step = 4,
                isScan = false
            )
        ))
    }
}

class JadeQRViewModelPreview() : JadeQRViewModelAbstract() {
    override val stepInfo: StateFlow<StepInfo> = MutableStateFlow(StepInfo())
    override val urPart: StateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")

    override val scenario: Scenario = ExportLightningScenario

    override fun setScanResult(scanResult: ScanResult) {

    }

    companion object {
        fun preview() = JadeQRViewModelPreview()
    }
}


