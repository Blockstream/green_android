package com.blockstream.green.ui.devices

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.ConnectionType
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.devices.toAndroidDevice
import com.blockstream.green.utils.QATester
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class DeviceScanViewModel(
    deviceManager: DeviceManagerAndroid,
    qaTester: QATester,
    gdk: Gdk,
    wally: Wally,
    bluetoothAdapter: BluetoothAdapter,
    @InjectedParam wallet: GreenWallet
) : AbstractDeviceViewModel(deviceManager, qaTester, wallet) {
    val wallet get() = walletOrNull!!

    val hasBleConnectivity = wallet.deviceIdentifiers?.any { it.connectionType == ConnectionType.BLUETOOTH } ?: false

    var requestUserActionEmitter: CompletableDeferred<Boolean>? = null

    private val _deviceLiveData = MutableLiveData<GreenDevice?>(null)
    val deviceLiveData: LiveData<GreenDevice?> get() = _deviceLiveData
    override var device: GreenDevice?
        get() = deviceLiveData.value
        set(value) {
            _deviceLiveData.postValue(value)
        }

    override val deviceConnectionManagerOrNull = DeviceConnectionManager(
        gdk = gdk,
        wally = wally,
        scope = viewModelScope.coroutineScope,
        applicationScope = applicationScope,
        bluetoothAdapter = bluetoothAdapter,
        httpRequestHandler = sessionManager.httpRequestHandler,
        interaction = this, // this leaks, we need a fix
    )

    init {
        session.device.takeIf { session.isConnected }?.also { device ->
            postSideEffect(SideEffects.Navigate(wallet to device))
        } ?: run {
            deviceManager.devices.onEach { devices ->
                var foundDevice = devices.firstOrNull { device ->
                    wallet.deviceIdentifiers?.any { it.uniqueIdentifier == device.uniqueIdentifier } == true
                }

                if(device == null) {

                    // Find a BLE device or request a usb authentication
                    foundDevice = foundDevice ?: devices.firstOrNull {
                        it.toAndroidDevice()?.needsUsbPermissionsToIdentify() == true
                    }

                    if(foundDevice != null){
                        if(foundDevice.isBle) {
                            // Found device, pause ble scanning to increase connectivity success
                            deviceManager.stopBluetoothScanning()
                        }

                        device = foundDevice
                        selectDevice(foundDevice)
                    }
                }
            }.launchIn(viewModelScope.coroutineScope)
        }
    }

    private fun selectDevice(device: GreenDevice) {
        if (device.gdkHardwareWallet != null) {
            // Device is unlocked
            onDeviceReady(device, null)
        } else if (device.hasPermissions()) {
            doAsync({
                session.disconnect()
                deviceConnectionManager.connectDevice(device)
            }, postAction = null, onSuccess = {
                countly.hardwareConnect(device)
            })
        } else {
            askForPermission(device)
        }
    }

    private fun askForPermission(device: GreenDevice) {
        device.toAndroidDevice()?.askForPermission(onError = {
            onDeviceFailed(device)
        }, onSuccess = {
            // Check again if it's valid (after authentication we can get the usb serial id
            if(wallet.deviceIdentifiers?.any { it.uniqueIdentifier == device.uniqueIdentifier } == true){
                selectDevice(device)
            }else{
                onDeviceFailed(device)
            }
        })
    }

    override fun onDeviceFailed(device: GreenDevice) {
        super.onDeviceFailed(device)
        this.device = null
        (deviceManager as DeviceManagerAndroid).startBluetoothScanning()
    }

    override fun onDeviceReady(device: GreenDevice, isJadeUninitialized: Boolean?) {

        if (deviceConnectionManager.needsAndroid14BleUpdate) {
            postSideEffect(SideEffects.OpenDialog(0))
            return
        }

        doAsync({
            val gdkHardwareWallet = device.gdkHardwareWallet ?: throw Exception("Not HWWallet initiated")

            deviceConnectionManager.authenticateDeviceIfNeeded(gdkHardwareWallet)

            val network = deviceConnectionManager.getOperatingNetworkForEnviroment(gdkHardwareWallet, wallet.isTestnet)

            if(wallet.isTestnet != network.isTestnet){
                throw Exception("The device is operating on the wrong Environment")
            }

            if(device.isLedger){
                // Change network based on user applet
                wallet.activeNetwork = network.id
            }
// Disable Wallet Hash ID checking until we can have a more UX friendly experience
//            // Check wallet hash id
//            val walletHashId = getWalletHashId(session, network, device)
//
//            if (wallet.walletHashId.isNotBlank() && wallet.walletHashId != walletHashId) {
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
//                    if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.id == device.id }){
//                        device.disconnect()
//                    }
//                    throw Exception("id_action_canceled")
//                }
//            }else{
//                wallet to device
//            }

            wallet to device

        }, onError = {
            onDeviceFailed(device)
            postSideEffect(SideEffects.ErrorSnackbar(it))
        }, onSuccess = {
            proceedToLogin = true
            postSideEffect(SideEffects.Navigate(it))
            countly.hardwareConnected(device)
        })
    }
}