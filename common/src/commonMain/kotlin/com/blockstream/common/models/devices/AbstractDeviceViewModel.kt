package com.blockstream.common.models.devices

import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.common.interfaces.DeviceConnectionInterface
import com.blockstream.common.managers.BluetoothState
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.StringHolder
import com.blockstream.jade.firmware.FirmwareUpdateState
import com.blockstream.jade.firmware.FirmwareUpgradeRequest
import com.blockstream.jade.firmware.HardwareQATester
import com.rickclephas.kmp.observableviewmodel.InternalKMPObservableViewModelApi
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

abstract class AbstractDeviceViewModel constructor(
    greenWallet: GreenWallet? = null
) : GreenViewModel(greenWalletOrNull = greenWallet), HardwareConnectInteraction {

    open var deviceOrNull: GreenDevice? = null

    val device: GreenDevice
        get() = deviceOrNull!!

    protected var disconnectDeviceOnCleared: Boolean = true

    override val isLoginRequired: Boolean = false

    val deviceConnectionManager: DeviceConnectionInterface by inject()
    val gdk: Gdk by inject()
    val wally: Wally by inject()
    val deviceManager: DeviceManager by inject()
    val qaTester: HardwareQATester by inject()

    class LocalEvents {
        object Refresh: Event
        object EnableBluetooth: Events.EventSideEffect(SideEffects.EnableBluetooth)
        object AskForBluetoothPermissions: Events.EventSideEffect(SideEffects.AskForBluetoothPermissions)
        object EnableLocationService: Events.EventSideEffect(SideEffects.EnableLocationService)
        object LocationServiceMoreInfo: Events.OpenBrowser(Urls.BLUETOOTH_PERMISSIONS)
        object Troubleshoot: Events.OpenBrowser(Urls.JADE_TROUBLESHOOT)
        data class RespondToFirmwareUpgrade(val index: Int? = null): Event
    }

    class LocalSideEffects {
        data class RequestPin(val deviceBrand: DeviceBrand) : SideEffect
        data class AskForFirmwareUpgrade(val request: FirmwareUpgradeRequest) : SideEffect
        object RequestWalletIsDifferent : SideEffect
    }

    var isDevelopment: Boolean = appInfo.isDevelopmentOrDebug

    var requestPinEmitter: CompletableDeferred<String>? = null

    private var askForFirmwareUpgradeEmitter: CompletableDeferred<Int?>? = null

    var requestNetworkEmitter: CompletableDeferred<Network?>? = null

    val firmwareState = MutableStateFlow<SideEffect?>(null)

    val bluetoothState = (if(isPreview) flowOf(BluetoothState.ON) else deviceManager.bluetoothState).map {
        it
    }.stateIn(
        viewModelScope = viewModelScope,
        SharingStarted.WhileSubscribed(),
        BluetoothState.UNAVAILABLE
    )


    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        println("SATODEBUG AbstractDeviceViewModel handleEvent() event: $event")

        if(event is LocalEvents.Refresh) {
            deviceManager.refreshDevices()
        }else if (event is LocalEvents.RespondToFirmwareUpgrade){
            askForFirmwareUpgradeEmitter?.complete(event.index)
        }
    }

    internal fun askForPermissions(device: GreenDevice, navigateTo: SideEffects.NavigateTo){
        device.askForUsbPermission(onSuccess = {
            postSideEffect(navigateTo)
        }, onError = { error ->
            error?.also {
                postSideEffect(SideEffects.ErrorSnackbar(it))
            }
        })
    }

    suspend fun getWalletHashId(session: GdkSession, network: Network, device: GreenDevice): String {
        return session.getWalletIdentifier(
            network = network, // xPub generation is network agnostic
            gdkHwWallet = device.gdkHardwareWallet,
            hwInteraction = this
        ).xpubHashId
    }

    override fun showError(err: String) {
        postSideEffect(SideEffects.ErrorSnackbar(Exception(err)))
    }

    override fun showInstructions(text: String) {
        postSideEffect(SideEffects.Snackbar(StringHolder.create(text)))
    }

    override fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest
    ): Deferred<Int?> {
        return CompletableDeferred<Int?>().also {
            askForFirmwareUpgradeEmitter = it
            postSideEffect(LocalSideEffects.AskForFirmwareUpgrade(firmwareUpgradeRequest))
        }
    }

    override fun interactionRequest(
        gdkHardwareWallet: GdkHardwareWallet,
        message: String?,
        isMasterBlindingKeyRequest: Boolean,
        completable: CompletableDeferred<Boolean>?
    ) {
        postSideEffect(
            SideEffects.DeviceInteraction(
                deviceId = deviceOrNull?.connectionIdentifier,
                message = message,
                isMasterBlindingKeyRequest = isMasterBlindingKeyRequest,
                completable = completable
            )
        )
    }

    override fun firmwareUpdateState(state: FirmwareUpdateState) {
        device.updateFirmwareState(state)

        when(state) {
            is FirmwareUpdateState.Initiate -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.JadeFirmwareUpdate(deviceId = device.connectionIdentifier)))
                countly.jadeOtaStart(
                    device = device,
                    config = state.firmwareFileData.image.config,
                    isDelta = state.firmwareFileData.image.patchSize != null,
                    version = state.firmwareFileData.image.version
                )
            }
            is FirmwareUpdateState.Uploading -> {

            }
            is FirmwareUpdateState.Uploaded -> {
                logger.i { "firmwareComplete: ${state.success}" }

                if(state.success) {
                    deviceOrNull?.also {
                        countly.jadeOtaComplete(
                            device = it,
                            config = state.firmwareFileData.image.config,
                            isDelta = state.firmwareFileData.image.patchSize != null,
                            version = state.firmwareFileData.image.version
                        )
                    }
                }
            }
            is FirmwareUpdateState.Failed -> {
                logger.i { "firmwareFailed: userCancelled ${state.userCancelled}" }

                deviceOrNull?.also {
                    if(state.userCancelled) {
                        countly.jadeOtaRefuse(
                            device = it,
                            state.firmwareFileData.image.config,
                            state.firmwareFileData.image.patchSize != null,
                            state.firmwareFileData.image.version
                        )
                    }else{
                        countly.jadeOtaFailed(
                            device = it,
                            error = state.error,
                            state.firmwareFileData.image.config,
                            state.firmwareFileData.image.patchSize != null,
                            state.firmwareFileData.image.version
                        )
                    }
                }
            }
            is FirmwareUpdateState.Completed -> {
                if (state.requireBleRebonding) {
                    postSideEffect(SideEffects.BleRequireRebonding)
                } else if (state.requireReconnection) {
                    // on firmware update, navigate to device list
                    postSideEffect(SideEffects.NavigateBack())
                }
            }
        }
    }

    override fun requestPinBlocking(deviceBrand: DeviceBrand): String {
        logger.i { "AbstractDeviceViewModel requestPinBlocking deviceBrand: ${deviceBrand}" }
        postSideEffect(LocalSideEffects.RequestPin(deviceBrand))

        return CompletableDeferred<String>().also {
            logger.i { "AbstractDeviceViewModel requestPinBlocking requestPinEmitter before" }
            requestPinEmitter = it
            logger.i { "AbstractDeviceViewModel requestPinBlocking requestPinEmitter after" }
        }.let {
            logger.i { "AbstractDeviceViewModel requestPinBlocking runBlocking before" }
            runBlocking { it.await() }

        }
    }

    override fun getAntiExfilCorruptionForMessageSign() = qaTester.getAntiExfilCorruptionForMessageSign()
    override fun getAntiExfilCorruptionForTxSign() = qaTester.getAntiExfilCorruptionForTxSign()
    override fun getFirmwareCorruption() = qaTester.getFirmwareCorruption()

    override fun requestNetwork(): Network? = greenWalletOrNull?.let {
        if (it.isMainnet) gdk.networks().bitcoinElectrum else gdk.networks().testnetBitcoinElectrum
    } ?: if (settingsManager.appSettings.testnet) {
        logger.i { "AbstractDeviceViewModel requestNetwork " }
        requestNetworkEmitter = CompletableDeferred()
        postSideEffect(SideEffects.SelectEnvironment)
        runBlocking { requestNetworkEmitter!!.await() }
    } else {
        gdk.networks().bitcoinElectrum
    }

    // Called from Android ViewModelX
    override fun onCleared() {
        super.onCleared()

        if(disconnectDeviceOnCleared) {
            if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.connectionIdentifier == deviceOrNull?.connectionIdentifier }){
                // Disconnect without blocking the UI
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    deviceOrNull?.disconnect()
                }
            }
        }
    }

    // This is called from Voyager
    @OptIn(InternalKMPObservableViewModelApi::class)
    override fun onDispose() {
        super.onDispose()
        // Call the internal InternalKMPObservableViewModelApi
        clear()
    }

    companion object : Loggable()
 }