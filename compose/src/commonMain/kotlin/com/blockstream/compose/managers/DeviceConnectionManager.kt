package com.blockstream.compose.managers

import com.blockstream.common.devices.GreenDevice
import com.blockstream.common.devices.JadeDevice
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.data.DeviceSupportsAntiExfilProtocol
import com.blockstream.common.gdk.data.DeviceSupportsLiquid
import com.blockstream.common.gdk.device.GdkHardwareWallet
import com.blockstream.common.gdk.device.HardwareConnectInteraction
import com.blockstream.common.interfaces.ConnectionResult
import com.blockstream.common.interfaces.DeviceConnectionInterface
import com.blockstream.common.jade.JadeHWWallet
import com.blockstream.jade.HttpRequestHandler
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.data.JadeError
import com.blockstream.jade.data.JadeState
import com.blockstream.jade.firmware.JadeFirmwareManager
import kotlinx.coroutines.CoroutineScope



open class DeviceConnectionManager(
    val gdk: Gdk,
    val wally: Wally,
    val scope: CoroutineScope
): DeviceConnectionInterface {

    @Throws(Exception::class)
    override suspend fun connectDevice(device: GreenDevice, httpRequestHandler: HttpRequestHandler, interaction: HardwareConnectInteraction): ConnectionResult {
        device.frozeHeartbeat()

        try {
            (device as? JadeDevice)?.let {
                return connectJadeDevice(device = it, httpRequestHandler = httpRequestHandler)
            }

            throw Exception("Device is not supported")
        } catch (e: Exception) {
            disconnectDevice(device)
            throw e
        }
    }

    internal open suspend fun disconnectDevice(device: GreenDevice){
        device.updateHeartbeat()

        (device as? JadeDevice)?.also {
            disconnectJadeDevice(it)
        }
    }


    private suspend fun disconnectJadeDevice(device: JadeDevice) {
        try {
            device.jadeApi?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun connectJadeDevice(device: JadeDevice, httpRequestHandler: HttpRequestHandler) : ConnectionResult {
        return createJadeApi(device = device, httpRequestHandler = httpRequestHandler)?.let { jadeApi ->
            device.jadeApi = jadeApi

            jadeApi.connect()?.let {
                onJadeConnected(device = device, jade = jadeApi)
            } ?: throw Exception("Couldn't connect to Jade device")

        } ?: throw Exception("Cannot create Jade API")
    }

    open suspend fun createJadeApi(device: GreenDevice, httpRequestHandler: HttpRequestHandler): JadeAPI? {
        return device.peripheral?.let { peripheral ->
            JadeAPI.fromBle(
                peripheral = peripheral,
                isBonded = device.isBonded,
                scope = scope,
                httpRequestHandler = httpRequestHandler
            )
        }
    }

    private suspend fun onJadeConnected(device: GreenDevice, jade: JadeAPI): ConnectionResult {
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

        device.gdkHardwareWallet = jadeWallet

        return ConnectionResult(isJadeUninitialized = jadeWallet.isUninitializedOrUnsaved)
    }

    override suspend fun authenticateDeviceIfNeeded(
        httpRequestHandler: HttpRequestHandler,
        interaction: HardwareConnectInteraction,
        gdkHardwareWallet: GdkHardwareWallet,
        jadeFirmwareManager: JadeFirmwareManager?
    ) {
        println("SATODEBUG DeviceConnectionManager authenticateDeviceIfNeeded() start")
        if (gdkHardwareWallet is JadeHWWallet && gdkHardwareWallet.getVersionInfo().jadeState != JadeState.READY) {
            try {
                gdkHardwareWallet.authenticate(interaction, jadeFirmwareManager ?: JadeFirmwareManager(
                    firmwareInteraction = interaction,
                    httpRequestHandler = httpRequestHandler,
                    jadeFwVersionsFile = JadeFirmwareManager.JADE_FW_VERSIONS_LATEST,
                    forceFirmwareUpdate = false
                ))
            } catch (e: Exception) {
                if (e is JadeError) {
                    when (e.code) {
                        JadeError.UNSUPPORTED_FIRMWARE_VERSION -> {
                            throw Exception("id_outdated_hardware_wallet")
                        }

                        JadeError.CBOR_RPC_NETWORK_MISMATCH -> {
                            throw Exception("id_the_network_selected_on_the")
                        }

                        else -> {
                            // Error from Jade hw - show the hw error message as a toast
                            throw Exception("id_please_reconnect_your_hardware")
                        }
                    }
                } else if ("GDK_ERROR_CODE -1 GA_connect" == e.message) {
                    throw Exception("id_unable_to_contact_the_green")
                } else {
                    throw Exception("id_please_reconnect_your_hardware")
                }
            }
        } else if(jadeFirmwareManager != null && gdkHardwareWallet is JadeHWWallet) {
            // force update if needed
            jadeFirmwareManager.checkFirmware(jade = gdkHardwareWallet.jade)
        }
        println("SATODEBUG DeviceConnectionManager authenticateDeviceIfNeeded() end")
    }
}