package com.blockstream.compose.managers

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import com.blockstream.common.data.AppInfo
import com.blockstream.common.devices.AndroidDevice
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.devices.toAndroidDevice
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.DeviceSupportsAntiExfilProtocol
import com.blockstream.common.gdk.data.DeviceSupportsLiquid
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.common.interfaces.ConnectionResult
import com.blockstream.compose.devices.LedgerDevice
import com.blockstream.compose.devices.SatochipDevice
import com.blockstream.compose.devices.TrezorDevice
import com.blockstream.jade.HttpRequestHandler
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.Loggable
import com.blockstream.jade.firmware.FirmwareUpgradeRequest
import com.blockstream.jade.fromUsb
import com.btchip.BTChipDongle
import com.btchip.BTChipException
import com.btchip.comm.BTChipTransport
import com.btchip.comm.android.BTChipTransportAndroid
import com.greenaddress.greenbits.wallets.BTChipHWWallet
import com.greenaddress.greenbits.wallets.LedgerBLEAdapter
import com.greenaddress.greenbits.wallets.SatochipHWWallet
import com.greenaddress.greenbits.wallets.TrezorHWWallet
import com.satoshilabs.trezor.Trezor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class DeviceConnectionManagerAndroid constructor(
    gdk: Gdk,
    wally: Wally,
    private val appInfo: AppInfo,
    scope: CoroutineScope,
    private val bluetoothAdapter: BluetoothAdapter
) : DeviceConnectionManager(
    gdk = gdk,
    wally = wally,
    scope = scope
) {

    override suspend fun connectDevice(
        device: GreenDevice,
        httpRequestHandler: HttpRequestHandler,
        interaction: HardwareConnectInteraction
    ): ConnectionResult {
        return ((device as? TrezorDevice)?.let {
            connectTrezorDevice(it, interaction)
        } ?: (device as? LedgerDevice)?.let {
            connectLedgerDevice(it, interaction)
        } ?: (device as? SatochipDevice)?.let {
            connectSatochipDevice(it, interaction) // SATODEBUG
        } ?: super.connectDevice(device, httpRequestHandler, interaction))

    }

    override suspend fun disconnectDevice(device: GreenDevice) {
        super.disconnectDevice(device)

        (device as? LedgerDevice)?.also {
            disconnectLedgerDevice(it)
        }
    }

    private suspend fun disconnectLedgerDevice(device: LedgerDevice) {
        try {
            device.transport?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun createJadeApi(
        device: GreenDevice,
        httpRequestHandler: HttpRequestHandler
    ): JadeAPI? {
        return device.toAndroidDevice()?.usbDevice?.let { usbDevice ->
            JadeAPI.fromUsb(
                usbDevice = usbDevice,
                usbManager = device.toAndroidDevice()!!.usbManager,
                httpRequestHandler = httpRequestHandler
            )
        } ?: super.createJadeApi(device, httpRequestHandler)
    }

    private suspend fun connectTrezorDevice(device: TrezorDevice, interaction: HardwareConnectInteraction): ConnectionResult {
        val trezor = Trezor.getDevice(device.usbManager, listOf(device.usbDevice!!))

        val version: List<Int> = trezor.firmwareVersion
        val firmware = version[0].toString() + "." + version[1] + "." + version[2]
        val vendorId: Int = trezor.vendorId

        logger.i { "Trezor Version: " + version + " vendorid:" + vendorId + " productid:" + trezor.productId }

        // Min allowed: v1.6.0 & v2.1.0
        val isFirmwareOutdated = version[0] < 1 ||
                version[0] == 1 && version[1] < 6 ||
                version[0] == 1 && version[1] == 6 && version[2] < 0 ||
                version[0] == 2 && version[1] < 1

        if (isFirmwareOutdated) {
            val isPositive = interaction.askForFirmwareUpgrade(
                FirmwareUpgradeRequest(
                    deviceBrand = DeviceBrand.Trezor.brand,
                    isUsb = true,
                    currentVersion = null,
                    upgradeVersion = null,
                    firmwareList = null,
                    hardwareVersion = null,
                    isUpgradeRequired = !appInfo.isDevelopmentOrDebug
                )
            ).await()

            if (isPositive != null) {
                onTrezorConnected(device, trezor, firmware)
            } else {
                throw Exception("Firmware version is not supported. Please update device.")
            }
        }

        return onTrezorConnected(device, trezor, firmware)
    }

    private fun onTrezorConnected(device: AndroidDevice, trezor: Trezor, firmwareVersion: String?): ConnectionResult {
        logger.d { "Creating Trezor HW wallet" }

        val trezorDevice = com.blockstream.common.gdk.data.Device(
            name = "Trezor",
            supportsArbitraryScripts = false,
            supportsLowR = false,
            supportsHostUnblinding = false,
            supportsExternalBlinding = false,
            supportsLiquid = DeviceSupportsLiquid.None,
            supportsAntiExfilProtocol = DeviceSupportsAntiExfilProtocol.None
        )

        device.gdkHardwareWallet = TrezorHWWallet(trezor, trezorDevice, firmwareVersion)

        return ConnectionResult()
    }
    
    private suspend fun connectSatochipDevice(device: SatochipDevice, interaction: HardwareConnectInteraction): ConnectionResult {
        logger.i {"SATODEBUG DeviceConnectionManagerAndroid connectSatochipDevice() start device: $device"}

        val satoDevice = com.blockstream.common.gdk.data.Device(
            name = "Satochip",
            supportsArbitraryScripts = false,
            supportsLowR = false,
            supportsHostUnblinding = false,
            supportsExternalBlinding = false,
            supportsLiquid = DeviceSupportsLiquid.None,
            supportsAntiExfilProtocol = DeviceSupportsAntiExfilProtocol.None
        )

        val pin: String? = null;
        //val pin: String? = satochipInteraction?.requestPassphrase(DeviceBrand.Satochip)
        println("SATODEBUG DeviceConnectionManagerAndroid onConnected(): PIN: " + pin)

        // provide activity and context needed for NFC
        val activity: Activity? = device.activityProvider?.getCurrentActivity()

        println("SATODEBUG DeviceConnectionManagerAndroid onConnected() creating gdkHardwareWallet")
        device.gdkHardwareWallet = SatochipHWWallet(satoDevice, pin, activity, device.context)
        println("SATODEBUG DeviceConnectionManagerAndroid onConnected() created gdkHardwareWallet!")

        logger.i { "SATODEBUG DeviceConnectionManagerAndroid connectSatochipDevice() end" }

        return ConnectionResult()
    }

    private suspend fun connectLedgerDevice(
        device: LedgerDevice,
        interaction: HardwareConnectInteraction
    ): ConnectionResult {

        return if (device.isBle) {
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.peripheral!!.identifier)

            suspendCoroutine { continuation ->
                // Ledger (Nano X)
                // Ledger BLE adapter will call the 'onLedger' function when the BLE connection is established
                // LedgerBLEAdapter.connectLedgerBLE(this, btDevice, this::onLedger, this::onLedgerError);
                LedgerBLEAdapter.connectLedgerBLE(device.context, bluetoothDevice,
                    { transport: BTChipTransport, hasScreen: Boolean, disconnectEvent: MutableStateFlow<Boolean> ->
                        scope.launch(context = Dispatchers.IO) {
                            onLedger(
                                device,
                                transport,
                                hasScreen,
                                disconnectEvent,
                                interaction
                            ).also {
                                continuation.resume(it)
                            }
                        }
                    }
                ) { transport: BTChipTransport? ->
                    continuation.resumeWithException(Exception("id_please_reconnect_your_hardware"))
                }
            }
        } else {
            return BTChipTransportAndroid.open(device.usbManager, device.usbDevice)?.let { transport ->
                if (BTChipTransportAndroid.isLedgerWithScreen(device.usbDevice)) {
                    // User entered PIN on-device
                    onLedger(device, transport, true, null, interaction)

                } else {
                    // Prompt for PIN to unlock device before setting it up
                    // showLedgerPinDialog(transport);
                    onLedger(device, transport, false, null, interaction)
                }
            } ?: run {
                throw Exception("id_please_reconnect_your_hardware")
            }
        }
    }

    private suspend fun onLedger(
        device: AndroidDevice,
        transport: BTChipTransport,
        hasScreen: Boolean,
        disconnectEvent: MutableStateFlow<Boolean>?,
        interaction: HardwareConnectInteraction
    ): ConnectionResult {
        transport.setDebug(appInfo.isDevelopmentOrDebug)

        try {

            val dongle = BTChipDongle(transport, hasScreen)
            val application = dongle.application
            logger.i { "Ledger application $application" }

            val isLegacy = application.name.lowercase().contains("legacy")
            val isTestnet = application.name.lowercase().contains("test")
            val isBitcoin = application.name.lowercase().contains("bitcoin")
            val isLiquid = application.name.lowercase().contains("liquid")

            if (application.name.contains("OLOS")) {
                throw Exception("id_ledger_dashboard_detected")
            }

            if(!isBitcoin && !isLiquid){
                throw Exception("id_ledger_dashboard_detected")
            }

            if(isBitcoin && !isLegacy){
                throw Exception("id_ledger_bitcoin_app_detected")
            }

            val network = when {
                isBitcoin && isTestnet -> gdk.networks().testnetBitcoinElectrum
                isLiquid && !isTestnet -> gdk.networks().liquidGreen
                isLiquid && isTestnet -> gdk.networks().testnetLiquidGreen
                else -> {
                    gdk.networks().bitcoinElectrum
                }
            }

            // We don't ask for firmware version while in the dashboard, since the Ledger Nano X would return invalid status
            val fw = dongle.firmwareVersion
            logger.i { "Ledger firmware version $fw" }

            var isFirmwareOutdated = true
            if (fw.architecture == BTChipDongle.BTCHIP_ARCH_LEDGER_1.toInt() && fw.major > 0) {
                // Min allowed: v1.0.4

                // Min allowed: v1.0.4
                isFirmwareOutdated =
                    (fw.major == 1 && fw.minor < 0) || (fw.major == 1 && fw.minor == 0 && fw.patch < 4)
            } else if (fw.architecture == BTChipDongle.BTCHIP_ARCH_NANO_SX.toInt() && fw.major > 0) {
                // Min allowed: v1.3.7
                isFirmwareOutdated =
                    (fw.major == 1 && fw.minor < 3) || (fw.major == 1 && fw.minor == 3 && fw.patch < 7)
            }

            if (isFirmwareOutdated) {

                val isPositive = interaction.askForFirmwareUpgrade(
                    FirmwareUpgradeRequest(
                        deviceBrand = DeviceBrand.Ledger.brand,
                        isUsb = device.isUsb,
                        currentVersion = null,
                        upgradeVersion = null,
                        firmwareList = null,
                        hardwareVersion = null,
                        isUpgradeRequired = !appInfo.isDevelopmentOrDebug
                    )
                ).await()

                if (isPositive != null) {
                    return onLedgerConnected(device, network, dongle, fw.toString(), disconnectEvent, interaction)
                } else {
                    throw Exception("Firmware version is not supported. Please update device.")
                }

            }

            return onLedgerConnected(device, network, dongle, fw.toString(), disconnectEvent, interaction)

        } catch (e: BTChipException) {
            e.printStackTrace()

            if (e.sw == 0x6faa) {
                throw Exception("id_please_disconnect_your_ledger")
            } else {
                throw Exception("id_ledger_dashboard_detected")
            }
        }
    }

    private fun onLedgerConnected(
        device: AndroidDevice,
        network: Network,
        dongle: BTChipDongle,
        firmwareVersion: String,
        disconnectEvent: StateFlow<Boolean>?,
        interaction: HardwareConnectInteraction
    ): ConnectionResult {
        val pin: String? =
            if (device.isUsb && !BTChipTransportAndroid.isLedgerWithScreen(device.usbDevice)) {
                interaction.requestPinBlocking(DeviceBrand.Ledger)
            } else {
                null
            }

        logger.i { "Creating Ledger HW wallet" + if (pin != null) " with PIN" else "" }

        val ledgerDevice = com.blockstream.common.gdk.data.Device(
            name = "Ledger",
            supportsArbitraryScripts = true,
            supportsLowR = false,
            supportsHostUnblinding = false,
            supportsExternalBlinding = false,
            supportsLiquid = DeviceSupportsLiquid.Lite,
            supportsAntiExfilProtocol = DeviceSupportsAntiExfilProtocol.None
        )

        device.gdkHardwareWallet = BTChipHWWallet(dongle, pin, network, ledgerDevice, firmwareVersion, disconnectEvent)

        return ConnectionResult()
    }

    companion object: Loggable()
}