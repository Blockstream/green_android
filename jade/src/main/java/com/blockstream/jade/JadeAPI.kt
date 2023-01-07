package com.blockstream.jade

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.blockstream.jade.data.JadeJson
import com.blockstream.jade.data.VersionInfo
import com.polidea.rxandroidble2.RxBleDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KLogging
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JadeAPI private constructor(
    jade: JadeInterface,
    requestProvider: HttpRequestProvider,
    val isUsb: Boolean
) : JadeAPIJava(jade, requestProvider) {

    override fun getVersionInfo(): VersionInfo {
        return jadeRpcAsString(METHOD_GET_VERSION_INFO, TIMEOUT_AUTONOMOUS).let {
            JadeJson.decode(it)
        }
    }

    private fun jadeRpcAsString(method: String, timeout: Int): String {
        return jadeRpc(method, timeout).toString()
    }

    val isBle: Boolean
        get() = !isUsb

    suspend fun connect(): VersionInfo? {
        // Connect the underlying transport
        jade.connect()

        // Test/flush the connection for a limited time
        for (attempt in 4 downTo 0) {
            // Short sleep before (re-)trying connection)
            delay(1.toDuration(DurationUnit.SECONDS))
            try {
                jade.drain()
                val info = this.versionInfo
                efusemac = info.efuseMac
                return info
            } catch (e: Exception) {
                // On error loop trying again
                logger.warn { "Error trying connect: $e" }
            }
        }

        // Couldn't verify connection
        logger.warn { "Exhausted retries, failed to connect to Jade"}

        return null
    }

    fun connectBlocking(): VersionInfo? {
        return runBlocking { connect() }
    }

    companion object : KLogging() {

        const val METHOD_GET_VERSION_INFO = "get_version_info"

        fun createSerial(
            requestProvider: HttpRequestProvider,
            usbManager: UsbManager,
            usbDevice: UsbDevice,
            baud: Int
        ): JadeAPI {
            val jade = JadeInterface.createSerial(usbManager, usbDevice, baud)
            return JadeAPI(jade, requestProvider, true)
        }

        fun createBle(
            context: Context,
            requestProvider: HttpRequestProvider,
            device: RxBleDevice
        ): JadeAPI {
            val jade = JadeInterface.createBle(context, device)
            return JadeAPI(jade, requestProvider, false)
        }
    }
}