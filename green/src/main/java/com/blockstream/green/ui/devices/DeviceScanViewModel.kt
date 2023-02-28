package com.blockstream.green.ui.devices

import android.content.Context
import android.os.Build
import androidx.lifecycle.*
import com.blockstream.gdk.GdkBridge
import com.blockstream.green.data.Countly
import com.blockstream.green.data.NavigateEvent
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.Device
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.ConsumableEvent
import com.blockstream.green.utils.QATester
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DeviceScanViewModel @AssistedInject constructor(
    @ApplicationContext val context: Context,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    deviceManager: DeviceManager,
    qaTester: QATester,
    countly: Countly,
    gdkBridge: GdkBridge,
    settingsManager: SettingsManager,
    @Assisted wallet: Wallet
) : AbstractDeviceViewModel(sessionManager, walletRepository, deviceManager, qaTester, countly, wallet) {
    val wallet get() = walletOrNull!!

    val hasBleConnectivity = wallet.deviceIdentifiers?.any { it.connectionType == Device.ConnectionType.BLUETOOTH } ?: false
    val canEnableBluetooth = MutableLiveData(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

    var requestUserActionEmitter: CompletableDeferred<Boolean>? = null

    private val _deviceLiveData = MutableLiveData<Device?>(null)
    val deviceLiveData: LiveData<Device?> get() = _deviceLiveData
    override var device: Device?
        get() = deviceLiveData.value
        set(value) {
            _deviceLiveData.postValue(value)
        }

    override val deviceConnectionManagerOrNull = DeviceConnectionManager(
        makeDeviceReady = true,
        countly = countly,
        gdkBridge = gdkBridge,
        settingsManager = settingsManager,
        httpRequestProvider = sessionManager.httpRequestProvider,
        interaction = this,
        qaTester = qaTester
    )
    val session get() = sessionManager.getWalletSession(wallet)

    init {
        session.device.takeIf { session.isConnected }?.also { device ->
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(wallet to device)))
        } ?: run {
            deviceManager.devicesStateFlow.onEach { devices ->
                var foundDevice = devices.firstOrNull { device ->
                    wallet.deviceIdentifiers?.any { it.uniqueIdentifier == device.uniqueIdentifier } == true
                }

                if(device == null) {

                    // Find a BLE device or request a usb authentication
                    foundDevice = foundDevice ?: devices.firstOrNull {
                        it.needsUsbPermissionsToIdentify()
                    }

                    if(foundDevice != null){
                        if(foundDevice.isBle) {
                            // Found device, pause ble scanning to increase connectivity success
                            deviceManager.pauseBluetoothScanning()
                        }

                        device = foundDevice
                        selectDevice(foundDevice)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun selectDevice(device: Device) {
        if (device.hwWallet != null) {
            // Device is unlocked
            onDeviceReady(device, null)
        } else if (device.hasPermissionsOrIsBonded() || device.handleBondingByHwwImplementation()) {
            doUserAction({
                session.disconnect()
                deviceConnectionManager.connectDevice(
                    context,
                    device
                )
            }, postAction = null, onSuccess = {
                countly.hardwareConnect(device)
            })
        } else {
            askForPermissionOrBond(device)
        }
    }

    private fun askForPermissionOrBond(device: Device) {
        device.askForPermissionOrBond(onError = {
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

    override fun onDeviceFailed(device: Device) {
        super.onDeviceFailed(device)
        this.device = null
        deviceManager.startBluetoothScanning()
    }

    override fun onDeviceReady(device: Device, isJadeUninitialized: Boolean?) {

        doUserAction({
            val hwWallet = device.hwWallet ?: throw Exception("Not HWWallet initiated")

            deviceConnectionManager.authenticateDeviceIfNeeded(hwWallet)

            val network = deviceConnectionManager.getOperatingNetworkForEnviroment(hwWallet, wallet.isTestnet)

            if(wallet.isTestnet != network.isTestnet){
                throw Exception("The device is operating on the wrong Environment")
            }

            if(device.isLedger){
                // Change network based on user applet
                wallet.activeNetwork = network.id
            }

            // Check wallet hash id
            val walletHashId = getWalletHashId(session, network, device)

            if (wallet.walletHashId.isNotBlank() && wallet.walletHashId != walletHashId) {

                // Wallet has different hash id, ask user if he wants to continue
                val userAction = CompletableDeferred<Boolean>().also{
                    requestUserActionEmitter = it
                    onEvent.postValue(ConsumableEvent(DeviceScanFragment.DeviceScanFragmentEvent.RequestWalletIsDifferent))
                }

                if (userAction.await()) {
                    val onboardingSession = sessionManager.getOnBoardingSession()
                    val epheneralWallet = Wallet.createEphemeralWallet(
                        networkId = network.id,
                        name = getWalletName(session, network, device),
                        isHardware = true,
                        isTestnet = network.isTestnet
                    ).also {
                        onboardingSession.ephemeralWallet = it
                        sessionManager.upgradeOnBoardingSessionToWallet(it)
                    }

                    epheneralWallet to device
                } else {
                    // Disconnect only if there are no other connected sessions
                    if(sessionManager.getConnectedHardwareWalletSessions().none { it.device?.id == device.id }){
                        device.disconnect()
                    }
                    throw Exception("id_action_canceled")
                }
            }else{
                wallet to device
            }

        }, onError = {
            onDeviceFailed(device)
            onError.value = ConsumableEvent(it)
        }, onSuccess = {
            proceedToLogin = true
            onEvent.postValue(ConsumableEvent(NavigateEvent.NavigateWithData(it)))
            countly.hardwareConnected(device)
        })
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): DeviceScanViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}