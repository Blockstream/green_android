package com.blockstream.common.models.jade

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrows_counter_clockwise
import blockstream_green.common.generated.resources.id_initiate_oracle_communication
import blockstream_green.common.generated.resources.id_jade_will_securely_create_and
import blockstream_green.common.generated.resources.id_reset
import blockstream_green.common.generated.resources.id_scan_qr_on_jade
import blockstream_green.common.generated.resources.id_scan_qr_with_jade
import blockstream_green.common.generated.resources.id_validate_pin_and_unlock
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
import com.blockstream.common.gdk.params.BcurEncodeParams
import com.blockstream.common.jade.HandshakeComplete
import com.blockstream.common.jade.HandshakeCompleteResponse
import com.blockstream.common.jade.HandshakeInit
import com.blockstream.common.jade.HandshakeInitResponse
import com.blockstream.common.jade.QrData
import com.blockstream.common.jade.QrDataResponse
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import com.blockstream.common.models.jade.JadeQRViewModel.Companion.PinUnlockScenarioDuo
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
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
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

data class StepInfo(
    val title: StringResource = Res.string.id_scan_qr_on_jade,
    val message: StringResource = Res.string.id_initiate_oracle_communication,
    val step: Int? = 1,
    val isScan: Boolean = false
)

data class Scenario(val steps: List<StepInfo>, val showStepCounter: Boolean = true)

abstract class JadeQRViewModelAbstract(
    val psbt: String? = null,
    val isLightningMnemonicExport: Boolean = false,
    greenWalletOrNull: GreenWallet? = null
) :
    AbstractScannerViewModel(isDecodeContinuous = true, greenWalletOrNull = greenWalletOrNull) {
    override fun screenName(): String =
        if (isLightningMnemonicExport) "ExportLightningKey" else "JadeQR"

    @NativeCoroutinesState
    abstract val stepInfo: StateFlow<StepInfo>

    @NativeCoroutinesState
    abstract val urPart: StateFlow<String?>

    abstract val scenario: StateFlow<Scenario>
}

@OptIn(ExperimentalStdlibApi::class)
class JadeQRViewModel(
    greenWalletOrNull: GreenWallet? = null,
    psbt: String? = null,
    isLightningMnemonicExport: Boolean = false
) : JadeQRViewModelAbstract(
    psbt = psbt,
    isLightningMnemonicExport = isLightningMnemonicExport,
    greenWalletOrNull = greenWalletOrNull
) {
    private val isPsbt = psbt != null
    private var _urParts = MutableStateFlow<List<String>?>(null)

    private var _urPartIndex = 0
    private val _urPart: MutableStateFlow<String?> = MutableStateFlow(null)
    override val urPart: StateFlow<String?> = _urPart.asStateFlow()

    private var _step = 0

    private var _scenario =
        MutableStateFlow(if (isPsbt) PsbtScenario else if (isLightningMnemonicExport) ExportLightningScenario else PinUnlockScenarioQuatro)
    override val scenario = _scenario.asStateFlow()

    private val _stepInfo: MutableStateFlow<StepInfo> =
        MutableStateFlow(scenario.value.steps.first())
    override val stepInfo: StateFlow<StepInfo> = _stepInfo.asStateFlow()

    private var _privateKey: ByteArray? = null

    private var _job: Job? = null

    class LocalEvents {
        object ClickTroubleshoot : Events.OpenBrowser(Urls.HELP_QR_PIN_UNLOCK)
    }

    class LocalSideEffects {
        object ScanQr : SideEffect
    }

    init {

        viewModelScope.launch {
            _navData.value = NavData(
                actions = listOf(NavAction(
                    title = getString(Res.string.id_reset),
                    icon = Res.drawable.arrows_counter_clockwise,
                    isMenuEntry = false,
                    onClick = {
                        restart()
                    }
                ))
            )
        }

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
                        }

                        logger.d { "Displaying $_urPartIndex/${parts.size}" }

                        // If half are displayed
                        if (_urPartIndex >= (parts.size / 2)) {
                            _isValid.value = true
                        }

                        _urPart.value = parts[_urPartIndex]
                        _urPartIndex++
                        delay(500L)
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

    private fun restart() {
        _step = 0
        _stepInfo.value = scenario.value.steps.first()

        if (isPsbt) {
            preparePsbtRequest()
        } else if (isLightningMnemonicExport) {
            prepareBip8539Request()
        }
    }

    private fun preparePsbtRequest() {
        doAsync({
            session.jadePsbtRequest(psbt ?: "")
        }, onSuccess = {
            _urParts.value = it.parts
        })
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

        if (_step < scenario.value.steps.size) {
            _stepInfo.value = scenario.value.steps[_step]
        } else {
            postSideEffect(SideEffects.NavigateBack())
        }
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            nextStep()
        }
    }

    override fun setScanResult(scanResult: ScanResult) {
        resetScanner()

        if(isPsbt) {
            postSideEffect(SideEffects.Success(scanResult.result))
            postSideEffect(SideEffects.NavigateBack())
        } else if (isLightningMnemonicExport) {
            decryptLightningMnemonic(scanResult)
        } else {
            doAsync({
                scanResult.bcur?.result?.httpRequest?.also { request ->
                    logger.d { "Request: $request" }
                    val httpResponse = session.httpRequest(request.params)

                    httpResponse.jsonObject["body"]?.let {
                        if (request.isHandshakeInit) {
                            GreenJson.json.decodeFromJsonElement<HandshakeInit>(it)
                                .let { HandshakeInitResponse(params = it) }.toCborHex()
                        } else if (request.isHandshakeComplete) {
                            GreenJson.json.decodeFromJsonElement<HandshakeComplete>(it)
                                .let { HandshakeCompleteResponse(params = it) }.toCborHex()
                        } else {
                            // Set scenario to 2-step pin unlock
                            _scenario.value = PinUnlockScenarioDuo
                            GreenJson.json.decodeFromJsonElement<QrData>(it)
                                .let { QrDataResponse(method = request.onReply, params = it) }
                                .toCborHex()
                        }
                    }?.also {
                        _urParts.value = session.jadePinRequest(it).parts
                    } ?: run {
                        throw Exception(httpResponse.jsonObject["error"]?.jsonPrimitive?.content)
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
                publicKey = scanResult.bcur!!.publicΚey!!.hexToByteArray(),
                encrypted = scanResult.bcur.encrypted!!.hexToByteArray()
            )
            lightningMnemonic ?: throw Exception("id_decoding_error_try_again_by_scanning")
        }, onSuccess = { lightningMnemonic: String ->
            postSideEffect(SideEffects.Mnemonic(lightningMnemonic))
            postSideEffect(SideEffects.NavigateBack())
        })
    }

    companion object : Loggable() {
        val ExportLightningScenario = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_with_jade,
                    message = Res.string.id_jade_will_securely_create_and,
                    step = 1,
                    isScan = false
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_jade_will_securely_create_and,
                    step = 1,
                    isScan = true
                ),
            ), showStepCounter = false
        )

        val PinUnlockScenarioQuatro = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_initiate_oracle_communication,
                    step = 1,
                    isScan = true
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_with_jade,
                    message = Res.string.id_validate_pin_and_unlock,
                    step = 2,
                    isScan = false
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_initiate_oracle_communication,
                    step = 3,
                    isScan = true
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_with_jade,
                    message = Res.string.id_validate_pin_and_unlock,
                    step = 4,
                    isScan = false
                )
            )
        )

        val PinUnlockScenarioDuo = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_initiate_oracle_communication,
                    step = 1,
                    isScan = true
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_with_jade,
                    message = Res.string.id_validate_pin_and_unlock,
                    step = 2,
                    isScan = false
                )
            )
        )

        val PsbtScenario = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_with_jade,
                    message = Res.string.id_scan_qr_with_jade,
                    step = 1,
                    isScan = false
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_scan_qr_on_jade,
                    step = 1,
                    isScan = true
                ),
            ), showStepCounter = true
        )
    }
}

class JadeQRViewModelPreview() : JadeQRViewModelAbstract() {
    override val stepInfo: StateFlow<StepInfo> =
        MutableStateFlow(PinUnlockScenarioDuo.steps.first())
    override val urPart: StateFlow<String?> =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")

    override val scenario = MutableStateFlow(PinUnlockScenarioDuo)

    override fun setScanResult(scanResult: ScanResult) {

    }

    companion object {
        fun preview() = JadeQRViewModelPreview()
    }
}


