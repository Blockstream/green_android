package com.blockstream.jade

import com.blockstream.jade.api.AuthRequest
import com.blockstream.jade.api.AuthRequestParams
import com.blockstream.jade.api.BlindingFactorRequest
import com.blockstream.jade.api.BlindingFactorRequestParams
import com.blockstream.jade.api.BlindingKeyRequest
import com.blockstream.jade.api.BlindingKeyRequestParams
import com.blockstream.jade.api.BooleanResponse
import com.blockstream.jade.api.ByteArrayResponse
import com.blockstream.jade.api.Commitment
import com.blockstream.jade.api.EntropyRequest
import com.blockstream.jade.api.EntropyRequestParams
import com.blockstream.jade.api.HandshakeCompleteRequest
import com.blockstream.jade.api.HandshakeCompleteRequestParams
import com.blockstream.jade.api.HandshakeInitRequest
import com.blockstream.jade.api.HandshakeInitRequestParams
import com.blockstream.jade.api.HttpRequest
import com.blockstream.jade.api.HttpRequestResponseResult
import com.blockstream.jade.api.JadeSerializer.Companion.jadeId
import com.blockstream.jade.api.LogoutRequest
import com.blockstream.jade.api.MasterBlindingKeyRequest
import com.blockstream.jade.api.MasterBlindingKeyRequestParams
import com.blockstream.jade.api.OtaCompleteRequest
import com.blockstream.jade.api.OtaDataRequest
import com.blockstream.jade.api.OtaRequest
import com.blockstream.jade.api.OtaRequestParams
import com.blockstream.jade.api.PinRequest
import com.blockstream.jade.api.PinRequestParams
import com.blockstream.jade.api.ReceiveAddressRequest
import com.blockstream.jade.api.ReceiveAddressRequestParams
import com.blockstream.jade.api.Request
import com.blockstream.jade.api.Response
import com.blockstream.jade.api.SharedNonceRequest
import com.blockstream.jade.api.SharedNonceRequestParams
import com.blockstream.jade.api.SignMessageRequest
import com.blockstream.jade.api.SignMessageRequestParams
import com.blockstream.jade.api.SignTransactionRequest
import com.blockstream.jade.api.SignTransactionRequestParams
import com.blockstream.jade.api.SignatureRequest
import com.blockstream.jade.api.SignatureRequestParams
import com.blockstream.jade.api.SignedMessage
import com.blockstream.jade.api.StringResponse
import com.blockstream.jade.api.TxInput
import com.blockstream.jade.api.TxInputRequest
import com.blockstream.jade.api.VersionInfo
import com.blockstream.jade.api.VersionInfoRequest
import com.blockstream.jade.api.VersionInfoResponse
import com.blockstream.jade.api.XpubRequest
import com.blockstream.jade.api.XpubRequestParams
import com.blockstream.jade.data.ChangeOutput
import com.blockstream.jade.data.JadeError
import com.blockstream.jade.data.JadeVersion
import com.blockstream.jade.data.SignedTransactionInputs
import com.juul.kable.Peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class JadeAPI internal constructor(
    val jade: JadeInterface,
    private val httpRequestHandler: HttpRequestHandler,
) {

    private var _versionInfo: VersionInfo? = null

    val isUsb = jade.isUsb
    val isBle = !jade.isUsb

    val disconnectEvent: StateFlow<Boolean>?
        get() = jade.bleDisconnectEvent

    suspend fun connect(): VersionInfo? {
        // Connect the underlying transport
        jade.connect()

        // Test/flush the connection for a limited time
        (0 until 3).forEach {
            // Short sleep before (re-)trying connection)
            delay(it.toDuration(DurationUnit.SECONDS))

            try {
                jade.drain()

                return getVersionInfo()
            } catch (e: Exception) {
                // On error loop trying again
                logger.w { "Error trying connect: $e" }
            }
        }

        // Couldn't verify connection
        logger.w { "Exhausted retries, failed to connect to Jade" }

        return null
    }

    suspend fun disconnect(logout: Boolean = true) {
        if (logout) {
            logout()
        }
        jade.disconnect()
    }

    // Generate the blinding factors and commitments for a given output.
    // Can optionally get a "custom" VBF, normally used for the last
    // input where the VBF is not random, but generated accordingly to
    // all the others.
    // `hash_prevouts` and `output_index` have the same meaning as in
    //   the `get_blinding_factor` call.
    // NOTE: the `asset_id` should be passed as it is normally displayed, so
    // reversed compared to the "consensus" representation.
//    @Throws(IOException::class)
//    fun getCommitments(
//        assetId: ByteArray?,
//        value: Long?,
//        hashPrevouts: ByteArray,
//        outputIdx: Int?,
//        vbf: ByteArray?
//    ): Commitment {
//        val params = makeParams("hash_prevouts", hashPrevouts)
//            .put("output_index", outputIdx)
//            .put("asset_id", assetId)
//            .put("value", value)
//
//        // Optional vbf param
//        if (vbf != null) {
//            params.put("vbf", vbf)
//        }
//
//        val result = this.jadeRpc("get_commitments", params, TIMEOUT_AUTONOMOUS)
//        return CborMapper.treeToValue(result, Commitment::class.java)
//    }

    @Throws(JadeError::class)
    private fun <T, P>resultOrThrow(
        request: Request<*, *>,
        response: Response<T, P>?,
    ): P {
        // Timeout/no response
        if (response == null) {
            logger.e { "Timeout - no response received for message id: ${request.id}" }

            throw JadeError(
                JadeError.JADE_RPC_MSG_TIMEOUT,
                "Timeout - no response received for message id: ${request.id}"
            )
        }

        // Raise any error from Jade as an exception
        response.error?.also { error ->
            logger.w { "Received error from Jade: $error" }

            throw JadeError(
                error.code,
                error.message,
                error.data
            )
        }

        if(request.id != response.id) {
            logger.e { "Request/Response id mismatch - expected id: ${request.id}, received: ${response.id}" }

            throw JadeError(
                code = JadeError.JADE_MSGS_OUT_OF_SYNC,
                message = "Request/Response id mismatch - expected id: ${request.id}, received: ${response.id}",
                data = response
            )
        }

        return response.result ?: throw Exception("Result is empty")
    }

    // OTA firmware update
    @Throws(Exception::class)
    suspend fun otaUpdate(
        firmware: ByteArray,
        firmwareSize: Int,
        firmwareHash: String?,
        patchSize: Int?,
        compressedHash: ByteArray,
        cb: (written: Int, totalSize: Int) -> Unit
    ): Boolean {

        val chunkSize = getVersionInfo(useCache = true).jadeOtaMaxChunk

        val response = jadeRpc(
            OtaRequest(
                method = if (patchSize == null) OtaRequest.OTA else OtaRequest.OTA_DELTA,
                params = OtaRequestParams(
                    fwSize = firmwareSize,
                    fwHash = firmwareHash,
                    cmpSize = firmware.size,
                    cmpHash = compressedHash,
                    patchSize = patchSize
                )
            ), BooleanResponse.serializer()
        )

        if(!response){
            return false
        }

        var written = 0
        firmware.toList().chunked(chunkSize).forEach {
            jadeRpc(OtaDataRequest(params = it.toByteArray()), BooleanResponse.serializer())
            written += it.size
            cb.invoke(written, firmware.size)
        }

        return jadeRpc(OtaCompleteRequest(), BooleanResponse.serializer())
    }

    // Get a "trusted" blinding factor to blind an output. Normally the blinding
    // factors are generated and returned in the `get_commitments` call, but
    // for the last output the VBF must be generated on the host side, so this
    //  call allows the host to get a valid ABF to compute the generator and
    // then the "final" VBF. Nonetheless, this call is kept generic, and can
    // also generate VBFs, thus the "type" parameter.
    // `hash_prevouts` is computed as specified in BIP143 (double SHA of all
    //   the outpoints being spent as input. It's not checked right away since
    //   at this point Jade doesn't know anything about the tx we are referring
    //   to. It will be checked later during `sign_liquid_tx`.
    // `output_index` is the output we are trying to blind.
    // `type` can either be "ASSET" or "VALUE" to generate ABFs or VBFs.
    @Throws(Exception::class)
    suspend fun getBlindingFactor(hashPrevouts: ByteArray, outputIdx: Int, type: String): ByteArray {
        val request = BlindingFactorRequest(params = BlindingFactorRequestParams(hashPrevouts = hashPrevouts, outputIdx = outputIdx, type = type))
        return jadeRpc(request, ByteArrayResponse.serializer())
    }

    // Get blinding key for script
    @Throws(Exception::class)
    suspend fun getBlindingKey(script: ByteArray): ByteArray {
        val request = BlindingKeyRequest(params = BlindingKeyRequestParams(script = script))
        return jadeRpc(request, ByteArrayResponse.serializer())
    }

    // Get the shared secret to unblind a tx, given the receiving script on our side
    // and the pubkey of the sender (sometimes called "nonce" in Liquid)
    @Throws(Exception::class)
    suspend fun getSharedNonce(script: ByteArray, pubkey: ByteArray): ByteArray {
        val request = SharedNonceRequest(params = SharedNonceRequestParams(script = script, theirPubKey = pubkey))
        return jadeRpc(request, ByteArrayResponse.serializer())
    }

    // Sign a transaction
    suspend fun signTx(network: String, txn: ByteArray, inputs: List<TxInput>, change: List<ChangeOutput?>): SignedTransactionInputs {
        // 1st message contains txn and number of inputs we are going to send.
        // Reply ok if that corresponds to the expected number of inputs (n).
        val request = SignTransactionRequest(id = jadeId(), method = "sign_tx", params = SignTransactionRequestParams(
            network = network,
            useAeSignatures = true,
            txn = txn,
            numInput = inputs.size,
            change = change
        ))
        val response = jadeRpc(request, BooleanResponse.serializer())

        if(!response){
            throw JadeError(11, "Error response from initial sign_tx call", response)
        }

        // Use helper function to send inputs and process replies
        return signTransactionInputs(inputs)
    }

    // Sign a liquid transaction
    @Throws(Exception::class)
    suspend fun signLiquidTx(
        network: String,
        txn: ByteArray,
        inputs: List<TxInput>,
        trustedCommitments: List<Commitment?>,
        change: List<ChangeOutput?>
    ): SignedTransactionInputs {
        // 1st message contains txn and number of inputs we are going to send.
        // Reply ok if that corresponds to the expected number of inputs (n).
        val request = SignTransactionRequest(id = jadeId(), method = "sign_liquid_tx", params = SignTransactionRequestParams(
            network = network,
            useAeSignatures = true,
            txn = txn,
            numInput = inputs.size,
            trustedCommitments = trustedCommitments,
            change = change
        ))

        val response = jadeRpc(request, BooleanResponse.serializer())

        if(!response){
            throw JadeError(11, "Error response from initial sign_liquid_tx call", response)
        }

        // Use helper function to send inputs and process replies
        return signTransactionInputs(inputs)
    }

    // Helper to send transaction inputs and retrieve signature responses
    @Throws(Exception::class)
    private suspend fun signTransactionInputs(
        inputs: List<TxInput>
    ): SignedTransactionInputs {
        /**
         * Anti-exfil protocol:
         * We send one message per input (which includes host-commitment *but
         * not* the host entropy) and receive the signer-commitment in reply.
         * Once all n input messages are sent, we can request the actual signatures
         * (as the user has a chance to confirm/cancel at this point).
         * We request the signatures passing the host-entropy for each one.
         */

        // Send inputs one at a time, receiving 'signer-commitment' in reply
        val signerCommitments = mutableListOf<ByteArray>()
        for(input in inputs){
            val request = TxInputRequest(id = jadeId(), method = "tx_input", params = input.copy(aeHostEntropy = null))
            signerCommitments += jadeRpc(request, ByteArrayResponse.serializer())
        }

        // Request the signatures one at a time, sending the entropy
        val signatures = mutableListOf<ByteArray>()
        for (input in inputs) {
            // Response in ByteArray
            val signature = jadeRpc(
                SignatureRequest(
                    params = SignatureRequestParams(aeHostEntropy = input.aeHostEntropy)
                ), ByteArrayResponse.serializer()
            )
            signatures += signature
        }
        return SignedTransactionInputs.create(signatures, signerCommitments)
    }

    // Sign a message
    @Throws(Exception::class)
    suspend fun signMessage(
        path: List<Long>,
        message: String,
        useAeProtocol: Boolean,
        aeHostCommitment: ByteArray,
        aeHostEntropy: ByteArray
    ) : SignedMessage {

        if (!useAeProtocol) {
            throw Exception("Jade only supports the Anti-Exfil protocol")
        }

        val request = SignMessageRequest(
            params = SignMessageRequestParams(
                path = path,
                message = message,
                aeHostCommitment = aeHostCommitment.takeIf { useAeProtocol }
            )
        )

        // In the case of Anti-Exfil signing, the inital returned data is the 'signer commitment' bytes
        // (which the caller can use later to verify the AE signature).
        // The caller must then send a 'get_signature' message, passing the 'host entropy'.
        val signerCommitment = jadeRpc(request, ByteArrayResponse.serializer())

        val signature = jadeRpc(
            SignatureRequest(
                params = SignatureRequestParams(aeHostEntropy = aeHostEntropy)
            ), StringResponse.serializer()
        )

        return SignedMessage(
            signature = signature,
            signerCommitment = signerCommitment
        )
    }

    // Get (receive) green address - multisig shield
    suspend fun getReceiveAddress(
        network: String,
        subaccount: Long,
        branch: Long,
        pointer: Long,
        recoveryxpub: String?,
        csvBlocks: Long?
    ): String {
        val request = ReceiveAddressRequest(
            id = jadeId(),
            method = "get_receive_address",
            params = ReceiveAddressRequestParams(
                network = network,
                subAccount = subaccount,
                branch = branch,
                pointer = pointer,
                recoveryXpub = recoveryxpub?.takeIf { it.isNotEmpty() },
                csvBlocks = csvBlocks?.takeIf { it > 0 }
            )
        )
        return jadeRpc(request, StringResponse.serializer())
    }

    // Get (receive) green address - singlesig
    suspend fun getReceiveAddress(network: String, variant: String, path: List<Long>): String {
        val request = ReceiveAddressRequest(
            id = jadeId(),
            method = "get_receive_address",
            params = ReceiveAddressRequestParams(network = network, variant = variant, path = path)
        )
        return jadeRpc(request, StringResponse.serializer())
    }

    private suspend fun logout(): Boolean {
        // Command added in fw 1.1.44
        return if (JadeVersion(getVersionInfo(useCache = true).jadeVersion) >= JadeVersion("0.1.44")) {
            jadeRpc(LogoutRequest(), BooleanResponse.serializer())
        } else {
            true
        }
    }

    suspend fun authUser(network: String): Boolean {
        val request = AuthRequest(params = AuthRequestParams(network = network, epoch = Clock.System.now().toEpochMilliseconds() / 1000))
        return jadeRpc(request, BooleanResponse.serializer())
    }

    suspend fun getVersionInfo(useCache: Boolean = false): VersionInfo {
        _versionInfo.takeIf { useCache }?.also {
            return it
        }

        val request = VersionInfoRequest()
        return jadeRpc(request, VersionInfoResponse.serializer()).also {
            _versionInfo = it
        }
    }

    // Send additional entropy for the rng to jade
    suspend fun addEntropy(entropy: ByteArray): Boolean {
        val request = EntropyRequest(params = EntropyRequestParams(entropy = entropy))
        return jadeRpc(request, BooleanResponse.serializer())
    }


    // Get xpub given path
    suspend fun getXpub(network: String, path: List<Long>): String {
        val request = XpubRequest(params = XpubRequestParams(network = network, path = path))
        return jadeRpc(request, StringResponse.serializer())
    }

    // Liquid calls
    // Get master [un-]blinding key for wallet
    suspend fun getMasterBlindingKey(onlyIfSilent: Boolean): ByteArray {
        val request = MasterBlindingKeyRequest(params = MasterBlindingKeyRequestParams(onlyIfSilent = onlyIfSilent))
        return jadeRpc(request, ByteArrayResponse.serializer())
    }

    private suspend fun <R: Response<*, P>, P> jadeRpc(request: Request<*, *>, serializer: DeserializationStrategy<R>): P {

        val response = jade.makeRpcCall(request = request, serializer = serializer, timeout = request.timeout, drain = false)

        val result = resultOrThrow(request, response)

        /*
         * The Jade can respond with a request for interaction with a remote
         * http server. This is used for interaction with the pinserver but the
         * code below acts as a dumb proxy and simply makes the http request and
         * forwards the response back to the Jade.
         */
        return if (result is HttpRequestResponseResult) {
            logger.d { "HttpRequestResponseResult: $result" }
            val httpResponse = makeHttpRequest(result.httpRequest)


            if (result.httpRequest.onReply == "pin") {
                val data = httpResponse.jsonObject["data"]!!.jsonPrimitive.content
                jadeRpc(request = PinRequest(params = PinRequestParams(data = data)), serializer)
            } else if (result.httpRequest.onReply == "handshake_init") {
                val sig = httpResponse.jsonObject["sig"]?.jsonPrimitive?.content!!
                val ske = httpResponse.jsonObject["ske"]?.jsonPrimitive?.content!!

                jadeRpc(request = HandshakeInitRequest(params = HandshakeInitRequestParams(sig = sig, ske = ske)), serializer)
            } else if (result.httpRequest.onReply == "handshake_complete") {
                val encryptedKey = httpResponse.jsonObject["encrypted_key"]?.jsonPrimitive?.content!!
                val hmac = httpResponse.jsonObject["hmac"]?.jsonPrimitive?.content!!

                jadeRpc(request = HandshakeCompleteRequest(params = HandshakeCompleteRequestParams(encryptedKey = encryptedKey, hmac = hmac)), serializer)
            } else {
                throw Exception("Unsupported on-reply operation")
            }

        } else {
            @Suppress("UNCHECKED_CAST")
            result as P
        }
    }

    // Helper to make http requests (with retries)
    // NOTE: Uses GDKSession's httpRequest() call to ensure Tor use as appropriate.
    private suspend fun makeHttpRequest(
        httpRequest: HttpRequest
    ): JsonElement {
        // If it fails retry up to 3 times
        (0 until 3).forEach { attempt ->
            logger.i { "Making gdk http request ($attempt): $httpRequest" }

            val response = httpRequest.params.let {
                httpRequestHandler.httpRequest(it.toJsonElement())
            }

            logger.i { "Received gdk http response: $response" }

            response.jsonObject["body"]?.let {
                return it
            }

            logger.e { "No response body received!" }
        }

        logger.e { "Exhausted retries" }
        throw Exception("HTTP Request failed")
    }

    companion object : Loggable() {

        fun fromBle(
            peripheral: Peripheral,
            isBonded: Boolean,
            scope: CoroutineScope,
            httpRequestHandler: HttpRequestHandler
        ): JadeAPI {
            val jade = JadeInterface.fromBle(
                peripheral = peripheral,
                isBonded = isBonded,
                scope = scope
            )
            return JadeAPI(jade, httpRequestHandler)
        }
    }
}