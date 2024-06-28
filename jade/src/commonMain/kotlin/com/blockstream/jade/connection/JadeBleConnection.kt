package com.blockstream.jade.connection

import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.utils.Loggable
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * Low-level BLE backend interface to Jade
 * Calls to send and receive bytes bytes to/from Jade.
 * Intended for use wrapped by JadeInterface (see JadeInterface.createBle()).
 */
class JadeBleConnection internal constructor(
    private val peripheral: Peripheral,
    private val scope: ApplicationScope,
    private val isBonded: Boolean
) : JadeConnection() {

    override val isUsb: Boolean = false

    override val isConnected
        get() = peripheral.state.value == State.Connected


    override val disconnectEvent: StateFlow<Boolean>
        get() = peripheral.state.map {
            it is State.Disconnected
        }.stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun connect() {
        logger.d { "Connecting" }

        peripheral.connect()

        if(!isBonded) {
            // Initiate a write so that Android bond the device if needed before continuing with the observe
            // else if we observe before that won't work
            peripheral.write(WriteCharacteristics, byteArrayOf(), writeType = WriteType.WithResponse)

            logger.d { "Bonded... delaying..." }

            // Stabilization delay
            delay(2000L)
        }

        peripheral.observe(ObserveCharacteristics).onEach {
            // Process data.
            onDataReceived(it)
        }.launchIn(scope)
    }

    override suspend fun disconnect() {
        logger.d { "Disconnecting" }

        runBlocking {
            peripheral.disconnect()
        }
    }

    override suspend fun write(bytes: ByteArray): Int {
        try {
            bytes.toList().chunked(JADE_MTU).forEach {
                peripheral.write(WriteCharacteristics, it.toByteArray(), writeType = WriteType.WithResponse)
            }

            return bytes.size.also {
                logger.d { "Sent ${bytes.size} bytes" }
            }
        } catch (e: Exception) {
            logger.d { "Send Failure: ${e.message}" }
            e.printStackTrace()
            return 0
        }
    }

    companion object : Loggable() {
        const val JADE_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"

        private val WriteCharacteristics =
            characteristicOf(JADE_SERVICE, "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val ObserveCharacteristics =
            characteristicOf(JADE_SERVICE, "6e400003-b5a3-f393-e0a9-e50e24dcca9e")

        const val JADE_MTU = 512
    }
}
