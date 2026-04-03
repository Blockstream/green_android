@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.data.lightning

import com.blockstream.data.config.AppInfo
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.platformName
import com.blockstream.glsdk.Exception
import com.blockstream.glsdk.NodeEvent
import com.blockstream.glsdk.NodeEventListener
import com.blockstream.glsdk.OnchainReceiveResponse
import com.blockstream.glsdk.OnchainSendResponse
import com.blockstream.glsdk.resolveInput
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

enum class ConnectStatus {
    Connect, Failed
}

class LightningSdk(
    private val appInfo: AppInfo,
    val workingDir: String,
    private val greenlightKeys: GreenlightKeys,
    private val firebase: FcmCommon,
) : NodeEventListener {

    private var greenlightSdkOrNull: GreenlightSdk? = null
    private val greenlightSdk
        get() = greenlightSdkOrNull!!

    val nodeInfoStateFlow: StateFlow<LightningNodeState>
        field = MutableStateFlow(LightningNodeState.Default)

    val healthCheckStatus: StateFlow<LightningHealthStatus>
        field = MutableStateFlow(LightningHealthStatus.OPERATIONAL)

    val eventSharedFlow: SharedFlow<LightningEvent>
        field = MutableSharedFlow<LightningEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val swapInfoStateFlow: StateFlow<List<Pair<LightningSwapInfo, Boolean>>>
        field = MutableStateFlow<List<Pair<LightningSwapInfo, Boolean>>>(listOf())

    val reverseSwapInfoStateFlow: StateFlow<List<LightningReverseSwapInfo>>
        field = MutableStateFlow<List<LightningReverseSwapInfo>>(listOf())

    private var inputTypeCache: Pair<String, LightningInputType>? = null

    val isConnected: Boolean
        get() = greenlightSdkOrNull != null

    suspend fun getNodeCredentials(mnemonic: String): ByteArray {
        return greenlightSdkOrNull?.credentials() ?: GreenlightSdk.restoreCredentials(mnemonic, greenlightKeys)
    }

    suspend fun connect(
        mnemonicAndCredentials: GreenlightMnemonicAndCredentials,
        parentXpubHashId: String? = null,
        isRestore: Boolean = false,
        quickResponse: Boolean = false
    ): ConnectStatus {
        if (greenlightSdkOrNull != null) {
            return ConnectStatus.Connect
        }
        try {
            logger.i { "Connecting to glsdk" }

            withTimeout(if (quickResponse) 5000L else 30000L) {
                greenlightSdkOrNull = GreenlightSdk.connect(
                    mnemonic = mnemonicAndCredentials.mnemonic,
                    credentials = mnemonicAndCredentials.credentials,
                    isRestoreOnly = isRestore,
                    greenlightKeys = greenlightKeys,
                    eventListener = this@LightningSdk
                )
            }

            parentXpubHashId?.also { registerNotificationWebhook(it) }

            updateNodeState()

            return ConnectStatus.Connect
        } catch (e: Exception) {
            e.printStackTrace()
            return ConnectStatus.Failed
        }
    }

    suspend fun stop() = withNodeContextCatch {
        greenlightSdkOrNull?.disconnect()
        greenlightSdkOrNull = null
    }

    private suspend fun registerNotificationWebhook(xpubHashId: String) = withNodeContextCatch {
        firebase.token?.also { token ->
            (if (appInfo.isDevelopment) GREEN_NOTIFY_DEVELOPMENT else GREEN_NOTIFY_PRODUCTION).let { backend ->
                "$backend/api/v1/notify?platform=${platformName()}&token=$token&app_data=$xpubHashId"
            }.also { url ->
                logger.i { "Registering webhook for wallet($xpubHashId) as $url" }
                // TODO glsdk: register the FCM webhook with greenlight once an equivalent of breez registerWebhook is exposed.
            }
        }
    }

    private suspend fun updateNodeState(): LightningNodeState = withNodeContext {
        greenlightSdk.nodeState().also {
            logger.d { "NodeState: $it" }
            nodeInfoStateFlow.value = it
            // Emit New Block as a way to identify established connection
            eventSharedFlow.tryEmit(LightningEvent.NewBlock(it.blockHeight))
        }
    }

    suspend fun sync() = withNodeContext {
        // glsdk pulls state on demand; nothing to sync explicitly.
    }

    suspend fun balanceOnChannel(): Long? = withNodeContextCatch {
        updateNodeState().channelsBalanceSatoshi().also {
            logger.d { "Balance (channel): $it" }
        }
    }

    suspend fun balanceCombined(): Long? = withNodeContextCatch {
        updateNodeState().let {
            it.channelsBalanceSatoshi() + it.onchainBalanceSatoshi()
        }.also {
            logger.d { "Balance (channel+onchain): $it" }
        }
    }

    // Cache input type to avoid server communication (LNURL) and probably a change in the requested data
    suspend fun parseBoltOrLNUrlAndCache(input: String): LightningInputType? {
        return inputTypeCache.takeIf { inputTypeCache?.first == input }?.second
            ?: parseBoltOrLNUrl(input)
    }

    private suspend fun parseBoltOrLNUrl(input: String?): LightningInputType? = withNodeContextCatch {
        if (!input.isNullOrBlank()) {
            resolveInput(input).toLightningInputType()?.also {
                inputTypeCache = input to it
            }
        } else {
            null
        }
    }

    suspend fun getTransactions(): List<LightningPayment>? = withNodeContextCatch {
        greenlightSdk.listPayments().also {
            logger.d { "Payments: $it" }
        }
    }

    suspend fun createInvoice(satoshi: Long, description: String): LightningReceivePayment = withNodeContext {
        logger.i { "Creating a $satoshi sats invoice - NodeId: ${nodeInfoStateFlow.value.id}" }
        greenlightSdk.createInvoice(satoshi = satoshi, description = description)
    }

    suspend fun sendPayment(invoice: LightningInvoice, satoshi: Long?): SendPaymentResult = withNodeContext {
        logger.i { "Send payment of $satoshi sats - Invoice: ${invoice.bolt11} - NodeId: ${nodeInfoStateFlow.value.id}" }
        greenlightSdk.sendPayment(invoice, satoshi)
    }

    suspend fun openChannelFee(satoshi: Long): ChannelOpenFee? = withNodeContextCatch {
        // TODO glsdk: greenlight opens channels lazily; once a fee preview API exists, wire it here.
        null
    }

//    suspend fun onchainFeeRates(): LightningFees? = tryCatch(context = Dispatchers.IO) {
//        greenlightSdk.onchainFeeRates()
//    }
//
//    fun onchainBalanceWithdrawableSat(): Long {
//        return when(val state = greenlightSdk.onchainBalanceState()){
//             is OnchainBalanceState.Available -> state.withdrawableSat.toLong()
//             else -> 0
//         }
//    }

    suspend fun receiveOnchain(): OnchainReceiveResponse = withNodeContext {
        greenlightSdk.onchainReceive()
    }

//    suspend fun prepareOnchainSend(toAddress: String, satPerVbyte: UInt?): PreparedOnchainSend = withExceptionHandler {
//        greenlightSdk.prepareOnchainSend(toAddress = toAddress, satPerVbyte = satPerVbyte).also {
//            logger.d { "prepareOnchainSend: $it" }
//        }
//    }

//    fun onchainSend(toAddress: String, satPerVbyte: UInt?, prepared: PreparedOnchainSend): OnchainSendResponse =
//        greenlightSdk.onchainSend(toAddress = toAddress, satPerVbyte = satPerVbyte, prepared = prepared)

    suspend fun onchainSend(toAddress: String): OnchainSendResponse = withNodeContext {
        greenlightSdk.onchainSend(toAddress = toAddress)
    }

    suspend fun onchainPaymentLimits(): OnchainLimits = withNodeContext {
        // TODO glsdk: derive max payable on-chain from Node.listFunds() outputs.
        notYetImplemented("onchainPaymentLimits")
    }

    suspend fun payOnchain(address: String, satPerVbyte: UInt?): Unit = withNodeContext(finallyBlock = {
        // TODO glsdk: refresh reverse-swap state once the equivalent flow is wired.
    }) {
        // TODO glsdk: greenlight has no reverse-swap; either route through Node.onchainSend or remove once UX decides.
        notYetImplemented("payOnchain")
    }

    suspend fun prepareRefund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): PrepareRefundResult = withNodeContext {
        // TODO glsdk: greenlight has no swap-refund flow; revisit when/if swap support lands.
        notYetImplemented("prepareRefund")
    }

    suspend fun refund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): RefundResult = withNodeContext(finallyBlock = {
        // TODO glsdk: refresh swap state when refunds become supported.
    }) {
        notYetImplemented("refund")
    }

    suspend fun fetchReverseSwapFees(sendAmountSat: ULong? = null): ReverseSwapLimits = withNodeContext {
        // TODO glsdk: greenlight has no reverse-swap pricing; the call site needs to be retired or stubbed at a higher layer.
        notYetImplemented("fetchReverseSwapFees")
    }

    suspend fun authLnUrl(requestData: LnUrlAuthData): LnUrlAuthOutcome = withNodeContext {
        // TODO glsdk: implement LNURL-auth signing against greenlight signer (no native helper).
        notYetImplemented("authLnUrl")
    }

    suspend fun payLnUrl(requestData: LnUrlPayData, amount: Long, comment: String): LnUrlPayOutcome = withNodeContext {
        greenlightSdk.payLnUrl(requestData, amount, comment)
    }

    suspend fun withdrawLnUrl(
        requestData: LnUrlWithdrawData,
        amount: Long,
        description: String?
    ): LnUrlWithdrawOutcome = withNodeContext {
        greenlightSdk.withdrawLnUrl(
            requestData = requestData,
            amount = amount,
            description = description
        )
    }

    suspend fun generateDiagnosticData(): String? = withNodeContextCatch {
        greenlightSdk.generateDiagnosticData()
    }

    override fun onEvent(event: NodeEvent) {
        logger.d { "Greenlight onEvent $event" }
        event.toLightningEvent().also { eventSharedFlow.tryEmit(it) }
    }

    private fun notYetImplemented(api: String): Nothing =
        throw Exception("$api is not yet implemented on glsdk")

    private fun exceptionWithNodeId(exception: Exception) =
        Exception(
            listOfNotNull(
                exception.message,
                "NodeId: ${nodeInfoStateFlow.value.id}",
                "Timestamp: ${Clock.System.now().epochSeconds}",
            ).joinToString("\n"),
            exception
        )

    /**
     * Executes [block] on [Dispatchers.Default] and rethrows any failure wrapped with
     * the current node id and timestamp via [exceptionWithNodeId], so call sites
     * surface diagnostic context. [finallyBlock], if provided, always runs after
     * [block] in the same dispatcher — use it for state refreshes that must happen
     * regardless of success.
     */
    private suspend fun <T> withNodeContext(
        finallyBlock: (suspend CoroutineScope.() -> Unit)? = null,
        block: suspend CoroutineScope.() -> T
    ): T {
        return withContext(context = Dispatchers.Default) {
            try {
                block()
            } catch (e: Exception) {
                throw exceptionWithNodeId(e)
            } finally {
                finallyBlock?.also { bl ->
                    bl.invoke(this)
                }
            }
        }
    }

    /**
     * Same as [withNodeContext] but swallows any exception via [tryCatch] and returns
     * `null` on failure. Use when the caller treats absence of a result the same as
     * an error and does not need the wrapped diagnostic exception to propagate.
     */
    private suspend fun <T> withNodeContextCatch(
        finallyBlock: (suspend CoroutineScope.() -> Unit)? = null,
        block: suspend CoroutineScope.() -> T
    ): T? = tryCatch {
        withNodeContext(finallyBlock = finallyBlock, block = block)
    }

    companion object : Loggable() {
        const val GREEN_NOTIFY_PRODUCTION = "https://green-notify.blockstream.com"
        const val GREEN_NOTIFY_DEVELOPMENT = "https://green-notify.dev.blockstream.com"
    }
}