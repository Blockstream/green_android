package com.blockstream.common.models.devices

import androidx.lifecycle.viewModelScope
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
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
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.sideeffects.SideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

abstract class AbstractDeviceViewModel constructor(
    greenWalletOrNull: GreenWallet? = null
) : GreenViewModel(greenWalletOrNull = greenWalletOrNull), HardwareConnectInteraction {

    protected var disconnectDeviceOnCleared: Boolean = true

    override val isLoginRequired: Boolean = false

    val deviceConnectionManager: DeviceConnectionInterface by inject()
    val gdk: Gdk by inject()
    val wally: Wally by inject()
    val deviceManager: DeviceManager by inject()

    class LocalEvents {
        object Refresh : Event
        object EnableBluetooth : Events.EventSideEffect(SideEffects.EnableBluetooth)
        object AskForBluetoothPermissions : Events.EventSideEffect(SideEffects.AskForBluetoothPermissions)
        object EnableLocationService : Events.EventSideEffect(SideEffects.EnableLocationService)
        object LocationServiceMoreInfo : Events.OpenBrowser(Urls.BLUETOOTH_PERMISSIONS)
        object Troubleshoot : Events.OpenBrowser(Urls.JADE_TROUBLESHOOT)

    }

    class LocalSideEffects {
        data class RequestPin(val deviceBrand: DeviceBrand) : SideEffect
        object RequestWalletIsDifferent : SideEffect
    }

    var isDevelopment: Boolean = appInfo.isDevelopmentOrDebug

    var requestPinEmitter: CompletableDeferred<String>? = null

    var requestNetworkEmitter: CompletableDeferred<Network?>? = null

    val firmwareState = MutableStateFlow<SideEffect?>(null)

    val bluetoothState = (if (isPreview) flowOf(BluetoothState.ON) else deviceManager.bluetoothState).map {
        it
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        BluetoothState.UNAVAILABLE
    )

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.Refresh) {
            deviceManager.refreshDevices()
        }
    }

    internal fun askForPermissions(device: GreenDevice, navigateTo: SideEffects.NavigateTo) {
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

    override fun interactionRequest(
        gdkHardwareWallet: GdkHardwareWallet,
        message: String?,
        isMasterBlindingKeyRequest: Boolean,
        completable: CompletableDeferred<Boolean>?
    ) {
        postSideEffect(
            SideEffects.RequestDeviceInteraction(
                deviceId = deviceOrNull?.connectionIdentifier,
                message = message,
                isMasterBlindingKeyRequest = isMasterBlindingKeyRequest,
                completable = completable
            )
        )
    }

    override fun requestPinBlocking(deviceBrand: DeviceBrand): String {
        postSideEffect(LocalSideEffects.RequestPin(deviceBrand))

        return CompletableDeferred<String>().also {
            requestPinEmitter = it
        }.let {
            runBlocking { it.await() }
        }
    }

    override fun requestNetwork(): Network? = greenWalletOrNull?.let {
        if (it.isMainnet) gdk.networks().bitcoinElectrum else gdk.networks().testnetBitcoinElectrum
    } ?: if (settingsManager.appSettings.testnet) {
        requestNetworkEmitter = CompletableDeferred()
        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Environment))
        runBlocking { requestNetworkEmitter!!.await() }
    } else {
        gdk.networks().bitcoinElectrum
    }

    // Called from Android ViewModelX
    override fun onCleared() {
        super.onCleared()

        if (disconnectDeviceOnCleared) {
            if (sessionManager.getConnectedHardwareWalletSessions()
                    .none { it.device?.connectionIdentifier == deviceOrNull?.connectionIdentifier }
            ) {
                // Disconnect without blocking the UI
                applicationScope.launch(context = Dispatchers.IO + logException(countly)) {
                    deviceOrNull?.disconnect()
                }
            }
        }
    }

    companion object : Loggable()
}