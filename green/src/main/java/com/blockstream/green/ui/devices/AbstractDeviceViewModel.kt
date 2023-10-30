package com.blockstream.green.ui.devices

import androidx.lifecycle.MutableLiveData
import com.blockstream.HwWalletLogin
import com.blockstream.JadeHWWallet
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceBrand
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.ConsumableEvent
import com.blockstream.green.data.AppEvent
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.ui.AppViewModelAndroid
import com.blockstream.green.utils.QATester
import com.greenaddress.greenbits.wallets.FirmwareFileData
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging

abstract class AbstractDeviceViewModel constructor(
    val deviceManager: DeviceManager,
    val qaTester: QATester,
    val walletOrNull: GreenWallet?
) : AppViewModelAndroid(), HardwareConnectInteraction, HwWalletLogin {

    protected var proceedToLogin: Boolean = false

    var requestNetworkEmitter: CompletableDeferred<Network?>? = null
    sealed class DeviceEvent : AppEvent {
        data class RequestPin(val deviceBrand: DeviceBrand) : DeviceEvent()
        object RequestNetwork: DeviceEvent()
        data class AskForFirmwareUpgrade(
            val request: FirmwareUpgradeRequest
        ) : DeviceEvent()

        data class FirmwarePushedToDevice(val firmwareFileData: FirmwareFileData, val hash: String) : DeviceEvent()

        data class FirmwareUpdateProgress(val written: Int, val totalSize: Int): DeviceEvent()

        data class FirmwareUpdateComplete(val success: Boolean): DeviceEvent()
    }

    abstract val deviceConnectionManagerOrNull : DeviceConnectionManager?
    abstract val device: Device?

    val deviceConnectionManager : DeviceConnectionManager
        get() = deviceConnectionManagerOrNull!!

    val bleAdapterState = deviceManager.bleAdapterState

    val onInstructions = MutableLiveData<ConsumableEvent<Int>>()

    var requestPinEmitter: CompletableDeferred<String>? = null

    var askForFirmwareUpgradeEmitter: CompletableDeferred<Int?>? = null

    fun getWalletHashId(session: GdkSession, network: Network, device: Device): String {
        return session.getWalletIdentifier(
            network = network, // xPub generation is network agnostic
            gdkHwWallet = device.gdkHardwareWallet,
            hwInteraction = this
        ).walletHashId
    }

    fun getWalletName(session: GdkSession, network: Network, device: Device) = if (device.isJade) {
        session.getWalletFingerprint(
            network = network,
            gdkHwWallet = device.gdkHardwareWallet,
            hwInteraction = this
        )?.uppercase()?.let {
            "Wallet: $it"
        } ?: device.name
    } else {
        device.name
    }

    override fun showError(err: String) {
        onError.postValue(ConsumableEvent(Exception(err)))
    }

    override fun showInstructions(resId: Int) {
        logger.info { "Show instructions $resId" }
        onInstructions.postValue(ConsumableEvent(resId))
    }

    override fun onDeviceReady(device: Device, isJadeUninitialized: Boolean?) {
        onProgressAndroid.postValue(true)
    }

    override fun onDeviceFailed(device: Device) {
        onProgressAndroid.postValue(false)
    }

    override fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest
    ): Deferred<Int?> {
        return CompletableDeferred<Int?>().also {
            askForFirmwareUpgradeEmitter = it
            onEvent.postValue(ConsumableEvent(DeviceEvent.AskForFirmwareUpgrade(firmwareUpgradeRequest)))
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
        onEvent.postValue(ConsumableEvent(DeviceEvent.RequestPin(deviceBrand)))

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

        onEvent.postValue(ConsumableEvent(DeviceEvent.RequestNetwork))

        return runBlocking { requestNetworkEmitter!!.await() }
    }

    override fun firmwarePushedToDevice(firmwareFileData: FirmwareFileData, hash: String) {
        onEvent.postValue(ConsumableEvent(DeviceEvent.FirmwarePushedToDevice(firmwareFileData, hash)))
        device?.also {
            countly.jadeOtaStart(it, firmwareFileData.image.config, firmwareFileData.image.patchSize != null, firmwareFileData.image.version)
        }
    }

    override fun firmwareProgress(written: Int, totalSize: Int) {
        onEvent.postValue(ConsumableEvent(DeviceEvent.FirmwareUpdateProgress(written, totalSize)))
    }

    override fun firmwareComplete(success: Boolean, firmwareFileData: FirmwareFileData) {
        logger.info { "firmwareComplete: $success" }

        (device?.gdkHardwareWallet as? JadeHWWallet)?.also {
            it.updateFirmwareVersion(firmwareFileData.image.version)
        }

        onEvent.postValue(ConsumableEvent(DeviceEvent.FirmwareUpdateComplete(success)))
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

            if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.id == device?.id }){
                // Disconnect without blocking the UI
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    device?.disconnect()
                }
            }
        }
    }

    companion object: KLogging()
}