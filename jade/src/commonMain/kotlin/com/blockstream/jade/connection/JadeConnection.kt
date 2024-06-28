package com.blockstream.jade.connection

import com.blockstream.common.utils.Loggable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout

/**
 * Abstract a low-level Jade connection - eg. over serial or ble.
 */
abstract class JadeConnection {
    // Derived classes push incoming/received data into this queue
    private val receivedData = Channel<ByteArray>(capacity = UNLIMITED)

    abstract val isUsb: Boolean

    abstract val isConnected: Boolean

    abstract val disconnectEvent: StateFlow<Boolean>?

    abstract suspend fun connect()

    abstract suspend fun disconnect()

    abstract suspend fun write(bytes: ByteArray): Int

    // Function to push data into the dataReceived queue
    protected fun onDataReceived(data: ByteArray) {
        logger.d { "onDataReceived: ${data.size} bytes" }

        if (data.isNotEmpty()) {
            receivedData.trySend(data)
        }
    }

    // Read a ByteArray, waiting for the timeout period (ms) if necessary.
    // If the timeout is zero data must be present immediately.
    // A timeout of less than zero and the call blocks until a byte is received.
    // Returns null if the timeout expires and no byte is available.
    suspend fun read(timeout: Int): ByteArray? {
        val pollTimeout = if ((timeout >= 0)) timeout else 10000

        return if (timeout == 0) {
            receivedData.tryReceive().getOrNull()
        } else if (timeout > 0) {
            try {
                withTimeout(pollTimeout.toLong()) {
                    receivedData.receive()
                }
            } catch (e: Exception) {
                logger.w { "read() timed-out - timeout(ms): $timeout" }
                null
            }
        } else {
            receivedData.receive()
        }
    }

    // Reads all the data that is currently outstanding (ie. that has been received).
    // Returns empty byte array if no data received/present.
    fun drain(): ByteArray {
        var drainedTo = byteArrayOf()

        // Drain the queue of data received
        do {
            val result = receivedData.tryReceive().also {
                it.getOrNull()?.also {
                    drainedTo += it
                }
            }
        } while (result.isSuccess)

        return drainedTo
    }

    companion object : Loggable()
}
