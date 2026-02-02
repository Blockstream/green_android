@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.data.lightning

import breez_sdk.Config
import breez_sdk.ConnectRequest
import breez_sdk.EnvironmentType
import breez_sdk.EventListener
import breez_sdk.GreenlightCredentials
import breez_sdk.GreenlightNodeConfig
import breez_sdk.NodeConfig
import breez_sdk.defaultConfig
import breez_sdk.mnemonicToSeed
import breez_sdk.parseInput
import com.blockstream.data.config.AppInfo
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.platformFileSystem
import com.blockstream.data.platformName
import com.blockstream.glsdk.Credentials
import com.blockstream.glsdk.Node
import com.blockstream.glsdk.Signer
import com.blockstream.utils.Loggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

enum class ConnectStatus {
    Connect, NoNode, Failed
}

class LightningSdk constructor(
    private val appInfo: AppInfo,
    val workingDir: String,
    private val greenlightKeys: GreenlightKeys,
    private val firebase: FcmCommon,
) : EventListener {

    private var greenlightSdkOrNull: GreenlightSdk? = null
    private val greenlightSdk
        get() = greenlightSdkOrNull!!

    private var breezSdkOrNull: BreezSdk? = null
    private val breezSdk
        get() = breezSdkOrNull!!

    val nodeInfoStateFlow: StateFlow<LightningNodeState>
        field = MutableStateFlow(LightningNodeState.Default)

    val healthCheckStatus: StateFlow<LightningHealthStatus>
        field = MutableStateFlow(LightningHealthStatus.OPERATIONAL)

    val eventSharedFlow: SharedFlow<LightningEvent>
        field = MutableSharedFlow<LightningEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val lspInfoStateFlow = MutableStateFlow<breez_sdk.LspInformation?>(null)

    val swapInfoStateFlow: StateFlow<List<Pair<LightningSwapInfo, Boolean>>>
        field = MutableStateFlow<List<Pair<LightningSwapInfo, Boolean>>>(listOf())

    val reverseSwapInfoStateFlow: StateFlow<List<LightningReverseSwapInfo>>
        field = MutableStateFlow<List<LightningReverseSwapInfo>>(listOf())

    private fun createConfig(partnerCredentials: GreenlightCredentials?): Config {
        val nodeConfig = NodeConfig.Greenlight(config = GreenlightNodeConfig(partnerCredentials = partnerCredentials, inviteCode = null))
        return defaultConfig(EnvironmentType.PRODUCTION, greenlightKeys.breezApiKey, nodeConfig).also {
            it.workingDir = workingDir
        }
    }

    private var inputTypeCache: Pair<String, LightningInputType>? = null

    val isConnected: Boolean
        get() = greenlightSdkOrNull != null

    private var nodeCredentials: ByteArray? = null

    init {
        logger.i { "Lightning SDK: $workingDir" }
        workingDir.toPath().also { path ->
            if (!platformFileSystem().exists(path)) {
                platformFileSystem().createDirectories(path, mustCreate = true)
            }
        }
    }

    suspend fun getNodeCredentials(mnemonic: String): ByteArray {
        nodeCredentials?.also {
            return it
        }

        return GreenlightSdk.restoreCredentials(mnemonic, greenlightKeys)
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
            val storedCredentials = mnemonicAndCredentials.credentials?.takeIf { it.isNotEmpty() }

            nodeCredentials = when {
                // Stored credentials available — use directly
                storedCredentials != null -> storedCredentials
                // No credentials and not restoring — register a new node (falls back to restore)
                !isRestore -> GreenlightSdk.restoreOrRegisterNode(mnemonicAndCredentials.mnemonic, greenlightKeys)
                // No credentials — recover from scheduler
                else -> GreenlightSdk.restoreCredentials(mnemonicAndCredentials.mnemonic, greenlightKeys)
            }

            logger.i { "Connecting to breez-sdk" }
            breezSdkOrNull = BreezSdk(withTimeout(if (quickResponse) 5000L else 30000L) {
                breez_sdk.connect(
                    req = ConnectRequest(
                        config = createConfig(partnerCredentials = greenlightKeys.toGreenlightCredentials()),
                        seed = mnemonicToSeed(mnemonicAndCredentials.mnemonic),
                        restoreOnly = mnemonicAndCredentials.credentials == null
                    ),
                    listener = this@LightningSdk
                )
            })

            parentXpubHashId?.also { registerNotificationWebhook(it) }

            updateNodeInfo()

            updateLspInformation()

            logger.i { "Connecting to glsdk" }
            withTimeout(if (quickResponse) 5000L else 30000L) {
                val signer = Signer(mnemonicAndCredentials.mnemonic)
                val signerHandle = signer.authenticate(Credentials.load(nodeCredentials!!)).start()
                greenlightSdkOrNull = GreenlightSdk(Node(Credentials.load(nodeCredentials!!)), signerHandle)
            }

            return ConnectStatus.Connect
        } catch (e: com.blockstream.glsdk.Exception.Other) {
            e.printStackTrace()
            return if (e.v1.contains("Recovery failed: no rows returned")) {
                ConnectStatus.NoNode
            } else {
                ConnectStatus.Failed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ConnectStatus.Failed
        }
    }

    private suspend fun registerNotificationWebhook(xpubHashId: String) {
        try {
            firebase.token?.also { token ->
                (if (appInfo.isDevelopment) GREEN_NOTIFY_DEVELOPMENT else GREEN_NOTIFY_PRODUCTION).let { backend ->
                    "$backend/api/v1/notify?platform=${platformName()}&token=$token&app_data=$xpubHashId"
                }.also { url ->
                    logger.i { "Registering webhook for wallet($xpubHashId) as $url" }
                    breezSdkOrNull?.registerWebhook(url)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateLspInformation() {
        try {
            lspInfoStateFlow.tryEmit(breezSdk.updateLspInformation().also {
                logger.d { "LspInformation: $it" }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNodeInfo(): LightningNodeState {
        return breezSdk.nodeInfo().toLightningNodeState().also {
            logger.d { "NodeState: $it" }
            nodeInfoStateFlow.value = it
            // Emit New Block as a way to identify established connection
            eventSharedFlow.tryEmit(LightningEvent.NewBlock(it.blockHeight))

            // serviceHealthCheck()
        }
    }

    fun sync() {
        breezSdkOrNull?.sync()
    }

    fun balanceOnChannel(): Long? {
        if (breezSdkOrNull == null) {
            return null
        }

        return try {
            updateNodeInfo().channelsBalanceSatoshi().also {
                logger.d { "Balance (channel+onchain): $it" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun balanceCombined(): Long? {
        if (breezSdkOrNull == null) {
            return null
        }

        return try {
            updateNodeInfo().let {
                it.channelsBalanceSatoshi() + it.onchainBalanceSatoshi()
            }.also {
                logger.d { "Balance (channel+onchain): $it" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Cache input type to avoid server communication (LNURL) and probably a change in the requested data
    fun parseBoltOrLNUrlAndCache(input: String): LightningInputType? {
        return (inputTypeCache.takeIf { inputTypeCache?.first == input }?.second
            ?: parseBoltOrLNUrl(input))
    }

    private fun parseBoltOrLNUrl(input: String?): LightningInputType? {
        return try {
            if (!input.isNullOrBlank()) {
                parseInput(input).toLightningInputType()?.also {
                    inputTypeCache = input to it
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTransactions(): List<LightningPayment>? {
        if (breezSdkOrNull == null) {
            return null
        }

        return try {
            // Update swap transactions
            updateSwapInfo()

            // Update reverse swap transactions
            updateReverseSwapInfo()

            breezSdkOrNull?.listPayments()?.also {
                logger.d { "Payments: $it" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun rescanSwaps() = tryCatch {
        breezSdk.rescanSwaps()
    }

    suspend fun openChannelFee(satoshi: Long): ChannelOpenFee? = tryCatch {
        breezSdk.openChannelFee(satoshi).also {
            logger.d { "openChannelFee: $it" }
        }
    }

    suspend fun createInvoice(satoshi: Long, description: String): LightningReceivePayment = withExceptionHandler {
        logger.i { "Creating a $satoshi sats invoice - NodeId: ${nodeInfoStateFlow.value.id}" }
        greenlightSdk.createInvoice(satoshi = satoshi, description = description)
    }

    suspend fun receiveOnchain(): LightningSwapInfo = withExceptionHandler {
        breezSdk.receiveOnchain().also {
            logger.d { "receiveOnchain $it" }
        }
    }

    private fun updateSwapInfo() {
        swapInfoStateFlow.value = (listOfNotNull(breezSdkOrNull?.inProgressSwap()).map {
            it to false
        } + (breezSdkOrNull?.listRefundables() ?: emptyList()).map {
            it to true
        }).also {
            logger.d { "updateSwapInfo $it" }
        }
    }

    private fun updateReverseSwapInfo() {
        reverseSwapInfoStateFlow.value = (breezSdkOrNull?.inProgressOnchainPayments() ?: emptyList()).also {
            logger.d { it.joinToString { it.toString() } }
        }
    }

    @Deprecated("Usually throws Fetch status failed: status: PermissionDenied, message: \"Not authorized\"")
    private fun serviceHealthCheck() = try {
        healthCheckStatus.value = breez_sdk.serviceHealthCheck(apiKey = greenlightKeys.breezApiKey)
            .status.toLightningHealthStatus().also {
                logger.d { "ServiceHealthCheck: ${it}" }
            }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    suspend fun prepareRefund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): PrepareRefundResult = withExceptionHandler {
        breezSdk.prepareRefund(swapAddress, toAddress, satPerVbyte)
    }

    suspend fun prepareRedeemOnchainFunds(toAddress: String, satPerVbyte: UInt?): PrepareRedeemOnchainResult = withExceptionHandler {
        breezSdk.prepareRedeemOnchainFunds(toAddress, satPerVbyte)
    }

    suspend fun refund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): RefundResult = withExceptionHandler(finallyBlock = {
        updateSwapInfo()
    }) {
        breezSdk.refund(swapAddress, toAddress, satPerVbyte)
    }

    suspend fun redeemOnchainFunds(toAddress: String, satPerVbyte: UInt?): RedeemOnchainResult = withExceptionHandler(finallyBlock = {
        updateNodeInfo()
    }) {
        breezSdk.redeemOnchainFunds(toAddress, satPerVbyte)
    }

    suspend fun recommendedFees(): LightningFees? = tryCatch(context = Dispatchers.IO) {
        breezSdkOrNull?.recommendedFees()
    }

    fun onchainPaymentLimits(): OnchainLimits {
        return breezSdk.onchainPaymentLimits().also {
            logger.d { "onchainPaymentLimits: $it" }
        }
    }

    fun fetchReverseSwapFees(sendAmountSat: ULong? = null): ReverseSwapLimits {
        return breezSdk.fetchReverseSwapFees(sendAmountSat).also {
            logger.d { "fetchReverseSwapFees: $it" }
        }
    }

    suspend fun payOnchain(address: String, satPerVbyte: UInt?) = withExceptionHandler(finallyBlock = {
        updateReverseSwapInfo()
    }) {
        breezSdk.payOnchain(address, satPerVbyte)
    }

    suspend fun sendPayment(invoice: LightningInvoice, satoshi: Long?): SendPaymentResult = withExceptionHandler {
        logger.i { "Send payment of $satoshi sats - Invoice: ${invoice.bolt11} - NodeId: ${nodeInfoStateFlow.value.id}" }
        breezSdk.sendPayment(invoice, satoshi)
    }

    suspend fun payLnUrl(requestData: LnUrlPayData, amount: Long, comment: String): LnUrlPayOutcome = withExceptionHandler {
        breezSdk.payLnUrl(requestData, amount, comment)
    }

    suspend fun authLnUrl(requestData: LnUrlAuthData): LnUrlAuthOutcome = withExceptionHandler {
        breezSdk.authLnUrl(requestData)
    }

    suspend fun withdrawLnUrl(
        requestData: LnUrlWithdrawData,
        amount: Long,
        description: String?
    ): LnUrlWithdrawOutcome = withExceptionHandler {
        breezSdk.withdrawLnUrl(requestData, amount, description)
    }

    suspend fun generateDiagnosticData(): String? = tryCatch {
        breezSdk.generateDiagnosticData()
    }

    suspend fun stop() {
        greenlightSdkOrNull?.disconnect()
        greenlightSdkOrNull = null

        breezSdkOrNull?.disconnect()
        breezSdkOrNull = null

        nodeCredentials = null
    }

    override fun onEvent(e: breez_sdk.BreezEvent) {
        logger.d { "Breez onEvent $e" }
        e.toLightningEvent()?.also { eventSharedFlow.tryEmit(it) }
    }

    private fun exceptionWithNodeId(exception: Exception) =
        Exception(
            listOfNotNull(
                exception.message,
                "NodeId: ${nodeInfoStateFlow.value.id}",
                "Timestamp: ${Clock.System.now().epochSeconds}",
            ).joinToString("\n"),
            exception.cause
        )

    private suspend fun <T> withExceptionHandler(
        finallyBlock: (suspend CoroutineScope.() -> Unit)? = null,
        block: suspend CoroutineScope.() -> T
    ): T {
        return try {
            withContext(context = Dispatchers.IO) {
                block()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        } finally {
            finallyBlock?.also { bl ->
                withContext(context = Dispatchers.IO) {
                    bl.invoke(this)
                }
            }
        }
    }

    companion object : Loggable() {
        const val GREEN_NOTIFY_PRODUCTION = "https://green-notify.blockstream.com"
        const val GREEN_NOTIFY_DEVELOPMENT = "https://green-notify.dev.blockstream.com"
    }
}
