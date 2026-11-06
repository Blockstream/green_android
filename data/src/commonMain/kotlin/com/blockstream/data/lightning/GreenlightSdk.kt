@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.data.lightning

import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.glsdk.Config
import com.blockstream.glsdk.Credentials
import com.blockstream.glsdk.Handle
import com.blockstream.glsdk.LnUrlPayRequest
import com.blockstream.glsdk.LnUrlWithdrawRequest
import com.blockstream.glsdk.LnUrlWithdrawRequestData
import com.blockstream.glsdk.Network
import com.blockstream.glsdk.Node
import com.blockstream.glsdk.NodeBuilder
import com.blockstream.glsdk.NodeEventListener
import com.blockstream.glsdk.OnchainReceiveResponse
import com.blockstream.glsdk.OnchainSendResponse
import com.blockstream.glsdk.Scheduler
import com.blockstream.glsdk.Signer
import com.blockstream.glsdk.listPayments
import com.blockstream.glsdk.resolveInput
import com.blockstream.glsdk.use
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GreenlightSdk constructor(private val nodeRpc: Node) {

    suspend fun createInvoice(
        satoshi: Long,
        description: String
    ): LightningReceivePayment {
        val response = nodeRpc.receive(
            label = Uuid.generateV7().toString(),
            description = description,
            amountMsat = satoshi.milliSatoshi(),
        )

        logger.d { "createInvoice: $response" }

        return LightningReceivePayment(
            invoice = (resolveInput(response.bolt11).toLightningInputType() as LightningInputType.Bolt11).invoice,
            openingFeeSatoshi = response.openingFeeMsat.satoshi()
        )
    }

    fun payLnUrl(requestData: LnUrlPayData, amount: Long, comment: String): LnUrlPayOutcome =
        nodeRpc.lnurlPay(
            LnUrlPayRequest(
                data = requestData.raw,
                amountMsat = amount.milliSatoshi(),
                comment = comment,
                validateSuccessActionUrl = true
            )
        ).toLnUrlPayOutcome()

    fun withdrawLnUrl(requestData: LnUrlWithdrawData, amount: Long, description: String?): LnUrlWithdrawOutcome =
        nodeRpc.lnurlWithdraw(
            LnUrlWithdrawRequest(
                data = LnUrlWithdrawRequestData(
                    callback = requestData.callback,
                    k1 = requestData.k1,
                    defaultDescription = requestData.defaultDescription,
                    minWithdrawable = requestData.minWithdrawable,
                    maxWithdrawable = requestData.maxWithdrawable,
                    lnurl = "",
                ),
                amountMsat = amount.milliSatoshi(),
                description = description,
            )
        ).toLnUrlWithdrawOutcome()

//    fun onchainFeeRates(): LightningFees = nodeRpc.onchainFeeRates().toLightningFees()
//
//    fun prepareOnchainSend(toAddress: String, satPerVbyte: UInt?): PreparedOnchainSend = nodeRpc.prepareOnchainSend(
//        destination = toAddress, amountOrAll = "all", satPerVbyte = satPerVbyte ?: 0.toUInt()
//    )
//
//    fun onchainSend(toAddress: String, satPerVbyte: UInt?, prepared: PreparedOnchainSend): OnchainSendResponse = nodeRpc.onchainSend(destination = toAddress, amountOrAll = "all", satPerVbyte = satPerVbyte, utxos = prepared.utxos)
//
//    fun onchainBalanceState(): OnchainBalanceState {
//        return nodeRpc.onchainBalanceState()
//    }

    fun onchainReceive(): OnchainReceiveResponse = nodeRpc.onchainReceive()

    fun onchainSend(toAddress: String): OnchainSendResponse =
        nodeRpc.onchainSend(destination = toAddress, amountOrAll = "all", satPerVbyte = null, utxos = null)

    fun credentials() = nodeRpc.credentials()

    fun nodeState(): LightningNodeState = tryCatchNull { nodeRpc.nodeState().toLightningNodeState() } ?: LightningNodeState.Default

    fun listPayments(): List<LightningPayment> = nodeRpc.listPayments(includeFailures = false).map { it.toLightningPayment() }

    fun sendPayment(invoice: LightningInvoice, satoshi: Long?): SendPaymentResult =
        nodeRpc.send(invoice = invoice.bolt11, amountMsat = satoshi?.milliSatoshi()).toSendPaymentResult()

    fun generateDiagnosticData(): String = nodeRpc.generateDiagnosticData()

    fun disconnect() {
        tryCatchNull {
            nodeRpc.destroy()
        }
    }

    companion object : Loggable(bucket = LogBucket.Lightning) {

        fun config(greenlightKeys: GreenlightKeys) =
            Config().withDeveloperCert(greenlightKeys.developerCert ?: throw Exception("No developer cert provided"))

        // Asks Greenlight whether the Lightning account of this node may be offered to the user.
        // It works before the node is registered.
        suspend fun isLightningAvailable(nodeId: ByteArray): Boolean = withContext(Dispatchers.IO) {
            Scheduler(network = Network.BITCOIN).use { it.lightningAvailable(nodeId = nodeId) }
        }

        suspend fun nodeId(mnemonic: String): ByteArray = withContext(Dispatchers.IO) {
            Signer(phrase = mnemonic).use { it.nodeId() }
        }

        suspend fun restoreCredentials(mnemonic: String, greenlightKeys: GreenlightKeys): ByteArray = withContext(Dispatchers.IO) {
            NodeBuilder(config = config(greenlightKeys)).recover(mnemonic = mnemonic).use { node ->
                node.credentials()
            }
        }

        // connect and startSigner block their caller rather than switch to Dispatchers.IO, because
        // withContext drops its result when the caller is cancelled, which would leak the running
        // node or signer.
        fun connect(
            mnemonic: String,
            credentials: ByteArray?,
            isRestoreOnly: Boolean,
            greenlightKeys: GreenlightKeys,
            eventListener: NodeEventListener
        ): GreenlightSdk {
            val builder = NodeBuilder(config = config(greenlightKeys))
                .withEventListener(listener = eventListener)

            val node = when {
                // Returning user: reuse stored credentials, no server round-trip for registration.
                credentials != null -> builder.connect(credentials = credentials, mnemonic = mnemonic)
                // Restore-only: recover an existing node without falling back to registration if none is found.
                isRestoreOnly -> builder.recover(mnemonic = mnemonic)
                // First run: recover if a node already exists for this mnemonic, otherwise register a new one.
                else -> builder.registerOrRecover(mnemonic = mnemonic, inviteCode = null)

            }

            return GreenlightSdk(node)
        }

        // Starts only the signer (no full node connect) so it can co-sign in-flight operations
        // for a node already registered on this device. Caller must stop() the returned Handle.
        fun startSigner(mnemonic: String, credentials: ByteArray): Handle {
            val signer = Signer(phrase = mnemonic).authenticate(creds = Credentials.load(raw = credentials))
            logger.i { "Starting signer" }
            return signer.start()
        }
    }
}