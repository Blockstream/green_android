package com.blockstream.compose.models.jade

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_get_watch_only_information_from
import blockstream_green.common.generated.resources.id_initiate_oracle_communication
import blockstream_green.common.generated.resources.id_jade_will_securely_create_and
import blockstream_green.common.generated.resources.id_psbt_saved_to_files
import blockstream_green.common.generated.resources.id_qr_pin_unlock
import blockstream_green.common.generated.resources.id_scan_qr_on_device
import blockstream_green.common.generated.resources.id_scan_qr_on_jade
import blockstream_green.common.generated.resources.id_scan_qr_with_device
import blockstream_green.common.generated.resources.id_scan_qr_with_jade
import blockstream_green.common.generated.resources.id_scan_your_xpub_on_jade
import blockstream_green.common.generated.resources.id_validate_pin_and_unlock
import blockstream_green.common.generated.resources.id_validate_the_transaction_details
import com.blockstream.data.Urls
import com.blockstream.data.data.AppConfig
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.ScanResult
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.extensions.logException
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.jade.HandshakeComplete
import com.blockstream.data.jade.HandshakeCompleteResponse
import com.blockstream.data.jade.HandshakeInit
import com.blockstream.data.jade.HandshakeInitResponse
import com.blockstream.data.jade.QrData
import com.blockstream.data.jade.QrDataResponse
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.looks.transaction.TransactionConfirmLook
import com.blockstream.compose.models.abstract.AbstractScannerViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.utils.Loggable
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject
import kotlin.io.encoding.Base64
import kotlin.time.Clock

@Serializable
sealed class JadeQrOperation {
    @Serializable
    data class Psbt(
        val psbt: String,
        val transactionConfirmLook: TransactionConfirmLook? = null,
        val askForJadeUnlock: Boolean
    ) : JadeQrOperation()

    @Serializable
    data object LightningMnemonicExport : JadeQrOperation()

    @Serializable
    data object ExportXpub : JadeQrOperation()

    @Serializable
    data object PinUnlock : JadeQrOperation()
}

data class StepInfo(
    val title: StringResource = Res.string.id_scan_qr_on_jade,
    val message: StringResource = Res.string.id_initiate_oracle_communication,
    val step: Int = 1,
    val isScan: Boolean = false
)

data class Scenario(
    val steps: List<StepInfo>,
    val showStepCounter: Boolean = true,
    val allowReset: Boolean = true
) {
    val isPinUnlock
        get() = this == JadeQRViewModel.Companion.PinUnlockScenarioQuatro || this == JadeQRViewModel.Companion.PinUnlockScenarioDuo
}

abstract class JadeQRViewModelAbstract(
    val operation: JadeQrOperation,
    val deviceModel: DeviceModel,
    greenWalletOrNull: GreenWallet? = null
) :
    AbstractScannerViewModel(isDecodeContinuous = true, greenWalletOrNull = greenWalletOrNull) {

    override fun screenName(): String = when (operation) {
        JadeQrOperation.ExportXpub -> "ExportXpub"
        JadeQrOperation.LightningMnemonicExport -> "ExportLightningKey"
        JadeQrOperation.PinUnlock -> "PinUnlock"
        is JadeQrOperation.Psbt -> "JadeQR"
    }
    abstract val stepInfo: StateFlow<StepInfo>
    abstract val urPart: StateFlow<String?>
    abstract val isLightTheme: StateFlow<Boolean>

    internal var _scenario = MutableStateFlow(scenarionForOperation())
    val scenario: StateFlow<Scenario> = _scenario

    internal fun scenarionForOperation() = when (operation) {
        JadeQrOperation.ExportXpub -> if (deviceModel.isJade) JadeQRViewModel.Companion.ExportXpubScenarioJade else JadeQRViewModel.Companion.ExportXpubScenarioGeneric
        JadeQrOperation.LightningMnemonicExport -> JadeQRViewModel.Companion.ExportLightningScenario
        JadeQrOperation.PinUnlock -> JadeQRViewModel.Companion.PinUnlockScenarioQuatro
        is JadeQrOperation.Psbt -> JadeQRViewModel.Companion.PsbtScenario
    }
}

@OptIn(ExperimentalStdlibApi::class)
class JadeQRViewModel(
    greenWalletOrNull: GreenWallet? = null,
    operation: JadeQrOperation,
    deviceModel: DeviceModel
) : JadeQRViewModelAbstract(
    operation = operation,
    deviceModel = deviceModel,
    greenWalletOrNull = greenWalletOrNull
) {
    private val appConfig: AppConfig by inject()

    private var _urParts = MutableStateFlow<List<String>?>(null)

    private var _urPartIndex = 0
    private val _urPart: MutableStateFlow<String?> = MutableStateFlow(null)
    override val urPart: StateFlow<String?> = _urPart.asStateFlow()

    private var _step = MutableStateFlow(0)

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

        data class ExportPsbt(val saveToDevice: Boolean) : Event
        data object ImportPsbt : Event
    }

    init {
        stepInfo.onEach {
            _isLightTheme.value = !it.isScan

            // Reset scanner if it is a scan step
            if (it.isScan) {
                resetScanner()
            }
        }.launchIn(this)

        combine(scenario, _step) { scenario, step ->
            _navData.value = NavData(
                title = getString(if (deviceModel.isJade) (if (scenario.isPinUnlock) Res.string.id_qr_pin_unlock else Res.string.id_scan_qr_with_jade) else Res.string.id_scan_qr_with_device),
                backHandlerEnabled = true,
                onBackClicked = {
                    postEvent(Events.NavigateBackUserAction)
                }
            )
        }.launchIn(this)

        _urParts.onEach { parts ->
            _job?.cancel()

            _urPartIndex = 0

            _urPart.value = parts?.firstOrNull()

            _isValid.value = false

            _job = viewModelScope.launch(context = logException(countly)) {
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

        _step.value = 0
        _stepInfo.value = scenario.value.steps.first()

        when (operation) {
            JadeQrOperation.LightningMnemonicExport -> prepareBip8539Request()
            is JadeQrOperation.Psbt -> preparePsbtRequest()
            else -> {

            }
        }

        bootstrap()

        if ((operation as? JadeQrOperation.Psbt)?.askForJadeUnlock == true) {
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

    private fun previousStep() {
        if (_step.value > 0) {
            _step.value--
            _stepInfo.value = scenario.value.steps[_step.value]
        }
    }

    private fun nextStep() {
        _step.value++

        if (_step.value < scenario.value.steps.size) {
            _stepInfo.value = scenario.value.steps[_step.value]
        } else {
            postSideEffect(SideEffects.Success(true))
            postSideEffect(SideEffects.NavigateBack())
        }
    }

    private fun exportPsbt(saveToDevice: Boolean) {
        doAsync({
            // Convert it to v0 for better compatibility
            val psbt = session.psbtToV0((operation as JadeQrOperation.Psbt).psbt)

            val filename = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).format(LocalDateTime.Format {
                year(padding = Padding.ZERO)
                char('_')
                monthNumber(padding = Padding.ZERO)
                char('_')
                day(padding = Padding.ZERO)
                char('_')
                hour(padding = Padding.ZERO)
                minute(padding = Padding.ZERO)
            }).let { "tx_$it" }

            val file = if (saveToDevice) {
                FileKit.openFileSaver(suggestedName = filename, extension = "psbt") ?: throw Exception("id_action_canceled")
            } else {
                PlatformFile(FileKit.cacheDir, "$filename.psbt")
            }

            file.write(psbt.encodeToByteArray())

            if (saveToDevice) {
                postSideEffect(SideEffects.Snackbar(text = StringHolder.create(Res.string.id_psbt_saved_to_files)))
            } else {
                postSideEffect(SideEffects.ShareFile(file = file))
            }
        }, onSuccess = {
            _isValid.value = true
        })
    }

    private fun importPsbt() {
        doAsync({
            val file = FileKit.openFilePicker(
                mode = FileKitMode.Single,
                type = FileKitType.File(listOf("psbt"))
            )

            file?.let {
                withContext(context = Dispatchers.IO) {
                    // 500 KB
                    if (file.size() >= 500_000) {
                        throw Exception("File too big")
                    }

                    val psbt = file.readBytes()

                    // In binary format
                    if (session.psbtIsBinary(psbt)) {
                        Base64.Default.encode(psbt)
                    } else {
                        // In Base64 format (Jade)
                        // Remove all non-printable characters
                        psbt
                            .decodeToString()
                            .replace("\r\n", "")
                            .replace("\n", "")
                            .replace("\r", "")
                            .takeIf { session.psbtIsBase64(it) } ?: throw Exception("Not a valid PSBT")
                    }
                }
            }

        }, onSuccess = { psbt: String? ->
            psbt?.also {
                postSideEffect(SideEffects.Success(it))
                postSideEffect(SideEffects.NavigateBack())
            }
        })
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        when (event) {
            is Events.NavigateBackUserAction -> {
                if (_step.value > 0) {
                    previousStep()
                } else {
                    postSideEffect(SideEffects.NavigateBack())
                }
            }

            is Events.Continue -> {
                nextStep()
            }

            is LocalEvents.ExportPsbt -> {
                exportPsbt(saveToDevice = event.saveToDevice)
            }

            is LocalEvents.ImportPsbt -> {
                importPsbt()
            }

            is LocalEvents.CheckTransactionDetails -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.DeviceInteraction(
                            greenWalletOrNull = greenWalletOrNull,
                            deviceId = null,
                            transactionConfirmLook = (operation as? JadeQrOperation.Psbt)?.transactionConfirmLook
                        )
                    )
                )
            }

            is LocalEvents.PinUnlock -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.JadeQR(
                            greenWalletOrNull = greenWalletOrNull,
                            operation = JadeQrOperation.PinUnlock,
                            deviceModel = deviceModel
                        )
                    )
                )
            }
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

            is JadeQrOperation.PinUnlock -> {
                pinUnlock(scanResult)
            }
        }
    }

    private fun pinUnlock(scanResult: ScanResult) {
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
                // Delay resetScanner to prevent error dialog flooding
                viewModelScope.launch {
                    delay(3000L)
                    resetScanner()
                }

                throw Exception("QR code is not related to PIN Unlock")
            }

        }, onSuccess = {
            nextStep()
        })
    }

    private fun decryptLightningMnemonic(scanResult: ScanResult) {
        doAsync({
            val lightningMnemonic = session.jadeBip8539Reply(
                privateKey = _privateKey!!,
                publicKey = scanResult.bcur!!.publicΚey!!.hexToByteArray(),
                encrypted = scanResult.bcur!!.encrypted!!.hexToByteArray()
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
                    message = Res.string.id_validate_the_transaction_details,
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

class JadeQRViewModelPreview(
    operation: JadeQrOperation = JadeQrOperation.Psbt(
        psbt = "psbt",
        transactionConfirmLook = TransactionConfirmLook(),
        askForJadeUnlock = true
    )
) : JadeQRViewModelAbstract(
    operation = operation, deviceModel = DeviceModel.BlockstreamGeneric
) {
    override val stepInfo = MutableStateFlow(JadeQRViewModel.Companion.PsbtScenario.steps.first())
    override val urPart =
        MutableStateFlow("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")

    override fun setScanResult(scanResult: ScanResult) {

    }

    override val isLightTheme: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        var step = 0
        viewModelScope.launch {
            do {
                delay(2000L)
                if (step >= scenario.value.steps.size) {
                    step = 0
                }
                stepInfo.value = scenario.value.steps[step]
                step++
            } while (true)
        }

        stepInfo.onEach {
            isLightTheme.value = !it.isScan
        }.launchIn(this)
    }

    companion object : Loggable() {
        fun preview() = JadeQRViewModelPreview()
        fun previewLightning() = JadeQRViewModelPreview(operation = JadeQrOperation.LightningMnemonicExport)
    }
}


