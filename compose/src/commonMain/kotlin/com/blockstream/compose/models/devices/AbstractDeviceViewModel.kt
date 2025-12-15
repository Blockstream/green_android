package com.blockstream.compose.models.devices

import androidx.lifecycle.viewModelScope
import com.blockstream.data.Urls
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.devices.DeviceBrand
import com.blockstream.data.devices.GreenDevice
import com.blockstream.data.extensions.logException
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.Wally
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.device.GdkHardwareWallet
import com.blockstream.data.gdk.device.HardwareConnectInteraction
import com.blockstream.data.interfaces.DeviceConnectionInterface
import com.blockstream.data.managers.BluetoothState
import com.blockstream.data.managers.DeviceManager
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.utils.Loggable
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