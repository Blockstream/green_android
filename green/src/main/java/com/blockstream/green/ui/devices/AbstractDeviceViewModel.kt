package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import com.blockstream.HwWalletLogin
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.managers.BluetoothState
import com.blockstream.common.managers.DeviceManager
import com.blockstream.common.managers.toAndroidBluetoothState
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.common.utils.Loggable
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.utils.QATester
import com.greenaddress.greenbits.wallets.FirmwareFileData
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class AbstractDeviceViewModel constructor(
    val deviceManager: DeviceManager,
    val qaTester: QATester,
    val walletOrNull: GreenWallet?
) : GreenViewModel(), HardwareConnectInteraction, HwWalletLogin {

    protected var proceedToLogin: Boolean = false

    var requestNetworkEmitter: CompletableDeferred<Network?>? = null

    val firmwareState = MutableStateFlow<SideEffect?>(null)

    class LocalSideEffects {
        data class RequestPin(val deviceBrand: DeviceBrand) : SideEffect
        object RequestNetwork: SideEffect
        data class AskForFirmwareUpgrade(
            val request: FirmwareUpgradeRequest
        ) : SideEffect

        data class FirmwarePushedToDevice(val firmwareFileData: FirmwareFileData, val hash: String) : SideEffect

        data class FirmwareUpdateProgress(val written: Int, val totalSize: Int): SideEffect

        data class FirmwareUpdateComplete(val success: Boolean): SideEffect

        object RequestWalletIsDifferent : SideEffect
    }

    abstract val deviceConnectionManagerOrNull : DeviceConnectionManager?
    abstract val device: GreenDevice?

    val deviceConnectionManager : DeviceConnectionManager
        get() = deviceConnectionManagerOrNull!!

    val bluetoothState = (deviceManager as DeviceManagerAndroid).bluetoothState.map {
        it.toAndroidBluetoothState()
    }.stateIn(
        viewModelScope = viewModelScope,
        SharingStarted.WhileSubscribed(),
        BluetoothState.Unavailable
    )

    val onInstructions = MutableLiveData<ConsumableEvent<Int>>()

    var requestPinEmitter: CompletableDeferred<String>? = null

    var askForFirmwareUpgradeEmitter: CompletableDeferred<Int?>? = null

    fun getWalletHashId(session: GdkSession, network: Network, device: GreenDevice): String {
        return session.getWalletIdentifier(
            network = network, // xPub generation is network agnostic
            gdkHwWallet = device.gdkHardwareWallet,
            hwInteraction = this
        ).xpubHashId
    }

    override fun showError(err: String) {
        postSideEffect(SideEffects.ErrorSnackbar(Exception(err)))
    }

    override fun showInstructions(resId: Int) {
        logger.i { "Show instructions $resId" }
        onInstructions.postValue(ConsumableEvent(resId))
    }

    override fun onDeviceReady(device: GreenDevice, isJadeUninitialized: Boolean?) {
        onProgress.value = true
    }

    override fun onDeviceFailed(device: GreenDevice) {
        onProgress.value = false
    }

    override fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest
    ): Deferred<Int?> {
        return CompletableDeferred<Int?>().also {
            askForFirmwareUpgradeEmitter = it
            postSideEffect(LocalSideEffects.AskForFirmwareUpgrade(firmwareUpgradeRequest))
        }
    }

    override fun firmwareUpdated(requireReconnection: Boolean, requireBleRebonding: Boolean) {
        if(requireBleRebonding){
            postSideEffect(SideEffects.Navigate(DeviceInfoViewModel.REQUIRE_REBONDING))
        }else if(requireReconnection) {
            // on firmware update, navigate to device list
            postSideEffect(SideEffects.NavigateBack())
        }
    }

    override fun requestPinBlocking(deviceBrand: DeviceBrand): String {
        postSideEffect(LocalSideEffects.RequestPin(deviceBrand))

        return CompletableDeferred<String>().also {
            requestPinEmitter = it
        }.let {
            runBlocking { it.await() }
        }
    }

    override fun getAntiExfilCorruptionForMessageSign() = qaTester.getAntiExfilCorruptionForMessageSign()
    override fun getAntiExfilCorruptionForTxSign() = qaTester.getAntiExfilCorruptionForTxSign()
    override fun getFirmwareCorruption() = qaTester.getFirmwareCorruption()

    override fun requestNetwork(): Network? {
        requestNetworkEmitter = CompletableDeferred()

        postSideEffect(LocalSideEffects.RequestNetwork)

        return runBlocking { requestNetworkEmitter!!.await() }
    }

    override fun firmwarePushedToDevice(firmwareFileData: FirmwareFileData, hash: String) {
        postSideEffect(LocalSideEffects.FirmwarePushedToDevice(firmwareFileData, hash))
        device?.also {
            countly.jadeOtaStart(it, firmwareFileData.image.config, firmwareFileData.image.patchSize != null, firmwareFileData.image.version)
        }
    }

    override fun firmwareProgress(written: Int, totalSize: Int) {
        postSideEffect(LocalSideEffects.FirmwareUpdateProgress(written, totalSize))
    }

    override fun firmwareFailed(userCancelled: Boolean, error: String, firmwareFileData: FirmwareFileData) {
        logger.i { "firmwareFailed: userCancelled $userCancelled" }

        device?.also {
            if(userCancelled) {
                countly.jadeOtaRefuse(
                    device = it,
                    firmwareFileData.image.config,
                    firmwareFileData.image.patchSize != null,
                    firmwareFileData.image.version
                )
            }else{
                countly.jadeOtaFailed(
                    device = it,
                    error = error,
                    firmwareFileData.image.config,
                    firmwareFileData.image.patchSize != null,
                    firmwareFileData.image.version
                )
            }
        }
    }

    override fun firmwareComplete(success: Boolean, firmwareFileData: FirmwareFileData) {
        logger.i { "firmwareComplete: $success" }

        postSideEffect(LocalSideEffects.FirmwareUpdateComplete(success))

        if(success) {
            device?.also {
                countly.jadeOtaComplete(it, firmwareFileData.image.config, firmwareFileData.image.patchSize != null, firmwareFileData.image.version)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // deviceConnectionManagerOrNull?.onDestroy()

        if(!proceedToLogin) {

            if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.connectionIdentifier == device?.connectionIdentifier }){
                // Disconnect without blocking the UI
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    device?.disconnect()
                }
            }
        }
    }

    companion object: Loggable()
}