package com.blockstream.green.ui.devices

import android.annotation.SuppressLint
import android.content.Context
import androidx.arch.core.util.Function
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.blockstream.DeviceBrand
import com.blockstream.gdk.GdkBridge
import com.blockstream.green.data.AppEvent
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.devices.HardwareConnect
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.AppViewModel
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import com.blockstream.green.utils.isDevelopmentFlavor
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mu.KLogging

class DeviceInfoViewModel @AssistedInject constructor(
    val sessionManager: SessionManager,
    val deviceManager: DeviceManager,
    val qaTester: QATester,
    val gdkBridge: GdkBridge,
    countly: Countly,
    @SuppressLint("StaticFieldLeak")
    @Assisted val applicationContext: Context,
    @Assisted val device: Device,
    @Assisted val wallet: Wallet?
) : AppViewModel(countly), HardwareConnectInteraction {

    sealed class DeviceInfoEvent : AppEvent {
        data class RequestPin(val deviceBrand: DeviceBrand) : DeviceInfoEvent()
        data class AskForFirmwareUpgrade(
            val request: FirmwareUpgradeRequest,
            val callback: Function<Int?, Void>
        ) : DeviceInfoEvent()
    }

    val rememberDevice = MutableLiveData(false)

    var requestPinEmitter: CompletableDeferred<String>? = null

    private val hardwareConnect = HardwareConnect(qaTester, isDevelopmentFlavor)

    val error = MutableLiveData<ConsumableEvent<String>>()
    val instructions = MutableLiveData<ConsumableEvent<Int>>()

    var session = sessionManager.getOnBoardingSession()

    val deviceState = device.deviceState.asLiveData()

    init {
        if(wallet != null){
            connectDeviceToNetwork(wallet.activeNetwork)
        }
    }

    fun connectDeviceToNetwork(network: String) {
        // Pause BLE scanning as can make unstable the connection to a ble device
        deviceManager.pauseBluetoothScanning()

        // Device is unlocked
        if (device.hwWallet != null) {
            if (device.isLedger) {
                // Ledger only operates on a single network
                sessionManager.getDeviceSessionForNetworkAllPolicies(device, gdkBridge.networks.getNetworkById(network))
            } else {
                sessionManager.getDeviceSessionForEnviroment(device, gdkBridge.networks.getNetworkById(network).isTestnet)
            }?.also {
                switchSessions(it)
            } ?: run {
                // This is needed only if the device can operate on mainnet/testnet simultaneously
                connectOnNetwork(network)
            }

        } else {
            unlockDeviceAndConnect(network)
        }
    }

    private fun switchSessions(session: GdkSession){
        onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(session.ephemeralWallet)))
    }

    private fun connectOnNetwork(network: String){
        session = sessionManager.getOnBoardingSession()

        doUserAction({
            session.disconnect()
            session.ephemeralWallet = wallet ?: Wallet.createEphemeralWallet(
                ephemeralId = 0,
                networkId = network,
                name = device.name,
                isHardware = true,
                isTestnet = session.networks.getNetworkById(network).isTestnet
            )
        }, onSuccess = {
            onDeviceReady()
        })
    }

    private fun unlockDeviceAndConnect(network: String){
        session = sessionManager.getOnBoardingSession()

        doUserAction({
            // Disconnect any previous hww connection
            session.disconnect()
            session.ephemeralWallet = wallet ?: Wallet.createEphemeralWallet(
                ephemeralId = 0,
                networkId = network,
                name = device.name,
                isHardware = true,
                isTestnet = session.networks.getNetworkById(network).isTestnet
            )
            hardwareConnect.connectDevice(this, session, device)
        }, postAction = null, onSuccess = {

        })
    }

    override fun onCleared() {
        super.onCleared()
        hardwareConnect.onDestroy()
    }

    override fun context() = applicationContext

    override fun showInstructions(resId: Int) {
        logger.info { "Show instructions $resId" }
        instructions.postValue(ConsumableEvent(resId))
    }

    override fun getGreenSession(): GdkSession {
        return session
    }

    override fun onJadeInitialization(session: GdkSession) {
        countly.jadeInitialize(session)
    }

    override fun getConnectionNetwork() = getGreenSession().prominentNetwork(getGreenSession().ephemeralWallet!!)

    override fun showError(err: String) {
        logger.info { "Shown error $err" }
        error.postValue(ConsumableEvent(err))
    }

    override fun onDeviceReady() {
        onProgress.postValue(false)

        session.ephemeralWallet?.let {
            sessionManager.upgradeOnBoardingSessionToWallet(it)
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
        }

        countly.hardwareConnect(device)
    }

    override fun onDeviceFailed() {
        onProgress.postValue(false)
    }

    override fun askForFirmwareUpgrade(
        firmwareUpgradeRequest: FirmwareUpgradeRequest,
        callback: Function<Int?, Void>
    ) {
        onEvent.postValue(ConsumableEvent(DeviceInfoEvent.AskForFirmwareUpgrade(firmwareUpgradeRequest, callback)))
    }

    override fun firmwareUpdated(requireReconnection: Boolean, requireBleRebonding: Boolean) {
        if(requireBleRebonding){
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(REQUIRE_REBONDING)))
        }else if(requireReconnection) {
            // on firmware update, navigate to device list
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateBack()))
        }
    }

    override fun requestPin(deviceBrand: DeviceBrand): CompletableDeferred<String> {
        onEvent.postValue(ConsumableEvent(DeviceInfoEvent.RequestPin(deviceBrand)))
        return CompletableDeferred<String>().also {
            requestPinEmitter = it
        }
    }

    override fun requestPinBlocking(deviceBrand: DeviceBrand): String {
        return requestPin(deviceBrand).let {
            runBlocking { it.await() }
        }
    }

    override fun getAntiExfilCorruptionForMessageSign() = qaTester.getAntiExfilCorruptionForMessageSign()
    override fun getAntiExfilCorruptionForTxSign() = qaTester.getAntiExfilCorruptionForTxSign()
    override fun getFirmwareCorruption() = qaTester.getFirmwareCorruption()

    fun setJadeFirmwareManager(jadeFirmwareManager: JadeFirmwareManager) {
        hardwareConnect.setJadeFirmwareManager(jadeFirmwareManager)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            applicationContext: Context,
            device: Device,
            wallet: Wallet?
        ): DeviceInfoViewModel
    }

    companion object : KLogging() {
        const val REQUIRE_REBONDING = "REQUIRE_REBONDING"

        fun provideFactory(
            assistedFactory: AssistedFactory,
            applicationContext: Context,
            device: Device,
            wallet: Wallet?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(applicationContext, device, wallet) as T
            }
        }
    }
}