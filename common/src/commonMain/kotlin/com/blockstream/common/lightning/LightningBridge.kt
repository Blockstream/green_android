package com.blockstream.common.lightning

import breez_sdk.BlockingBreezServices
import breez_sdk.BreezEvent
import breez_sdk.Config
import breez_sdk.EnvironmentType
import breez_sdk.EventListener
import breez_sdk.GreenlightCredentials
import breez_sdk.GreenlightNodeConfig
import breez_sdk.InputType
import breez_sdk.ListPaymentsRequest
import breez_sdk.LnInvoice
import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlCallbackStatus
import breez_sdk.LnUrlPayRequest
import breez_sdk.LnUrlPayRequestData
import breez_sdk.LnUrlPayResult
import breez_sdk.LnUrlWithdrawRequest
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LnUrlWithdrawResult
import breez_sdk.LspInformation
import breez_sdk.NodeConfig
import breez_sdk.NodeState
import breez_sdk.OpenChannelFeeRequest
import breez_sdk.OpenChannelFeeResponse
import breez_sdk.OpeningFeeParams
import breez_sdk.Payment
import breez_sdk.PaymentTypeFilter
import breez_sdk.ReceiveOnchainRequest
import breez_sdk.ReceivePaymentRequest
import breez_sdk.ReceivePaymentResponse
import breez_sdk.RecommendedFees
import breez_sdk.RefundRequest
import breez_sdk.RefundResponse
import breez_sdk.SendPaymentRequest
import breez_sdk.SendPaymentResponse
import breez_sdk.SwapInfo
import breez_sdk.SweepRequest
import breez_sdk.SweepResponse
import breez_sdk.connect
import breez_sdk.defaultConfig
import breez_sdk.mnemonicToSeed
import breez_sdk.parseInput
import breez_sdk.parseInvoice
import co.touchlab.kermit.Logger
import com.blockstream.common.platformFileSystem
import com.blockstream.common.utils.Loggable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath

class LightningBridge constructor(
    val workingDir: String,
    val greenlightKeys: GreenlightKeys,
) : EventListener {

    private var breezSdkOrNull: BlockingBreezServices? = null
    private val breezSdk
        get() = breezSdkOrNull!!

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

        val nodeConfig = NodeConfig.Greenlight(config = GreenlightNodeConfig(partnerCredentials = partnerCredentials, inviteCode = null))

        return defaultConfig(EnvironmentType.PRODUCTION, greenlightKeys.apiKey, nodeConfig).also {
            it.workingDir = workingDir
        }
    }

    private var inputTypeCache: Pair<String, InputType>? = null

    var isConnected = false
        private set

    init {
        logger.i { "Lightning SDK: $workingDir" }
        workingDir.toPath().also { path ->
            if(!platformFileSystem().exists(path)){
                platformFileSystem().createDirectories(path, mustCreate = true)
            }
        }
    }

    fun connectToGreenlight(mnemonic: String, checkCredentials: Boolean): Boolean {
        val partnerCredentials = if (checkCredentials) null else greenlightKeys.toGreenlightCredentials()
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

            appGreenlightCredentials = greenlightKeys.toGreenlightCredentials()?.let {
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
                    Logger.d { "LspInformation: $it" }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNodeInfo(): NodeState {
        return breezSdk.nodeInfo().also {
            Logger.d { "NodeState: $it" }
            _nodeInfoStateFlow.value = it
            // Emit New Block as a way to identify established connection
            _eventSharedFlow.tryEmit(BreezEvent.NewBlock(it.blockHeight))
        }
    }

    fun sync(){
        breezSdkOrNull?.sync()
    }

    fun balance(): Long? {
        if(breezSdkOrNull == null){
            return null
        }
        return updateNodeInfo().also {
            Logger.d { "Balance: ${it.channelsBalanceSatoshi()}" }
        }.channelsBalanceSatoshi()
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

        // Update swap transactions
        updateSwapInfo()

        return breezSdkOrNull?.listPayments(
            ListPaymentsRequest(
                filter = PaymentTypeFilter.ALL
            )
        )
    }

    fun openChannelFee(satoshi: Long): OpenChannelFeeResponse? {
        return try {
            breezSdk.openChannelFee(OpenChannelFeeRequest(amountMsat = satoshi.toULong() * 1000u)).also {
                logger.d { "openChannelFee: $it" }
            }
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    fun createInvoice(satoshi: Long, description: String, openingFeeParams: OpeningFeeParams? = null): ReceivePaymentResponse {
        return try {
            breezSdk.receivePayment(
                ReceivePaymentRequest(
                    amountMsat = satoshi.milliSatoshi(),
                    description = description,
                    openingFeeParams = openingFeeParams
                )
            ).also {
                logger.d { "receivePayment: $it" }
            }
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun receiveOnchain(request: ReceiveOnchainRequest = ReceiveOnchainRequest()): SwapInfo? {
        return try {
            breezSdk.receiveOnchain(request).also {
                logger.d { "receiveOnchain $it" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        }
    }

    private fun updateSwapInfo() {
        _swapInfoStateFlow.value = (listOfNotNull(breezSdkOrNull?.inProgressSwap()).map {
            it to false
        } + (breezSdkOrNull?.listRefundables()?.map {
            it to true
        } ?: emptyList()))
    }

    fun refund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): RefundResponse {
        return try {
            breezSdk.refund(
                RefundRequest(
                    swapAddress = swapAddress,
                    toAddress = toAddress,
                    satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        } finally {
            updateSwapInfo()
        }
    }

    fun sweep(toAddress: String, satPerVbyte: UInt?): SweepResponse {
        return try {
            breezSdk.sweep(
                SweepRequest(
                    toAddress = toAddress,
                    feeRateSatsPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
                )
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

    fun sendPayment(bolt11: String, satoshi: Long?): SendPaymentResponse {
        return try {
            breezSdk.sendPayment(
                SendPaymentRequest(bolt11 = bolt11, amountMsat = satoshi?.milliSatoshi())
            )
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        }
    }

    fun payLnUrl(requestData: LnUrlPayRequestData, amount: Long, comment: String): LnUrlPayResult {
        return try {
            breezSdk.payLnurl(
                LnUrlPayRequest(
                    data = requestData, amountMsat = amount.milliSatoshi(), comment = comment
                )
            )
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
    ): LnUrlWithdrawResult {
        return try {
            breezSdk.withdrawLnurl(
                LnUrlWithdrawRequest(
                    data = requestData, amountMsat = amount.milliSatoshi(), description = description
                )
            )
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
        updateNodeInfo()
    }

    fun stop() {
        try {
            breezSdkOrNull?.disconnect()
            breezSdkOrNull = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onEvent(e: BreezEvent) {
        Logger.d { "Breez onEvent $e" }
        _eventSharedFlow.tryEmit(e)
    }

    private fun exceptionWithNodeId(exception: Exception) =
        Exception("${exception.message}\nNodeId: ${_nodeInfoStateFlow.value.id}\nTimestamp: ${Clock.System.now().epochSeconds}", exception.cause)

    companion object: Loggable()
}