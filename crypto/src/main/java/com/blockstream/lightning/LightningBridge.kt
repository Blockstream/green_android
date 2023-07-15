package com.blockstream.lightning

import android.util.Base64
import breez_sdk.BlockingBreezServices
import breez_sdk.BreezEvent
import breez_sdk.Config
import breez_sdk.EnvironmentType
import breez_sdk.EventListener
import breez_sdk.GreenlightCredentials
import breez_sdk.GreenlightNodeConfig
import breez_sdk.InputType
import breez_sdk.LnInvoice
import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlCallbackStatus
import breez_sdk.LnUrlPayRequestData
import breez_sdk.LnUrlPayResult
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LspInformation
import breez_sdk.Network
import breez_sdk.NodeConfig
import breez_sdk.NodeState
import breez_sdk.Payment
import breez_sdk.PaymentTypeFilter
import breez_sdk.RecommendedFees
import breez_sdk.SwapInfo
import breez_sdk.connect
import breez_sdk.defaultConfig
import breez_sdk.mnemonicToSeed
import breez_sdk.parseInput
import breez_sdk.parseInvoice
import com.blockstream.common.lightning.AppGreenlightCredentials
import com.blockstream.crypto.BuildConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import mu.KLogging
import java.io.File

class LightningBridge constructor(
    val workingDir: File
) : EventListener {

    private var breezSdkOrNull: BlockingBreezServices? = null
    private val breezSdk
        get() = breezSdkOrNull!!
    private val network
        get() = Network.BITCOIN

    // Emulate GreenlightCredentials
    var appGreenlightCredentials: AppGreenlightCredentials? = null

    private val _nodeInfoStateFlow = MutableStateFlow<NodeState>(
        NodeState(
            id = "",
            blockHeight = 0u,
            channelsBalanceMsat = 0u,
            onchainBalanceMsat = 0u,
            utxos = listOf(),
            maxPayableMsat = 0u,
            maxReceivableMsat = 0u,
            maxSinglePaymentAmountMsat = 0u,
            maxChanReserveMsats = 0u,
            connectedPeers = listOf(),
            inboundLiquidityMsats = 0u
        )
    )

    val nodeInfoStateFlow
        get() = _nodeInfoStateFlow.asStateFlow()

    private val _eventSharedFlow = MutableSharedFlow<BreezEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val eventSharedFlow
        get() = _eventSharedFlow.asSharedFlow()

    private val _lspInfoStateFlow = MutableStateFlow<LspInformation?>(null)

    val lspInfoStateFlow
        get() = _lspInfoStateFlow.asStateFlow()

    private val _swapInfoStateFlow = MutableStateFlow<List<Pair<SwapInfo, Boolean>>>(listOf())

    val swapInfoStateFlow
        get() = _swapInfoStateFlow.asStateFlow()

    private fun createConfig(partnerCredentials: GreenlightCredentials?): Config {

        val apiKey = BuildConfig.BREEZ_API_KEY.let { base64 ->
            try{
                String(Base64.decode(base64, Base64.DEFAULT))
            }catch (e: Exception){
                ""
            }
        }

        val nodeConfig = NodeConfig.Greenlight(config = GreenlightNodeConfig(partnerCredentials = partnerCredentials, inviteCode = null))

        return defaultConfig(EnvironmentType.PRODUCTION, apiKey, nodeConfig).also {
            it.workingDir = workingDir.absolutePath
        }
    }

    private var inputTypeCache: Pair<String, InputType>? = null

    var isConnected = false
        private set

    init {
        logger.info { "Lightning SDK: ${workingDir.absoluteFile}" }

        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
    }

    private val greenlightCredentials by lazy {
        if (BuildConfig.GREENLIGHT_DEVICE_CERT.isNotBlank() && BuildConfig.GREENLIGHT_DEVICE_KEY.isNotBlank()) {
            GreenlightCredentials(
                deviceKey = Base64.decode(BuildConfig.GREENLIGHT_DEVICE_KEY, Base64.DEFAULT)
                    .toUByteArray().toTypedArray().toList(),
                deviceCert = Base64.decode(BuildConfig.GREENLIGHT_DEVICE_CERT, Base64.DEFAULT)
                    .toUByteArray().toTypedArray().toList(),
            )
        } else null
    }

    fun connectToGreenlight(mnemonic: String, isRestore: Boolean): Boolean {
        val partnerCredentials = if (isRestore) null else greenlightCredentials
        return start(mnemonic, partnerCredentials)
    }

    private fun start(mnemonic: String, partnerCredentials: GreenlightCredentials?): Boolean {
        if (breezSdkOrNull != null) {
            return true
        }

        try{
            breezSdkOrNull = connect(
                config = createConfig(partnerCredentials),
                seed = mnemonicToSeed(mnemonic),
                listener = this
            )

            appGreenlightCredentials = greenlightCredentials?.let {
                AppGreenlightCredentials.fromGreenlightCredentials(
                    it
                )
            }

            isConnected = true

            updateNodeInfo()

            updateLspInformation()

            return true
        }catch (e: Exception){
            e.printStackTrace()
        }
        return false
    }

    private fun updateLspInformation() {
        try {
            breezSdk.lspId()?.also {
                _lspInfoStateFlow.tryEmit(breezSdk.fetchLspInfo(it).also {
                    logger.debug { "LspInformation: $it" }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNodeInfo(): NodeState? {
        return breezSdk.nodeInfo()?.also {
            logger.debug { "NodeState: $it" }
            _nodeInfoStateFlow.value = it
            // Emit New Block as a way to identify established connection
            _eventSharedFlow.tryEmit(BreezEvent.NewBlock(it.blockHeight))
        }
    }

    fun balance(): Long? {
        if(breezSdkOrNull == null){
            return null
        }
        return updateNodeInfo()?.also {
            logger.debug { "Balance: ${it.channelsBalanceSatoshi()}" }
        }?.channelsBalanceSatoshi()
    }

    fun parseBolt11(bolt11: String): LnInvoice? {
        return try {
            if (bolt11.isNotBlank()) {
                parseInvoice(bolt11)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Cache input type to avoid server communication (LNURL) and propably a change in the requested data
    fun parseBoltOrLNUrlAndCache(input: String): InputType? {
        return (inputTypeCache.takeIf { inputTypeCache?.first == input }?.second
            ?: parseBoltOrLNUrl(input))
    }

    private fun parseBoltOrLNUrl(input: String?): InputType? {
        return try {
            if (!input.isNullOrBlank()) {
                parseInput(input).takeIf {
                    it is InputType.Bolt11 || it is InputType.LnUrlPay || it is InputType.LnUrlWithdraw || it is InputType.LnUrlAuth
                }?.also {
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

    fun getTransactions(): List<Payment>? {
        if(breezSdkOrNull == null){
            return null
        }

        return breezSdk.listPayments(
            filter = PaymentTypeFilter.ALL,
            fromTimestamp = null,
            toTimestamp = null
        ).onEach {
            logger.info { it }
        }.also {
            updateSwapInfo()
        }
    }

    fun createInvoice(satoshi: Long, description: String): LnInvoice {
        return try {
            breezSdk.receivePayment(amountSats = satoshi.toULong(), description = description)
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun receiveOnchain(): SwapInfo? {
        return try {
            breezSdk.receiveOnchain()
        } catch (e: Exception) {
            e.printStackTrace()

            if (e.message?.contains(
                    "Swap in progress was detected".lowercase(),
                    ignoreCase = true
                ) == true
            ) {
                throw Exception("id_swap_in_progress_was_detected")
            }

            throw exceptionWithNodeId(e)
        }
    }

    private fun updateSwapInfo() {
        _swapInfoStateFlow.value = (listOfNotNull(breezSdk.inProgressSwap()).map {
            it to false
        } + breezSdk.listRefundables().map {
            it to true
        })
    }

    fun refund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): String? {
        return try {
            breezSdk.refund(
                swapAddress = swapAddress,
                toAddress = toAddress,
                satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        } finally {
            updateSwapInfo()
        }
    }

    fun sweep(toAddress: String, satPerVbyte: UInt?){
        return try {
            breezSdk.sweep(
                toAddress = toAddress,
                feeRateSatsPerVbyte = satPerVbyte?.toULong() ?: breezSdk.recommendedFees().economyFee.toULong()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        } finally {
            updateNodeInfo()
        }
    }

    fun recommendedFees(): RecommendedFees {
        return breezSdk.recommendedFees()
    }

    fun sendPayment(bolt11: String, satoshi: Long?): Payment {
        return try {
            breezSdk.sendPayment(bolt11, amountSats = satoshi?.toULong())
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun payLnUrl(requestData: LnUrlPayRequestData, amount: Long, comment: String): LnUrlPayResult {
        return try {
            breezSdk.payLnurl(requestData, amount.toULong(), comment)
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun authLnUrl(requestData: LnUrlAuthRequestData): LnUrlCallbackStatus {
        return try {
            breezSdk.lnurlAuth(requestData)
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun withdrawLnurl(
        requestData: LnUrlWithdrawRequestData,
        amount: Long,
        description: String?
    ): LnUrlCallbackStatus {
        return try {
            breezSdk.withdrawLnurl(requestData, amount.toULong(), description)
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun listLisps(): List<LspInformation> {
        return breezSdk.listLsps()
    }

    fun connectLsp(id: String) {
        breezSdk.connectLsp(lspId = id)
    }

    fun lspId(): String? {
        return breezSdk.lspId()
    }

    fun fetchLspInfo(id: String): LspInformation? {
        return breezSdkOrNull?.fetchLspInfo(id)
    }

    fun closeLspChannels(){
        breezSdkOrNull?.closeLspChannels()
    }

    fun stop() {
        try {
            breezSdkOrNull?.disconnect()
            breezSdkOrNull = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        breezSdkOrNull?.close()
    }

    override fun onEvent(e: BreezEvent) {
        logger.info { "Breez onEvent $e" }
        _eventSharedFlow.tryEmit(e)
    }

    private fun exceptionWithNodeId(exception: Exception) =
        Exception("${exception.message}\nNodeId: ${_nodeInfoStateFlow.value.id}\nTimestamp: ${Clock.System.now().epochSeconds}", exception.cause)

    companion object : KLogging()
}