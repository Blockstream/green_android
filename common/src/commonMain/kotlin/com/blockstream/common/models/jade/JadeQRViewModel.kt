package com.blockstream.common.models.jade

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrows_counter_clockwise
import blockstream_green.common.generated.resources.id_get_watch_only_information_from
import blockstream_green.common.generated.resources.id_initiate_oracle_communication
import blockstream_green.common.generated.resources.id_jade_will_securely_create_and
import blockstream_green.common.generated.resources.id_qr_pin_unlock
import blockstream_green.common.generated.resources.id_reset
import blockstream_green.common.generated.resources.id_scan_qr_on_device
import blockstream_green.common.generated.resources.id_scan_qr_on_jade
import blockstream_green.common.generated.resources.id_scan_qr_with_device
import blockstream_green.common.generated.resources.id_scan_qr_with_jade
import blockstream_green.common.generated.resources.id_scan_your_xpub_on_jade
import blockstream_green.common.generated.resources.id_validate_pin_and_unlock
import blockstream_green.common.generated.resources.id_validate_the_transaction_details
import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.data.ScanResult
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.jade.HandshakeComplete
import com.blockstream.common.jade.HandshakeCompleteResponse
import com.blockstream.common.jade.HandshakeInit
import com.blockstream.common.jade.HandshakeInitResponse
import com.blockstream.common.jade.QrData
import com.blockstream.common.jade.QrDataResponse
import com.blockstream.common.looks.transaction.TransactionConfirmLook
import com.blockstream.common.models.abstract.AbstractScannerViewModel
import com.blockstream.common.models.jade.JadeQRViewModel.Companion.PinUnlockScenarioDuo
import com.blockstream.common.models.jade.JadeQRViewModel.Companion.PinUnlockScenarioQuatro
import com.blockstream.common.models.jade.JadeQRViewModel.Companion.PsbtScenario
import com.blockstream.common.navigation.NavigateDestinations
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

@Parcelize
sealed class JadeQrOperation(open val askForJadeUnlock: Boolean = false) : Parcelable, JavaSerializable {
    class Psbt(
        val psbt: String,
        val transactionConfirmLook: TransactionConfirmLook? = null,
        override val askForJadeUnlock: Boolean
    ) : JadeQrOperation()
    data object LightningMnemonicExport : JadeQrOperation()
    data object ExportXpub : JadeQrOperation()
    data object PinUnlock : JadeQrOperation()
}

data class StepInfo(
    val title: StringResource = Res.string.id_scan_qr_on_jade,
    val message: StringResource = Res.string.id_initiate_oracle_communication,
    val step: Int? = 1,
    val isScan: Boolean = false
)

data class Scenario(
    val steps: List<StepInfo>,
    val showStepCounter: Boolean = true,
    val allowReset: Boolean = true
) {
    val isPinUnlock
        get() = this == PinUnlockScenarioQuatro || this == PinUnlockScenarioDuo
}

abstract class JadeQRViewModelAbstract(
    val operation: JadeQrOperation,
    val deviceBrand: DeviceBrand,
    greenWalletOrNull: GreenWallet? = null
) :
    AbstractScannerViewModel(isDecodeContinuous = true, greenWalletOrNull = greenWalletOrNull) {

    override fun screenName(): String = when (operation) {
        JadeQrOperation.ExportXpub -> "ExportXpub"
        JadeQrOperation.LightningMnemonicExport -> "ExportLightningKey"
        JadeQrOperation.PinUnlock -> "PinUnlock"
        is JadeQrOperation.Psbt -> "JadeQR"
    }

    @NativeCoroutinesState
    abstract val stepInfo: StateFlow<StepInfo>

    @NativeCoroutinesState
    abstract val urPart: StateFlow<String?>

    abstract val scenario: StateFlow<Scenario>

    @NativeCoroutinesState
    abstract val isLightTheme: StateFlow<Boolean>
}

@OptIn(ExperimentalStdlibApi::class)
class JadeQRViewModel(
    greenWalletOrNull: GreenWallet? = null,
    operation: JadeQrOperation,
    deviceBrand: DeviceBrand
) : JadeQRViewModelAbstract(
    operation = operation,
    deviceBrand = deviceBrand,
    greenWalletOrNull = greenWalletOrNull
) {
    private var _urParts = MutableStateFlow<List<String>?>(null)

    private var _urPartIndex = 0
    private val _urPart: MutableStateFlow<String?> = MutableStateFlow(null)
    override val urPart: StateFlow<String?> = _urPart.asStateFlow()

    private var _step = 0

    private var _scenario = MutableStateFlow(scenarionForOperation())
    override val scenario = _scenario

    private val _stepInfo: MutableStateFlow<StepInfo> =
        MutableStateFlow(scenario.value.steps.first())
    override val stepInfo: StateFlow<StepInfo> = _stepInfo

    private var _privateKey: ByteArray? = null

    private var _job: Job? = null

    private val _isLightTheme = MutableStateFlow(false)
    override val isLightTheme: StateFlow<Boolean> = _isLightTheme

    class LocalEvents {
        object ClickTroubleshoot : Events.OpenBrowser(Urls.HELP_QR_PIN_UNLOCK)
        object CheckTransactionDetails : Event
        object PinUnlock : Event
    }

    init {
        stepInfo.onEach {
            _isLightTheme.value = !it.isScan

            // Reset scanner if it is a scan step
            if (it.isScan) {
                resetScanner()
            }
        }.launchIn(this)

        scenario.onEach { scenario ->
            _navData.value = NavData(
                title = getString(if (deviceBrand.isJade) (if (scenario.isPinUnlock) Res.string.id_qr_pin_unlock else Res.string.id_scan_qr_with_jade) else Res.string.id_scan_qr_with_device),
                actions = listOfNotNull(NavAction(
                    title = getString(Res.string.id_reset),
                    icon = Res.drawable.arrows_counter_clockwise,
                    isMenuEntry = false,
                    onClick = {
                        restart()
                    }
                ).takeIf { scenario.allowReset })
            )
        }.launchIn(this)

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

        if (operation.askForJadeUnlock) {
            viewModelScope.launch {
                delay(400L)
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.AskJadeUnlock(
                            isOnboarding = false
                        )
                    )
                )
            }
        }
    }

    private fun scenarionForOperation() = when (operation) {
        JadeQrOperation.ExportXpub -> if (deviceBrand.isJade) ExportXpubScenarioJade else ExportXpubScenarioGeneric
        JadeQrOperation.LightningMnemonicExport -> ExportLightningScenario
        JadeQrOperation.PinUnlock -> PinUnlockScenarioQuatro
        is JadeQrOperation.Psbt -> PsbtScenario
    }

    private fun restart() {
        _step = 0
        _stepInfo.value = scenario.value.steps.first()

        when (operation) {
            JadeQrOperation.LightningMnemonicExport -> prepareBip8539Request()
            is JadeQrOperation.Psbt -> preparePsbtRequest()
            else -> {

            }
        }
    }

    private fun preparePsbtRequest() {
        doAsync({
            session.jadePsbtRequest((operation as JadeQrOperation.Psbt).psbt)
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
            postSideEffect(SideEffects.Success(true))
            postSideEffect(SideEffects.NavigateBack())
        }
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            nextStep()
        } else if (event is LocalEvents.CheckTransactionDetails) {
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.DeviceInteraction(
                        transactionConfirmLook = (operation as? JadeQrOperation.Psbt)?.transactionConfirmLook
                    )
                )
            )
        } else if (event is LocalEvents.PinUnlock) {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.JadeQR(operation = JadeQrOperation.PinUnlock)))
        }
    }

    override fun setScanResult(scanResult: ScanResult) {
        logger.d { "scanResult: $scanResult" }

        when (operation) {
            JadeQrOperation.LightningMnemonicExport -> {
                decryptLightningMnemonic(scanResult)
            }

            is JadeQrOperation.Psbt -> {
                postSideEffect(SideEffects.Success(scanResult.result))
                postSideEffect(SideEffects.NavigateBack())
            }

            is JadeQrOperation.ExportXpub -> {
                postSideEffect(SideEffects.Success(scanResult.result))
                postSideEffect(SideEffects.NavigateBack())
            }

            else -> {
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
                    step = 2,
                    isScan = true
                ),
            ), showStepCounter = false
        )

        val PinUnlockScenarioQuatro = Scenario(
            steps = listOf(
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
            ), allowReset = false
        )

        val PinUnlockScenarioDuo = Scenario(
            steps = listOf(
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
            ), allowReset = false
        )

        val PsbtScenario = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_with_jade,
                    message = Res.string.id_validate_the_transaction_details,
                    step = 1,
                    isScan = false
                ),
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_validate_pin_and_unlock,
                    step = 2,
                    isScan = true
                ),
            ), showStepCounter = true
        )

        val ExportXpubScenarioJade = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_on_jade,
                    message = Res.string.id_scan_your_xpub_on_jade,
                    isScan = true
                ),
            ), showStepCounter = false
        )

        val ExportXpubScenarioGeneric = Scenario(
            listOf(
                StepInfo(
                    title = Res.string.id_scan_qr_on_device,
                    message = Res.string.id_get_watch_only_information_from,
                    isScan = true
                ),
            ), showStepCounter = false
        )
    }
}

class JadeQRViewModelPreview() : JadeQRViewModelAbstract(
    operation = JadeQrOperation.Psbt(
        psbt = "psbt",
        transactionConfirmLook = TransactionConfirmLook(),
        askForJadeUnlock = true
    ), deviceBrand = DeviceBrand.Blockstream
) {
    override val stepInfo = MutableStateFlow(PsbtScenario.steps.first())
    override val urPart =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")

    override val scenario = MutableStateFlow(PsbtScenario)

    override fun setScanResult(scanResult: ScanResult) {

    }

    override val isLightTheme: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        var step = 0
//        viewModelScope.launch {
//            do{
//                delay(2000L)
//                if(step >= scenario.value.steps.size){
//                    step = 0
//                }
//                stepInfo.value = scenario.value.steps[step]
//                step++
//            }while (true)
//        }

        stepInfo.onEach {
            isLightTheme.value = !it.isScan
        }.launchIn(this)
    }

    companion object {
        fun preview() = JadeQRViewModelPreview()
    }
}


