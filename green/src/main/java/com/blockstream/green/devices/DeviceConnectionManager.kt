package com.blockstream.green.devices

import android.bluetooth.BluetoothAdapter
import android.os.Build
import com.blockstream.JadeHWWallet
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.DeviceSupportsAntiExfilProtocol
import com.blockstream.common.gdk.data.DeviceSupportsLiquid
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.interfaces.HttpRequestHandler
import com.blockstream.green.R
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.data.JadeNetworks
import com.blockstream.jade.data.JadeState
import com.blockstream.jade.api.VersionInfo
import com.blockstream.jade.data.JadeError
import com.blockstream.jade.data.JadeVersion
import com.btchip.BTChipDongle
import com.btchip.BTChipException
import com.btchip.comm.BTChipTransport
import com.btchip.comm.android.BTChipTransportAndroid
import com.greenaddress.greenbits.wallets.BTChipHWWallet
import com.greenaddress.greenbits.wallets.FirmwareUpgradeRequest
import com.greenaddress.greenbits.wallets.JadeFirmwareManager
import com.greenaddress.greenbits.wallets.LedgerBLEAdapter
import com.greenaddress.greenbits.wallets.TrezorHWWallet
import com.satoshilabs.trezor.Trezor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.blockstream.common.utils.Loggable
import com.blockstream.jade.fromUsb
import kotlinx.coroutines.runBlocking

class DeviceConnectionManager constructor(
    val gdk: Gdk,
    val wally: Wally,
    val scope: CoroutineScope,
    val applicationScope: ApplicationScope,
    val bluetoothAdapter: BluetoothAdapter,
    private val httpRequestHandler: HttpRequestHandler,
    private val interaction: HardwareConnectInteraction,
) {

    private val jadeFirmwareManager by lazy {
        JadeFirmwareManager(
            interaction,
            httpRequestHandler,
            JadeFirmwareManager.JADE_FW_VERSIONS_LATEST,
            false
        )
    }

    var needsAndroid14BleUpdate:Boolean = false
        private set

    fun connectDevice(device: GreenDevice) {
        scope.launch(context = Dispatchers.IO + logException()) {
            try {
                when {
                    device.deviceBrand.isJade -> {
                        connectJadeDevice(device)
                    }

                    device.deviceBrand.isTrezor -> {
                        device.toAndroidDevice()?.let { connectTrezorDevice(it) }
                    }

                    device.deviceBrand.isLedger -> {
                        device.toAndroidDevice()?.let { connectLedgerDevice(it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.also { interaction.showError(it) }
                interaction.onDeviceFailed(device)
            }
        }
    }

    private suspend fun connectJadeDevice(device: GreenDevice) {
        try {
            (device.peripheral?.let { peripheral ->
                JadeAPI.fromBle(
                    peripheral = peripheral,
                    isBonded = device.isBonded,
                    scope = applicationScope,
                    httpRequestHandler = httpRequestHandler
                )
            } ?: device.toAndroidDevice()?.usbDevice?.let { usbDevice ->
                JadeAPI.fromUsb(
                    usbDevice = usbDevice,
                    usbManager = device.toAndroidDevice()!!.usbManager,
                    httpRequestHandler = httpRequestHandler
                )
            })?.also { jadeApi ->

                val version = jadeApi.connect()

                if (version != null) {
                    onJadeConnected(device, version, jadeApi)
                } else {
                    closeJadeAndFail(device, jadeApi)
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            closeJadeAndFail(device, null)
        }
    }

    suspend fun authenticateDeviceIfNeeded(gdkHardwareWallet: GdkHardwareWallet, jadeFirmwareManager: JadeFirmwareManager? = null) {
        if(gdkHardwareWallet is JadeHWWallet && gdkHardwareWallet.getVersionInfo().jadeState != JadeState.READY){
            try {
                gdkHardwareWallet.authenticate(interaction, jadeFirmwareManager ?: this.jadeFirmwareManager)
            } catch (e: Exception) {
                if (e is JadeError) {
                    when (e.code) {
                        JadeError.UNSUPPORTED_FIRMWARE_VERSION -> {
                            interaction.showInstructions(R.string.id_outdated_hardware_wallet)
                        }
                        JadeError.CBOR_RPC_NETWORK_MISMATCH -> {
                            interaction.showInstructions(R.string.id_the_network_selected_on_the)
                        }
                        else -> {
                            // Error from Jade hw - show the hw error message as a toast
                            interaction.showError(e.message ?: "Error")
                            interaction.showInstructions(R.string.id_please_reconnect_your_hardware)
                        }
                    }
                } else if ("GDK_ERROR_CODE -1 GA_connect" == e.message) {
                    interaction.showInstructions(R.string.id_unable_to_contact_the_green)
                } else {
                    interaction.showInstructions(R.string.id_please_reconnect_your_hardware)
                }
            }
        } else if(jadeFirmwareManager != null && gdkHardwareWallet is JadeHWWallet) {
            // force update if needed
            jadeFirmwareManager.checkFirmware(jade = gdkHardwareWallet.jade)
        }
    }

    private suspend fun onJadeConnected(device: GreenDevice, verInfo: VersionInfo, jade: JadeAPI) {
        try {
            val version = JadeVersion(verInfo.jadeVersion)

            val jadeDevice = com.blockstream.common.gdk.data.Device(
                name = "Jade",
                supportsArbitraryScripts = true,
                supportsLowR = true,
                supportsHostUnblinding = true,
                supportsExternalBlinding = true,
                supportsLiquid = DeviceSupportsLiquid.Lite,
                supportsAntiExfilProtocol = DeviceSupportsAntiExfilProtocol.Optional
            )

            val jadeWallet = JadeHWWallet(gdk, wally, jade, jadeDevice)

            if (version.isLessThan(JadeVersion("1.0.25")) && device.isBle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                needsAndroid14BleUpdate = true
            }

            onHWalletCreated(device, jadeWallet, jadeWallet.isUninitializedOrUnsaved)
        } catch (e: Exception) {
            closeJadeAndFail(device, jade)
        }
    }

    private fun closeJadeAndFail(device: GreenDevice, jadeApi: JadeAPI?) {
        try {
            runBlocking {
                jadeApi?.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            interaction.onDeviceFailed(device)
        }
    }

    private suspend fun connectTrezorDevice(device: AndroidDevice) {
        try {
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
                        deviceBrand = DeviceBrand.Trezor,
                        isUsb = true,
                        currentVersion = null,
                        upgradeVersion = null,
                        firmwareList = null,
                        hardwareVersion = null,
                        isUpgradeRequired = !isDevelopmentOrDebug
                    )
                ).await()

                if (isPositive != null) {
                    onTrezorConnected(device, trezor, firmware)
                } else {
                    closeTrezorAndFail(device)
                }
                return
            }

            onTrezorConnected(device, trezor, firmware)
        }catch (e: Exception){
            e.printStackTrace()
            closeTrezorAndFail(device)
        }
    }

    private fun onTrezorConnected(device: AndroidDevice, trezor: Trezor, firmwareVersion: String?) {
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

        onHWalletCreated(device, TrezorHWWallet(trezor, trezorDevice, firmwareVersion))
    }

    private fun closeTrezorAndFail(device: AndroidDevice) {
        interaction.onDeviceFailed(device)
    }

    private fun connectLedgerDevice(device: AndroidDevice) {
        if (device.isBle) {

            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.peripheral!!.identifier)

            // Ledger (Nano X)
            // Ledger BLE adapter will call the 'onLedger' function when the BLE connection is established
            // LedgerBLEAdapter.connectLedgerBLE(this, btDevice, this::onLedger, this::onLedgerError);
            LedgerBLEAdapter.connectLedgerBLE(
                device.context,
                bluetoothDevice,
                { transport: BTChipTransport, hasScreen: Boolean, disconnectEvent: MutableStateFlow<Boolean> ->
                    scope.launch(context = Dispatchers.IO) {
                        onLedger(
                            device,
                            transport,
                            hasScreen,
                            disconnectEvent
                        )
                    }
                }
            ) { transport: BTChipTransport? ->
                interaction.showInstructions(R.string.id_please_reconnect_your_hardware)
                closeLedgerAndFail(device, transport)
            }
        } else {
            BTChipTransportAndroid.open(device.usbManager, device.usbDevice)?.also { transport ->
                if (BTChipTransportAndroid.isLedgerWithScreen(device.usbDevice)) {
                    // User entered PIN on-device
                    scope.launch {
                        onLedger(
                            device,
                            transport,
                            true,
                            null
                        )
                    }
                } else {
                    // Prompt for PIN to unlock device before setting it up
                    // showLedgerPinDialog(transport);
                    scope.launch {
                        onLedger(
                            device,
                            transport,
                            false,
                            null
                        )
                    }
                }
            } ?: run {
                interaction.showInstructions(R.string.id_please_reconnect_your_hardware)
            }
        }
    }

    private suspend fun onLedger(
        device: AndroidDevice,
        transport: BTChipTransport,
        hasScreen: Boolean,
        disconnectEvent: MutableStateFlow<Boolean>?
    ) {
        transport.setDebug(isDevelopmentOrDebug)

        try {

            val dongle = BTChipDongle(transport, hasScreen)
            val application = dongle.application
            logger.i { "Ledger application $application" }

            val isLegacy = application.name.lowercase().contains("legacy")
            val isTestnet = application.name.lowercase().contains("test")
            val isBitcoin = application.name.lowercase().contains("bitcoin")
            val isLiquid = application.name.lowercase().contains("liquid")

            if (application.name.contains("OLOS")) {
                interaction.showInstructions(R.string.id_ledger_dashboard_detected)
                closeLedgerAndFail(device, transport)
                return
            }

            if(!isBitcoin && !isLiquid){
                interaction.showInstructions(R.string.id_ledger_dashboard_detected)
                closeLedgerAndFail(device, transport)
                return
            }

            if(isBitcoin && !isLegacy){
                interaction.showInstructions(R.string.id_ledger_bitcoin_app_detected)
                closeLedgerAndFail(device, transport)
                return
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
                        deviceBrand = DeviceBrand.Ledger,
                        isUsb = device.isUsb,
                        currentVersion = null,
                        upgradeVersion = null,
                        firmwareList = null,
                        hardwareVersion = null,
                        isUpgradeRequired = !isDevelopmentOrDebug
                    )
                ).await()

                if (isPositive != null) {
                    onLedgerConnected(device, network, dongle, fw.toString(), disconnectEvent)
                } else {
                    closeLedgerAndFail(device, transport)
                }

            }

            onLedgerConnected(device, network, dongle, fw.toString(), disconnectEvent)

        } catch (e: BTChipException) {
            e.printStackTrace()

            if (e.sw == 0x6faa) {
                interaction.showInstructions(R.string.id_please_disconnect_your_ledger)
            } else {
                interaction.showInstructions(R.string.id_ledger_dashboard_detected)
            }

            closeLedgerAndFail(device, transport)
        } catch (e: Exception) {
            e.printStackTrace()
            closeLedgerAndFail(device, transport)
        }
    }

    private fun onLedgerConnected(
        device: AndroidDevice,
        network: Network,
        dongle: BTChipDongle,
        firmwareVersion: String,
        disconnectEvent: StateFlow<Boolean>?
    ) {
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

        onHWalletCreated(
            device,
            BTChipHWWallet(dongle, pin, network, ledgerDevice, firmwareVersion, disconnectEvent)
        )
    }

    private fun closeLedgerAndFail(device: AndroidDevice, transport: BTChipTransport?) {
        try {
            transport?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            interaction.onDeviceFailed(device)
        }
    }

    private fun onHWalletCreated(device: GreenDevice, hwWallet: GdkHardwareWallet, isJadeUninitialized: Boolean? = null) {
        device.gdkHardwareWallet = hwWallet
        interaction.onDeviceReady(device, isJadeUninitialized)
    }

    suspend fun getOperatingNetworkForEnviroment(gdkHardwareWallet: GdkHardwareWallet, isTestnet: Boolean): Network {
        return (when (gdkHardwareWallet) {
            is JadeHWWallet -> {
                gdkHardwareWallet.getVersionInfo().jadeNetworks.let { networks ->
                    when (networks) {
                        JadeNetworks.MAIN -> {
                            gdk.networks().bitcoinElectrum.takeIf { !isTestnet }
                        }
                        JadeNetworks.TEST -> {
                            gdk.networks().testnetBitcoinElectrum.takeIf { isTestnet  }
                        }
                        else -> {
                            if(isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum
                        }
                    }
                }
            }
            is TrezorHWWallet -> {
                // Can operate on mainnet/testnet
                if(isTestnet) gdk.networks().testnetBitcoinElectrum else gdk.networks().bitcoinElectrum
            }
            is BTChipHWWallet -> {
                gdkHardwareWallet.network.takeIf { it.isTestnet == isTestnet }
            }
            else -> {
                null
            }
        }) ?: getOperatingNetwork(gdkHardwareWallet)
    }

    suspend fun getOperatingNetwork(gdkHardwareWallet: GdkHardwareWallet): Network {
        return when (gdkHardwareWallet) {
            is JadeHWWallet -> {
                gdkHardwareWallet.getVersionInfo().jadeNetworks.let { networks->
                    when (networks) {
                        JadeNetworks.MAIN -> {
                            gdk.networks().bitcoinElectrum
                        }
                        JadeNetworks.TEST -> {
                            gdk.networks().testnetBitcoinElectrum
                        }
                        else -> {
                            interaction.requestNetwork()
                        }
                    }
                }
            }
            is TrezorHWWallet -> {
                interaction.requestNetwork()
            }

            is BTChipHWWallet -> {
                gdkHardwareWallet.network!!
            }

            else -> {
                throw Exception("Not implemented")
            }
        } ?: throw Exception("id_action_canceled")
    }

    companion object : Loggable()
}