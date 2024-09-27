package com.blockstream.jade

import com.blockstream.jade.api.AuthRequest
import com.blockstream.jade.api.HandshakeInitRequest
import com.blockstream.jade.api.HttpRequestDataResponse
import com.blockstream.jade.api.HttpRequestNoDataResponse
import com.blockstream.jade.api.JadeSerializer
import com.blockstream.jade.api.LogResponse
import com.blockstream.jade.api.Request
import com.blockstream.jade.api.Response
import com.blockstream.jade.connection.JadeBleConnection
import com.blockstream.jade.connection.JadeConnection
import com.juul.kable.Peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.DeserializationStrategy

/**
 * Mid-level interface to Jade
 * Wraps either a serial or a ble connection
 * Calls to send and receive messages as JsonNode trees.
 * Intended for use wrapped by JadeAPI (see JadeAPI.createSerial() and JadeAPI.createBle()).
 */
@OptIn(ExperimentalStdlibApi::class)
class JadeInterface internal constructor(private val connection: JadeConnection) {
    private val mutex = Mutex()

    private val isConnected: Boolean
        get() = connection.isConnected

    val isUsb: Boolean
        get() = connection.isUsb

    val bleDisconnectEvent: StateFlow<Boolean>?
        get() = connection.disconnectEvent

    suspend fun connect() {
        connection.connect()

        // Read and discard anything present on connection
        drain().also {
            logger.d { "Discarded ${it.size} bytes on connection" }
        }
    }

    suspend fun disconnect() {
        connection.disconnect()
    }

    fun drain(): ByteArray {
        logger.d { "Draining interface" }
        return connection.drain()
    }

    @Throws(Exception::class)
    suspend fun <R : Response<*, P>, P> makeRpcCall(
        request: Request<*, *>,
        serializer: DeserializationStrategy<R>,
        timeout: Int,
        drain: Boolean = false
    ): Response<*, *>? = mutex.withLock {
        // If requested, drain any existing outstanding messages first
        if (drain) {
            drain()
        }

        // Send the request
        writeRequest(request)

        // Await the response
        readResponse(request, serializer, timeout)
    }

    @Throws(Exception::class)
    private suspend fun writeRequest(request: Request<*, *>) {
        if (!isConnected) {
            throw Exception("JadeInterface not connected")
        }

        logger.d { "Sending request: $request" }
        val bytes = request.toCbor()

        logger.d { "Sending ${bytes.size} bytes : ${bytes.toHexString()}" }
        connection.write(bytes)
    }

    @Throws(Exception::class)
    private suspend fun <R : Response<*, P>, P> readResponse(
        request: Request<*, *>,
        serializer: DeserializationStrategy<R>,
        timeout: Int
    ): Response<*, *>? {
        if (!isConnected) {
            throw Exception("JadeInterface not connected")
        }

        logger.d { "Awaiting response - timeout(ms): $timeout" }
        var collected = byteArrayOf()

        while (true) {
            // Collect response bytes so we can try to parse them as a cbor message
            val next = connection.read(timeout)

            if (next == null) {
                // Timeout or other critical error
                logger.w { "read() operation returned no next byte - timeout(ms): $timeout" }
                return null
            }

            collected += next

            try {
                // Try to parse as into response message
                val response: R? = JadeSerializer.decodeOrNull(serializer, collected.also {
                    logger.d { "Received data: ${it.toHexString()}" }
                })

                // Platform specific cbor de-serializer
                val json: String? = null
                // json = CborMapper.readTree(collected.toByteArray())

                // Is it a response to a request ?
                if (response != null) {
                    // A proper response
                    logger.i { "Response received: $response $json" }
                    return response
                }

                // If request is AuthRequest check if it's a HttpRequestResponse
                if (request is AuthRequest || request is HandshakeInitRequest) {
                    val httpResponse = JadeSerializer.decodeOrNull(
                        serializer = HttpRequestDataResponse.serializer(),
                        cbor = collected
                    )?.toHttpRequestDataResponse() ?: JadeSerializer.decodeOrNull(
                        serializer = HttpRequestNoDataResponse.serializer(),
                        cbor = collected
                    )?.toHttpRequestDataResponse()

                    if (httpResponse != null) {
                        logger.d { "HttpResponse $httpResponse" }
                        return httpResponse
                    }
                }

                if (json != null) {
                    logger.e { "Message not recognized by Kotlin.Serialization. Check Response Class: $json" }
                }

                val log = JadeSerializer.decodeOrNull(LogResponse.serializer(), collected)

                if (log != null) {
                    // Clear the collected bytes
                    collected = byteArrayOf()

                    logger.d { "LOG: ${log.log}" }
                }
            } catch (e: Exception) {
                logger.w { "Error: ${e.message}" }
                e.printStackTrace()
                throw e
            }
        }
    }

    companion object : Loggable() {

        fun fromBle(
            peripheral: Peripheral,
            isBonded: Boolean,
            scope: CoroutineScope
        ): JadeInterface {
            val ble = JadeBleConnection(peripheral = peripheral, scope = scope, isBonded = isBonded)
            return JadeInterface(ble)
        }
    }
}
