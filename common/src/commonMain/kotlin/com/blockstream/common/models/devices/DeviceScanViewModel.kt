package com.blockstream.common.models.devices

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_troubleshoot
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavAction
import com.blockstream.common.data.NavData
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewGreenDevice
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.juul.kable.ConnectionLostException
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.getString

abstract class DeviceScanViewModelAbstract(greenWallet: GreenWallet) :
    AbstractDeviceViewModel(greenWallet = greenWallet) {
    override fun screenName(): String = "DeviceScan"


    @NativeCoroutinesState
    abstract val deviceFlow: StateFlow<GreenDevice?>
}

class DeviceScanViewModel(greenWallet: GreenWallet) :
    DeviceScanViewModelAbstract(greenWallet = greenWallet) {

    private val _deviceFlow: MutableStateFlow<GreenDevice?> = MutableStateFlow(null)
    override val deviceFlow: StateFlow<GreenDevice?> = _deviceFlow.asStateFlow()

    override var deviceOrNull: GreenDevice?
        get() = deviceFlow.value
        set(value) {
            _deviceFlow.value = value
        }

    init {
        println("SATODEBUG DeviceScanViewModel init")
        println("SATODEBUG DeviceScanViewModel init session: $session")
        println("SATODEBUG DeviceScanViewModel init session.device: ${session.device}")
        println("SATODEBUG DeviceScanViewModel init session.isConnected: ${session.isConnected}")
        session.device.takeIf { session.isConnected }?.also { device ->

            deviceManager.savedDevice = device
            println("SATODEBUG DeviceScanViewModel init deviceManager.savedDevice: ${deviceManager.savedDevice}")

            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.Login(
                        greenWallet = greenWallet,
                        deviceId = device.connectionIdentifier
                    )
                )
            )
            println("SATODEBUG DeviceScanViewModel init after postSideEffect")

        } ?: run {

            combine(deviceFlow, deviceManager.devices) { _, devices ->
                println("SATODEBUG DeviceScanViewModel init run combine devices: $devices")
                if(deviceFlow.value == null) {
                    var foundDevice = devices.firstOrNull { device ->
                        println("SATODEBUG DeviceScanViewModel init run combine step1 device: $device")
                        greenWallet.deviceIdentifiers?.any { it.uniqueIdentifier == device.uniqueIdentifier } == true
                    }
                    println("SATODEBUG DeviceScanViewModel init run combine step2 foundDevice: $foundDevice")

                    // Find a BLE device or request a usb authentication
                    foundDevice = foundDevice ?: devices.firstOrNull {
                        it.needsUsbPermissionsToIdentify()
                    }
                    println("SATODEBUG DeviceScanViewModel init run combine step3 foundDevice: $foundDevice")

                    // TODO if device is disconnected, do not rescan it!!!!

                    foundDevice?.also {
                        println("SATODEBUG DeviceScanViewModel init run combine step4 foundDevice: $foundDevice")
                        selectDevice(it)
                    }
                }
            }.launchIn(this)
        }

        viewModelScope.launch {
            _navData.value = NavData(
                title = greenWallet.name,actions = listOfNotNull(
                    NavAction(title = getString(Res.string.id_troubleshoot), onClick = {
                        postEvent(LocalEvents.Troubleshoot)
                    }),
                )
            )
        }

        bootstrap()
    }

    private fun selectDevice(device: GreenDevice) {
        println("SATODEBUG DeviceScanViewModel selectDevice() device: $device")
        _deviceFlow.value = device

        if (device.hasPermissions()) {

            doAsync({
                println("SATODEBUG DeviceScanViewModel selectDevice() doAsync START")
                if (device.gdkHardwareWallet == null) {
                    println("SATODEBUG DeviceScanViewModel selectDevice() doAsync device.gdkHardwareWallet is NULL")
                    session.disconnect()
                    deviceConnectionManager.connectDevice(
                        device,
                        httpRequestHandler = sessionManager.httpRequestHandler,
                        interaction = this
                    )
                    countly.hardwareConnect(device)
                }

                println("SATODEBUG DeviceScanViewModel selectDevice() doAsync device.gdkHardwareWallet: ${device.gdkHardwareWallet}")
                val gdkHardwareWallet = device.gdkHardwareWallet ?: throw Exception("Not HWWallet initiated")

                println("SATODEBUG DeviceScanViewModel selectDevice() doAsync BEFORE deviceConnectionManager.authenticateDeviceIfNeeded()")
                deviceConnectionManager.authenticateDeviceIfNeeded(httpRequestHandler = sessionManager.httpRequestHandler, interaction = this, gdkHardwareWallet = gdkHardwareWallet)
                println("SATODEBUG DeviceScanViewModel selectDevice() doAsync AFTER deviceConnectionManager.authenticateDeviceIfNeeded()")

                val network = device.getOperatingNetworkForEnviroment(device, gdk, greenWallet.isTestnet)
                    ?: throw Exception("No network is available")
                println("SATODEBUG DeviceScanViewModel selectDevice() doAsync network: $network")

                if(greenWallet.isTestnet != network.isTestnet){
                    throw Exception("The device is operating on the wrong Environment")
                }

                if(device.isLedger){
                    // Change network based on user applet
                    greenWallet.activeNetwork = network.id
                }

                val walletHashId = getWalletHashId(session, network, device)
                println("SATODEBUG DeviceScanViewModel selectDevice() doAsync walletHashId: $walletHashId")

                if (greenWallet.xPubHashId.isNotBlank() && greenWallet.xPubHashId != walletHashId) {
                    // Disconnect only if there are no other connected sessions
                    if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.connectionIdentifier == device.connectionIdentifier }){
                        device.disconnect()
                    }
                    throw Exception("The wallet hash is different from the previous wallet.")
                }

    //// Disable Wallet Hash ID checking until we can have a more UX friendly experience
    //            // Check wallet hash id
    //            val walletHashId = getWalletHashId(session, network, device)
    //
    //            if (greenWallet.xPubHashId.isNotBlank() && greenWallet.xPubHashId != walletHashId) {
    //
    //                // Wallet has different hash id, ask user if he wants to continue
    //                val userAction = CompletableDeferred<Boolean>().also{
    //                    requestUserActionEmitter = it
    //                    onEvent.postValue(ConsumableEvent(DeviceScanFragment.DeviceScanFragmentEvent.RequestWalletIsDifferent))
    //                }
    //
    //                if (userAction.await()) {
    //                    val onboardingSession = sessionManager.getOnBoardingSession()
    //                    val epheneralWallet = Wallet.createEphemeralWallet(
    //                        networkId = network.id,
    //                        name = getWalletName(session, network, device),
    //                        isHardware = true,
    //                        isTestnet = network.isTestnet
    //                    ).also {
    //                        onboardingSession.ephemeralWallet = it
    //                        sessionManager.upgradeOnBoardingSessionToWallet(it)
    //                    }
    //
    //                    epheneralWallet to device
    //                } else {
    //                    // Disconnect only if there are no other connected sessions
    //                    if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.connectionIdentifier == device.connectionIdentifier }){
    //                        device.disconnect()
    //                    }
    //                    throw Exception("id_action_canceled")
    //                }
    //            }else{
    //                greenWallet to device
    //            }

                greenWallet to device

            }, onError = {
                println("SATODEBUG DeviceScanViewModel selectDevice() onError : ${it}")
                if (it !is ConnectionLostException) {
                    postSideEffect(SideEffects.ErrorSnackbar(it))
                }
                _deviceFlow.value = null
            }, onSuccess = {
                println("SATODEBUG DeviceScanViewModel selectDevice() onSuccess : ${it}")
                disconnectDeviceOnCleared = false

                deviceManager.savedDevice = it.second

                println("SATODEBUG DeviceScanViewModel selectDevice() onSuccess BEFORE postSideEffect NavigateTo(NavigateDestinations.Login)")
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.Login(
                            greenWallet = it.first,
                            deviceId = it.second.connectionIdentifier
                        )
                    )
                )
                countly.hardwareConnected(device)
                println("SATODEBUG DeviceScanViewModel selectDevice() onSuccess END")
            })
        } else {
            askForPermission(device)
        }
    }

    private fun askForPermission(device: GreenDevice) {
        device.askForUsbPermission(onError = {
            logger.d { "askForUsbPermission failed ${it?.message}" }
            _deviceFlow.value = null
        }, onSuccess = {
            // Check again if it's valid (after authentication we can get the usb serial id
            if (greenWallet.deviceIdentifiers?.any { it.uniqueIdentifier == device.uniqueIdentifier } == true) {
                selectDevice(device)
            } else {
                // USB is not the same
                _deviceFlow.value = null
            }
        })
    }

    companion object: Loggable()
}


class DeviceScanViewModelPreview() : DeviceScanViewModelAbstract(greenWallet = previewWallet()) {

    override val deviceFlow: StateFlow<GreenDevice?> =  MutableStateFlow(previewGreenDevice())
    override var deviceOrNull: GreenDevice? = previewGreenDevice()

    companion object {
        fun preview() = DeviceScanViewModelPreview()
    }
}