package com.blockstream.common.lightning

import breez_sdk.BlockingBreezServices
import breez_sdk.BreezEvent
import breez_sdk.Config
import breez_sdk.ConfigureNodeRequest
import breez_sdk.ConnectRequest
import breez_sdk.EnvironmentType
import breez_sdk.EventListener
import breez_sdk.GreenlightCredentials
import breez_sdk.GreenlightNodeConfig
import breez_sdk.HealthCheckStatus
import breez_sdk.InputType
import breez_sdk.ListPaymentsRequest
import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlCallbackStatus
import breez_sdk.LnUrlPayRequest
import breez_sdk.LnUrlPayRequestData
import breez_sdk.LnUrlPayResult
import breez_sdk.LnUrlWithdrawRequest
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LnUrlWithdrawResult
import breez_sdk.LspInformation
import breez_sdk.MaxReverseSwapAmountResponse
import breez_sdk.NodeConfig
import breez_sdk.NodeState
import breez_sdk.OpenChannelFeeRequest
import breez_sdk.OpenChannelFeeResponse
import breez_sdk.OpeningFeeParams
import breez_sdk.Payment
import breez_sdk.PrepareRedeemOnchainFundsRequest
import breez_sdk.PrepareRedeemOnchainFundsResponse
import breez_sdk.PrepareRefundRequest
import breez_sdk.PrepareRefundResponse
import breez_sdk.ReceiveOnchainRequest
import breez_sdk.ReceivePaymentRequest
import breez_sdk.ReceivePaymentResponse
import breez_sdk.RecommendedFees
import breez_sdk.RedeemOnchainFundsRequest
import breez_sdk.RedeemOnchainFundsResponse
import breez_sdk.RefundRequest
import breez_sdk.RefundResponse
import breez_sdk.ReportIssueRequest
import breez_sdk.ReportPaymentFailureDetails
import breez_sdk.ReverseSwapFeesRequest
import breez_sdk.ReverseSwapInfo
import breez_sdk.ReverseSwapPairInfo
import breez_sdk.ReverseSwapStatus
import breez_sdk.SdkException
import breez_sdk.SendOnchainRequest
import breez_sdk.SendOnchainResponse
import breez_sdk.SendPaymentRequest
import breez_sdk.SendPaymentResponse
import breez_sdk.SwapInfo
import breez_sdk.connect
import breez_sdk.defaultConfig
import breez_sdk.mnemonicToSeed
import breez_sdk.parseInput
import co.touchlab.kermit.Logger
import com.blockstream.common.data.AppInfo
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.platformFileSystem
import com.blockstream.common.platformName
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath

class LightningBridge constructor(
    private val appInfo: AppInfo,
    val workingDir: String,
    private val greenlightKeys: GreenlightKeys,
    private val firebase: FcmCommon,
    private val lightningManager: LightningManager,
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
            pendingOnchainBalanceMsat = 0u,
            utxos = listOf(),
            maxPayableMsat = 0u,
            maxReceivableMsat = 0u,
            maxSinglePaymentAmountMsat = 0u,
            maxChanReserveMsats = 0u,
            connectedPeers = listOf(),
            inboundLiquidityMsats = 0u,
        )
    )

    val nodeInfoStateFlow
        get() = _nodeInfoStateFlow.asStateFlow()

    private val _healthCheckStatus = MutableStateFlow<HealthCheckStatus>(HealthCheckStatus.OPERATIONAL)

    val healthCheckStatus
        get() = _healthCheckStatus.asStateFlow()

    private val _eventSharedFlow = MutableSharedFlow<BreezEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val eventSharedFlow
        get() = _eventSharedFlow.asSharedFlow()

    private val _lspInfoStateFlow = MutableStateFlow<LspInformation?>(null)

    val lspInfoStateFlow
        get() = _lspInfoStateFlow.asStateFlow()

    private val _swapInfoStateFlow = MutableStateFlow<List<Pair<SwapInfo, Boolean>>>(listOf())
    private val _reverseSwapInfoStateFlow = MutableStateFlow<List<ReverseSwapInfo>>(listOf())

    val swapInfoStateFlow
        get() = _swapInfoStateFlow.asStateFlow()

    val reverseSwapInfoStateFlow
        get() = _reverseSwapInfoStateFlow.asStateFlow()

    private fun createConfig(partnerCredentials: GreenlightCredentials?): Config {

        val nodeConfig = NodeConfig.Greenlight(config = GreenlightNodeConfig(partnerCredentials = partnerCredentials, inviteCode = null))

        return defaultConfig(EnvironmentType.PRODUCTION, greenlightKeys.breezApiKey, nodeConfig).also {
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

    @NativeCoroutinesIgnore
    suspend fun connectToGreenlight(mnemonic: String, parentXpubHashId: String? = null, restoreOnly: Boolean = true, quickResponse: Boolean = false): Boolean? {
        if (breezSdkOrNull != null) {
            return true
        }

        try {
            breezSdkOrNull = withTimeout(if(quickResponse) 5000L else 30000L) {
                connect(
                    req = ConnectRequest(
                        config = createConfig(greenlightKeys.toGreenlightCredentials()),
                        seed = mnemonicToSeed(mnemonic),
                        restoreOnly = restoreOnly
                    ),
                    listener = this@LightningBridge
                )
            }

            appGreenlightCredentials = greenlightKeys.toGreenlightCredentials()?.let {
                AppGreenlightCredentials.fromGreenlightCredentials(
                    it
                )
            }

            isConnected = true

            parentXpubHashId?.also { registerNotifications(it) }

            updateNodeInfo()

            updateLspInformation()

            return true
        } catch (e: SdkException) {
            e.printStackTrace()

            // SdkException for not registered node
            // Failed to initialize the SDK: Failed to connect to Greenlight: status: Internal,
            // message: "Unable to register node: not authorized: an invite code or a partner certificate is require to register a new node (see https://bit.ly/glinvites for details"
            return if(e.message?.lowercase()?.contains("register node") == true) {
                false
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun registerNotifications(xpubHashId: String) {
        try {
            firebase.token?.also { token ->
                (if (appInfo.isDevelopment) GREEN_NOTIFY_DEVELOPMENT else GREEN_NOTIFY_PRODUCTION).let { backend ->
                    "$backend/api/v1/notify?platform=${platformName()}&token=$token&app_data=$xpubHashId"
                }.also { url ->
                    logger.d { "Registering webhook for wallet($xpubHashId) as $url" }
                    breezSdkOrNull?.registerWebhook(url)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

            serviceHealthCheck()
        }
    }

    fun sync(){
        breezSdkOrNull?.sync()
    }

    fun balance(): Long? {
        if(breezSdkOrNull == null){
            return null
        }

        return try {
            updateNodeInfo().channelsBalanceSatoshi().also {
                Logger.d { "Balance: $it" }
            }
        }catch (e: Exception){
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
        if (breezSdkOrNull == null) {
            return null
        }

        return try {
            // Update swap transactions
            updateSwapInfo()

            // Update reverse swap transactions
            updateReverseSwapInfo()

            breezSdkOrNull?.listPayments(
                ListPaymentsRequest()
            )?.also {
              logger.d { "Payments: $it" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rescanSwaps() {
        try {
            breezSdk.rescanSwaps()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun receiveOnchain(request: ReceiveOnchainRequest = ReceiveOnchainRequest()): SwapInfo {
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
        } ?: emptyList())).also {
            logger.d { "updateSwapInfo $it" }
        }
    }

    private fun updateReverseSwapInfo(){
        _reverseSwapInfoStateFlow.value = breezSdkOrNull?.inProgressReverseSwaps().also {
            it?.also {
                logger.d { it.joinToString { it.toString() } }
            }
        }?.filter { it.status == ReverseSwapStatus.INITIAL || it.status == ReverseSwapStatus.IN_PROGRESS } ?: emptyList()
    }

    private fun serviceHealthCheck() = try {
        _healthCheckStatus.value = breez_sdk.serviceHealthCheck(apiKey = greenlightKeys.breezApiKey).status.also {
            logger.d { "ServiceHealthCheck: ${it}" }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    fun reportIssue(paymentHash: String){
        try {
            val report = ReportIssueRequest.PaymentFailure(ReportPaymentFailureDetails(paymentHash, null))
            breezSdk.reportIssue(report)
        } catch (e: Exception){
          e.printStackTrace()
        }
    }

    fun prepareRefund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): PrepareRefundResponse {
        return try {
            breezSdk.prepareRefund(
                PrepareRefundRequest(
                    swapAddress = swapAddress,
                    toAddress = toAddress,
                    satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        }
    }

    fun prepareRedeemOnchainFunds(toAddress: String, satPerVbyte: UInt?): PrepareRedeemOnchainFundsResponse {
        return try {
            breezSdk.prepareRedeemOnchainFunds(
                PrepareRedeemOnchainFundsRequest(
                    toAddress = toAddress,
                    satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        }
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

    fun redeemOnchainFunds(toAddress: String, satPerVbyte: UInt?): RedeemOnchainFundsResponse {
        return try {
            breezSdk.redeemOnchainFunds(
                RedeemOnchainFundsRequest(
                    toAddress = toAddress,
                    satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw exceptionWithNodeId(e)
        } finally {
            updateNodeInfo()
        }
    }

    @NativeCoroutinesIgnore
    suspend fun recommendedFees(): RecommendedFees = withContext(context = Dispatchers.IO) {
        breezSdk.recommendedFees()
    }

    fun maxReverseSwapAmount(): MaxReverseSwapAmountResponse {
        return breezSdk.maxReverseSwapAmount().also {
            logger.d { "maxReverseSwapAmount: $it" }
        }
    }

    fun fetchReverseSwapFees(req: ReverseSwapFeesRequest): ReverseSwapPairInfo {
        return breezSdk.fetchReverseSwapFees(req).also {
                logger.d { "fetchReverseSwapFees: $it" }
            }
    }

    fun sendOnchain(address: String, satPerVbyte: UInt?): SendOnchainResponse {
        return try {
            val amount = maxReverseSwapAmount().totalSat
            val fee = breezSdk.fetchReverseSwapFees(ReverseSwapFeesRequest(sendAmountSat = amount))

            breezSdk.sendOnchain(
                SendOnchainRequest(
                    amountSat = amount,
                    onchainRecipientAddress = address,
                    pairHash = fee.feesHash,
                    satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
                )
            )
        } catch (e: Exception) {
            throw exceptionWithNodeId(e)
        } finally {
            updateReverseSwapInfo()
        }
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

    fun withdrawLnUrl(
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

    fun setCloseToAddress(closeToAddress: String) {
        logger.i { "Setting closeToAddress" }
        try {
            breezSdk.configureNode(ConfigureNodeRequest(closeToAddress = closeToAddress))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateDiagnosticData(): String? {
        return try {
            breezSdk.generateDiagnosticData()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    fun release(){
        lightningManager.release(this)
    }

    companion object: Loggable() {
        const val GREEN_NOTIFY_PRODUCTION = "https://green-notify.blockstream.com"
        const val GREEN_NOTIFY_DEVELOPMENT = "https://green-notify.dev.blockstream.com"
    }
}