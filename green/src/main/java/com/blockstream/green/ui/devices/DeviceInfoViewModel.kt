package com.blockstream.green.ui.devices

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.blockstream.common.data.DeviceIdentifier
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.devices.DeviceConnectionManager
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.devices.HardwareConnectInteraction
import com.blockstream.green.gdk.getWallet
import com.blockstream.green.utils.QATester
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.blockstream.common.utils.Loggable
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class DeviceInfoViewModel constructor(
    deviceManager: DeviceManagerAndroid,
    qaTester: QATester,
    val gdk: Gdk,
    val wally: Wally,
    bluetoothAdapter: BluetoothAdapter,
    @InjectedParam override val device: GreenDevice,
) : AbstractDeviceViewModel(deviceManager, qaTester, null), HardwareConnectInteraction {

    // Don't use onProgress for this screen as is not turned off for animation reasons
    val navigationLock = MutableLiveData(false)

    val jadeIsUninitialized = MutableLiveData(false)

    val deviceIsConnected = MutableLiveData(false)

    val deviceState = device.deviceState.asLiveData()

    override val deviceConnectionManagerOrNull = DeviceConnectionManager(
        gdk = gdk,
        wally = wally,
        scope = viewModelScope.coroutineScope,
        applicationScope = applicationScope,
        bluetoothAdapter = bluetoothAdapter,
        httpRequestHandler = sessionManager.httpRequestHandler,
        interaction = this
    )

    init {
        if(device.gdkHardwareWallet == null){
            connectDevice()
        }

        bootstrap()
    }

    private fun connectDevice() {
        onProgress.value = true
        navigationLock.value = true
        deviceConnectionManager.connectDevice(device)
        countly.hardwareConnect(device)
    }

    fun authenticateAndContinue(jadeFirmwareManager: JadeFirmwareManager? = null){
        val gdkHardwareWallet = device.gdkHardwareWallet ?: return

        doAsync({
            // Authenticate device if needed
            deviceConnectionManager.authenticateDeviceIfNeeded(gdkHardwareWallet = gdkHardwareWallet, jadeFirmwareManager = jadeFirmwareManager)

            val network = deviceConnectionManager.getOperatingNetwork(gdkHardwareWallet)
            val isEphemeral = !settingsManager.appSettings.rememberHardwareDevices

            val previousSession = (if(device.isLedger){
                sessionManager.getDeviceSessionForNetworkAllPolicies(device, network, isEphemeral)
            }else{
                sessionManager.getDeviceSessionForEnvironment(device, network.isTestnet, isEphemeral)
            })

            if(previousSession != null){
                // Session already setup
                previousSession.getWallet(database, sessionManager)?.also {
                    return@doAsync it
                }
            }

            var session = sessionManager.getOnBoardingSession().also {
                // Disconnect any previous hww connection
                it.disconnect()
            }

            val walletHashId = getWalletHashId(session, network, device)
            // Disable Jade wallet fingerprint, keep the device name // getWalletName(session, network, device)
            val walletName = device.name

            val wallet: GreenWallet
            if (isEphemeral) {
                wallet = GreenWallet.createEphemeralWallet(
                    networkId = network.id,
                    name = walletName,
                    isHardware = true,
                    isTestnet = network.isTestnet
                ).also {
                    session.setEphemeralWallet(it)
                }

                sessionManager.upgradeOnBoardingSessionToWallet(wallet)
            } else {

                var isNewWallet = false

                // Persist wallet and device identifier
                wallet = database.getWalletWithXpubHashId(
                    xPubHashId = walletHashId,
                    isTestnet = network.isTestnet,
                    isHardware = true
                )?.let {
                    if(device.isLedger){
                        // Change network based on user applet
                        it.activeNetwork = network.id
                    }
                    it
                } ?: run {
                    isNewWallet = true
                    GreenWallet.createWallet(
                        xPubHashId = walletHashId,
                        name = walletName,
                        activeNetwork = network.id,
                        activeAccount = 0,
                        isHardware = true,
                        isTestnet = network.isTestnet,
                    )
                }

                val combinedLoginCredentials = (wallet.wallet.device_identifiers ?: listOf()) +
                        listOf(
                            DeviceIdentifier(
                                name = device.name,
                                uniqueIdentifier = device.uniqueIdentifier,
                                brand = device.deviceBrand,
                                connectionType = device.type
                            )
                        ).toSet().toList() // Make it unique

                wallet.deviceIdentifiers = combinedLoginCredentials

                if(isNewWallet){
                    database.insertWallet(wallet)
                }else{
                    database.updateWallet(wallet)
                }

                session = sessionManager.getWalletSessionOrCreate(wallet)

                countly.importWallet(session)
            }
            
            wallet
        }, postAction = {
            navigationLock.value = false
            onProgress.value = it == null
        }, onSuccess = { wallet: GreenWallet ->
            proceedToLogin = true
            postSideEffect(SideEffects.Navigate(wallet))
        })
    }

    override fun onDeviceReady(device: GreenDevice, isJadeUninitialized: Boolean?) {

        if (deviceConnectionManager.needsAndroid14BleUpdate) {
            postSideEffect(SideEffects.OpenDialog(0))
        }

        onProgress.value = false
        navigationLock.postValue(false)
        deviceIsConnected.postValue(true)
        countly.hardwareConnected(device)
        jadeIsUninitialized.postValue(isJadeUninitialized == true)
    }

    override fun onDeviceFailed(device: GreenDevice) {
        super.onDeviceFailed(device)
        postSideEffect(SideEffects.NavigateBack())
        navigationLock.postValue(false)
    }

    companion object : Loggable() {
        const val REQUIRE_REBONDING = "REQUIRE_REBONDING"
    }
}