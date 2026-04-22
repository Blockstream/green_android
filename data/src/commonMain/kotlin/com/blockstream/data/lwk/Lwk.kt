package com.blockstream.data.lwk

import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.cancelChildren
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapAsset
import com.blockstream.utils.Loggable
import com.github.michaelbull.retry.policy.fullJitterBackoff
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.policy.stopAtAttempts
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lwk.Address
import lwk.AnyClient
import lwk.BitcoinAddress
import lwk.BoltzSession
import lwk.BoltzSessionBuilder
import lwk.InvoiceResponse
import lwk.LightningPayment
import lwk.LockupResponse
import lwk.LogLevel
import lwk.Logging
import lwk.LwkException
import lwk.Mnemonic
import lwk.Network
import lwk.Payment
import lwk.PaymentKind
import lwk.PaymentState
import lwk.PreparePayResponse
import lwk.WebHook
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Lwk(
    val walletId: String
) : Logging, KoinComponent {

    private lateinit var xPubHashId: String

    private val appInfo: AppInfo by inject()
    private val database: Database by inject()
    private lateinit var boltzSession: BoltzSession

    private val scope = CoroutineScope(context = Dispatchers.IO)

    private val monitor = mutableSetOf<String>()

    // Cache for inspectPaymentInstruction results. Re-parsing is cheap, but LNURL resolution
    // is a network round-trip and the call sites (GetSendAccountsUseCase, IsInvoiceSwappableUseCase,
    // PrepareTransactionUseCase…) hit it many times per user action.
    //
    // We memoize both completed results (`inspectCache`) and in-flight Deferreds (`inspectInFlight`)
    // so concurrent callers for the same input share a single network call instead of racing.
    private val inspectCacheMutex = Mutex()
    private val inspectCache = mutableMapOf<String, PaymentInstruction?>()
    private val inspectInFlight = mutableMapOf<String, Deferred<Result<PaymentInstruction?>>>()

    // BIP-353 resolves to a BOLT12 offer via DNSSEC. The lookup is hot on the action path
    // (inspectPaymentInstruction caches the wrapped Bolt12, but resolveToLightningPayment
    // re-parses from the raw input). Caching the resolved offer string by input avoids a
    // duplicate DNS round-trip on the second hit.
    private val bip353CacheMutex = Mutex()
    private val bip353Cache = mutableMapOf<String, String>()

    private val _isConnected = MutableStateFlow(false)

    val isConnected: Boolean
        get() = _isConnected.value

    private val _invoicePaidSharedFlow =
        MutableSharedFlow<Pair<String, Long?>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val invoicePaidSharedFlow
        get() = _invoicePaidSharedFlow.asSharedFlow()

    fun connect(
        xPubHashId: String,
        mnemonic: String,
        bitcoinAddress: String? = null,
        liquidAddress: String? = null
    ) {

        check(xPubHashId.isNotBlank()) {
            "Wallet should not have a blank xPubHashId"
        }

        this.xPubHashId = xPubHashId

        scope.launchSafe {
            if (!isConnected) {
                logger.d { "Connect to Boltz using LWK" }
                val network = Network.mainnet()
                val mnemonic = Mnemonic(mnemonic)

                val client = try {
                    AnyClient.fromElectrum(network.defaultElectrumClient())
                } catch (e: Exception) {
                    logger.d { "Electrum error: ${e.message}, fallback to Esplora" }
                    AnyClient.fromEsplora(network.defaultEsploraClient())
                }

                tryCatch {
                    val boltzSessionBuilder = BoltzSessionBuilder(
                        network = network,
                        client = client,
                        timeout = 30.toULong(),
                        mnemonic = mnemonic,
                        referralId = "blockstream",
                        logging = this@Lwk,
                        polling = true,
                        timeoutAdvance = 10.toULong(),
                        randomPreimages = true
                    )

                    retry(fullJitterBackoff(min = 1_000, max = 30_000)) {
                        logger.d { "Trying connection with Boltz" }
                        boltzSession = BoltzSession.fromBuilder(boltzSessionBuilder).also {
                            logger.d { "Connected with Boltz" }
                            onSessionConnect(xPubHashId = xPubHashId)
                        }

                        // Restore swaps after connection (only user's lockups)
                        tryCatch {
                            val cloudSwaps = boltzSession.swapRestore()
                            liquidAddress?.let { Address(it) }?.also { liquidAddress ->
                                boltzSession.restorableSubmarineSwaps(cloudSwaps, liquidAddress).forEach { submarineSwap ->
                                    val pay = boltzSession.restorePreparePay(submarineSwap)
                                    val txHash = pay.lockupTxid()

                                    if (txHash != null) {
                                        val swapId = pay.swapId()
                                        database.setSwap(
                                            id = swapId,
                                            walletId = walletId,
                                            xPubHashId = xPubHashId,
                                            swapType = SwapType.NormalSubmarine,
                                            isAutoSwap = false,
                                            isMagic = false,
                                            data = pay.serialize()
                                        )
                                        database.setSwapTxHash(id = swapId, txHash = txHash)
                                    }
                                }

                                bitcoinAddress?.let { BitcoinAddress(it) }?.also { bitcoinAddress ->
                                    val restorableLbtcToBtc =
                                        boltzSession.restorableLbtcToBtcSwaps(cloudSwaps, bitcoinAddress, liquidAddress)
                                    val restorableBtcToLbtc =
                                        boltzSession.restorableBtcToLbtcSwaps(cloudSwaps, liquidAddress, bitcoinAddress)

                                    (restorableLbtcToBtc + restorableBtcToLbtc).forEach { data ->
                                        val lockup = boltzSession.restoreLockup(data)
                                        val txHash = lockup.lockupTxid()

                                        if (txHash != null) {
                                            val swapId = lockup.swapId()
                                            database.setSwap(
                                                id = swapId,
                                                walletId = walletId,
                                                xPubHashId = xPubHashId,
                                                swapType = SwapType.Chain,
                                                isAutoSwap = false,
                                                isMagic = false,
                                                data = lockup.serialize()
                                            )
                                            database.setSwapTxHash(id = swapId, txHash = txHash)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkBoltzSession() {
        check(this::boltzSession.isInitialized) {
            "Boltz session is not yet initialized"
        }
    }

    private fun onSessionConnect(xPubHashId: String) {
        logger.d { "onSessionConnect $xPubHashId" }
        _isConnected.value = true

        database.getPendingSwapsFlow(xPubHashId).onEach { swaps ->
            tryCatch {
                logger.d { "Pending Swaps ${swaps.map { it.id }}" }
                swaps.forEach {
                    handleSwap(swapId = it.id, data = it.data_)
                }
            }
        }.launchIn(scope)
    }

    private suspend fun handleSwap(swapId: String, data: String) {
        checkBoltzSession()

        if (monitor.contains(swapId)) return

        monitor.add(swapId)

        tryCatch {
            val json = Json.parseToJsonElement(data)

            when (val type = json.jsonObject["swap_type"]?.jsonPrimitive?.content) {
                SWAP_TYPE_SUBMARINE -> {
                    logger.d { "Restoring submarine swap {$swapId}..." }
                    val pay = boltzSession.restorePreparePay(data)
                    handlePay(pay)
                }

                SWAP_TYPE_REVERSE_SUBMARINE -> {
                    logger.d { "Restoring reverse submarine swap $swapId..." }
                    val response = boltzSession.restoreInvoice(data)
                    handleInvoice(response)
                }

                SWAP_TYPE_CHAIN -> {
                    logger.d { "Restoring chain swap $swapId..." }
                    val response = boltzSession.restoreLockup(data)
                    handleChainLockup(response)
                }

                else -> {
                    logger.d { "Unknown swap type: $type , $data" }
                    database.deleteSwap(swapId)
                }
            }
        }
    }

    private fun webhook(): WebHook {
        return (if (appInfo.isDevelopment) {
            DEV_BASE_URL
        } else {
            BASE_URL
        }).let {
            WebHook("${it}webhook/boltz/${xPubHashId}".also {
                logger.d { "Webhook: $it" }
            }, listOf())
        }
    }

    suspend fun restorePreparePay(data: String) = ioExceptionHandler {
        boltzSession.restorePreparePay(data)
    }

    // Reverse Submarine Swaps (Lightning -> Chain)
    suspend fun createReverseSubmarineSwap(address: String, amount: Long, description: String?): InvoiceResponse = ioExceptionHandler {
        boltzSession.invoice(amount = amount.toULong(), description = description, claimAddress = Address(address), webhook = webhook())
    }

    /**
     * Parses a pasted/scanned payment instruction and, for LNURL, fetches the server's info
     * (min/max sendable, metadata) in the same call so the UI has everything needed to show
     * the amount-input step.
     *
     * Strips an optional `lightning:` URI prefix once here so all callers share a single
     * cache entry regardless of whether they pre-normalised the input themselves.
     *
     * Returns null for unrecognised input. Throws on network failure for LNURL resolution.
     */
    suspend fun inspectPaymentInstruction(input: String): PaymentInstruction? {
        val key = input.removePrefix("lightning:")

        val deferred = inspectCacheMutex.withLock {
            if (inspectCache.containsKey(key)) {
                return inspectCache[key]
            }

            // Share in-flight work so concurrent callers for the same input wait on one request.
            // The body is wrapped in runCatching so an inspect failure (LNURL 404, BIP-353 NXDOMAIN,
            // network error) doesn't propagate out of `scope.async` and cancel the parent scope —
            // that would tear down unrelated background work like the swap-monitor loops.
            inspectInFlight[key] ?: scope.async(Dispatchers.IO) {
                runCatching { inspectPaymentInstructionUncached(key) }.also { result ->
                    try {
                        if (result.isSuccess) {
                            inspectCacheMutex.withLock { inspectCache[key] = result.getOrNull() }
                        }
                    } finally {
                        inspectCacheMutex.withLock { inspectInFlight.remove(key) }
                    }
                }
            }.also { inspectInFlight[key] = it }
        }

        return deferred.await().getOrThrow()
    }

    private suspend fun inspectPaymentInstructionUncached(input: String): PaymentInstruction? =
        withContext(context = Dispatchers.IO) {
            val parsed = try {
                Payment(input)
            } catch (_: Exception) {
                return@withContext null
            }

            try {
                when (parsed.kind()) {
                    PaymentKind.LIGHTNING_INVOICE -> {
                        val invoice = parsed.lightningInvoice() ?: return@withContext null
                        invoice.use {
                            val amount = it.amountMilliSatoshis()?.let { msat -> (msat / 1000u).toLong() }
                            PaymentInstruction.Bolt11(invoice = input, amountSats = amount)
                        }
                    }

                    PaymentKind.LIGHTNING_OFFER -> {
                        val offer = parsed.lightningOffer() ?: return@withContext null
                        bolt12InstructionFor(offer)
                    }

                    PaymentKind.LN_URL -> {
                        // LNURL resolution is a network round-trip to the lnurlp endpoint.
                        // Transient failures (timeout, DNS, 5xx) are common and self-recover,
                        // so retry with jittered backoff before surfacing to the caller.
                        val info = retry(
                            stopAtAttempts<Throwable>(LNURL_INSPECT_MAX_ATTEMPTS) +
                                fullJitterBackoff(min = 500, max = 3_000)
                        ) {
                            parsed.resolveLnurlInfo()
                        }
                        PaymentInstruction.LnUrl(
                            raw = input,
                            minSats = (info.minSendable / 1000u).toLong(),
                            maxSats = (info.maxSendable / 1000u).toLong(),
                            description = extractLnUrlDescription(info.metadata),
                            metadata = info.metadata,
                        )
                    }

                    PaymentKind.BIP353 -> {
                        // BIP-353 is a human-readable name that resolves via DNSSEC TXT to a
                        // BOLT12 offer. Transparently resolve to Bolt12 here so the rest of
                        // the send flow can route it through the existing BOLT12 path.
                        // User explicitly asked for BIP-353 (via the ₿ prefix), so map every
                        // failure to the BIP-353-specific error rather than the raw LWK message.
                        val offer = try {
                            resolveBip353Offer(input)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            throw Exception("id_failed_to_resolve_bip353_payment_request")
                        }
                        bolt12InstructionFor(offer)
                    }

                    else -> null
                }
            } finally {
                parsed.close()
            }
        }

    /**
     * Resolves a BIP-353 instruction (e.g. `₿matt@mattcorallo.com`) to a BOLT12 offer string.
     *
     * DNSSEC validation happens inside the `bitcoin-payment-instructions` crate. Network
     * resolution can fail transiently (UDP loss, recursive resolver hiccups), so we apply
     * a jittered retry policy. Successful resolutions are cached so the action path can
     * reuse them without a second DNS round-trip.
     */
    private suspend fun resolveBip353Offer(bip353Address: String): String {
        bip353CacheMutex.withLock { bip353Cache[bip353Address] }?.let { return it }

        val offer = withContext(context = Dispatchers.IO) {
            retry(
                stopAtAttempts<Throwable>(BIP353_INSPECT_MAX_ATTEMPTS) +
                    fullJitterBackoff(min = 500, max = 3_000)
            ) {
                val parsed = Payment(bip353Address)
                try {
                    parsed.resolveBip353().use { resolved ->
                        if (resolved.kind() == PaymentKind.BIP353) {
                            throw Exception("id_failed_to_resolve_bip353_payment_request")
                        }
                        resolved.lightningOffer() ?: throw Exception("id_invalid_address")
                    }
                } finally {
                    parsed.close()
                }
            }
        }

        bip353CacheMutex.withLock { bip353Cache[bip353Address] = offer }
        return offer
    }

    /**
     * Full submarine-swap pipeline for any Lightning-payable instruction.
     *
     * Branches on the parsed kind:
     *  - BOLT11: uses the invoice directly.
     *  - BOLT12 amountless: requires `amountSats`; sets it on the offer; Boltz fetches the invoice.
     *  - BOLT12 per-item: requires `itemCount`; sets it on the offer; Boltz fetches the invoice.
     *  - LNURL: requires `amountSats`; resolves to a BOLT11 via LWK, then swaps.
     *  - BIP-353: resolves the human-readable name to a BOLT12 offer, then proceeds as BOLT12.
     *
     * Strips an optional `lightning:` URI prefix once here so the bip353 cache lookup in
     * `resolveBip353Offer` lines up with the key used by `inspectPaymentInstruction`.
     */
    suspend fun createNormalSubmarineSwap(
        input: String,
        refundAddress: String,
        amountSats: Long? = null,
    ): PreparePayResponse = ioExceptionHandler {
        val normalizedInput = input.removePrefix("lightning:")
        val lightningPayment = resolveToLightningPayment(normalizedInput, amountSats)
        try {
            boltzSession.preparePay(
                lightningPayment = lightningPayment,
                refundAddress = Address(refundAddress),
                webhook = webhook(),
            )
        } finally {
            lightningPayment.close()
        }
    }

    private suspend fun resolveToLightningPayment(
        input: String,
        amountSats: Long?,
    ): LightningPayment {
        val parsed = Payment(input)
        try {
            return when (parsed.kind()) {
                PaymentKind.LIGHTNING_INVOICE -> LightningPayment(input)

                PaymentKind.LIGHTNING_OFFER -> {
                    val offer = parsed.lightningOffer()
                        ?: throw Exception("id_invalid_address")
                    buildBolt12LightningPayment(offer, amountSats)
                }

                PaymentKind.LN_URL -> {
                    val amount = amountSats ?: throw Exception("id_invalid_amount")
                    val info = parsed.resolveLnurlInfo()
                    parsed.fetchLnurlInvoice(info, amount.toULong()).use { invoicePayment ->
                        val bolt11 = invoicePayment.lightningInvoice()?.toString()
                            ?: throw Exception("id_invalid_address")
                        LightningPayment(bolt11)
                    }
                }

                PaymentKind.BIP353 -> {
                    val offer = resolveBip353Offer(input)
                    buildBolt12LightningPayment(offer, amountSats)
                }

                else -> throw Exception("id_invalid_address")
            }
        } finally {
            parsed.close()
        }
    }

    private fun buildBolt12LightningPayment(
        offer: String,
        amountSats: Long?,
    ): LightningPayment {
        val lp = LightningPayment(offer)
        try {
            val hasAmount = try { lp.bolt12OfferHasAmount() } catch (_: LwkException) { false }
            if (hasAmount) {
                lp.setBolt12InvoiceAmountViaItems(1u)
            } else {
                val amount = amountSats ?: throw Exception("id_invalid_amount")
                lp.setBolt12InvoiceAmount(amount.toULong())
            }
        } catch (t: Throwable) {
            lp.close()
            throw t
        }
        return lp
    }

    private fun bolt12InstructionFor(offer: String): PaymentInstruction.Bolt12 {
        return LightningPayment(offer).use { lp ->
            val hasAmount = try { lp.bolt12OfferHasAmount() } catch (_: LwkException) { false }
            val amountSats: Long? = if (hasAmount) {
                try {
                    lp.setBolt12InvoiceAmountViaItems(1u)
                    lp.bolt12InvoiceAmount()?.toLong()
                } catch (_: Throwable) { null }
            } else null
            PaymentInstruction.Bolt12(
                offer = offer,
                amountMode = if (hasAmount) Bolt12AmountMode.WITH_AMOUNT else Bolt12AmountMode.AMOUNTLESS,
                amountSats = amountSats,
            )
        }
    }

    suspend fun btcToLbtc(amount: Long, refundAddress: String, claimAddress: String): LockupResponse = ioExceptionHandler {
        boltzSession.btcToLbtc(
            amount = amount.toULong(),
            refundAddress = BitcoinAddress(refundAddress),
            claimAddress = Address(claimAddress),
            webhook = webhook()
        )
    }

    suspend fun lbtcToBtc(amount: Long, refundAddress: String, claimAddress: String): LockupResponse = ioExceptionHandler {
        boltzSession.lbtcToBtc(
            amount = amount.toULong(),
            refundAddress = Address(refundAddress),
            claimAddress = BitcoinAddress(claimAddress),
            webhook = webhook()
        )
    }

    fun handleChainLockup(lockup: LockupResponse) {
        scope.launchSafe {
            val swapId = lockup.swapId()
            logger.d { "Handle lockup $swapId" }
            do {
                val state = try {
                    logger.d { "Handling lockup $swapId" }

                    val state = lockup.advance()

                    logger.d { "Lockup $swapId state: $state" }

                    when (state) {
                        PaymentState.CONTINUE -> {
                            // continue
                        }

                        PaymentState.SUCCESS, PaymentState.FAILED -> {
                            database.setSwapComplete(swapId)
                        }
                    }

                    state
                } catch (_: LwkException.NoBoltzUpdate) {
                    logger.d { "No boltz update for lockup $swapId... waiting" }
                    delay(NO_BOLTZ_UPDATE_DELAY_MS)
                    PaymentState.CONTINUE
                } catch (e: Exception) {
                    handleSwapAdvanceError(swapId, "lockup", e)
                }

            } while (isActive && state == PaymentState.CONTINUE)
        }
    }

    private fun handleInvoice(invoice: InvoiceResponse) {
        scope.launchSafe {
            val swapId = invoice.swapId()
            logger.d { "Handle invoice $swapId" }
            do {
                val state = try {
                    logger.d { "Handling invoice $swapId" }

                    val state = invoice.advance()

                    logger.d { "Invoice $swapId state: $state" }

                    when (state) {
                        PaymentState.CONTINUE -> {
                            // continue
                        }

                        PaymentState.SUCCESS, PaymentState.FAILED -> {
                            if (state == PaymentState.SUCCESS) {
                                _invoicePaidSharedFlow.emit(
                                    invoice.bolt11Invoice().paymentHash() to invoice.bolt11Invoice().amountMilliSatoshis()?.satoshi()
                                )
                            }

                            database.setSwapComplete(swapId)
                        }
                    }

                    state
                } catch (_: LwkException.NoBoltzUpdate) {
                    logger.d { "No boltz update for invoice $swapId... waiting" }
                    delay(NO_BOLTZ_UPDATE_DELAY_MS)
                    PaymentState.CONTINUE
                } catch (e: Exception) {
                    handleSwapAdvanceError(swapId, "invoice", e)
                }

            } while (isActive && state == PaymentState.CONTINUE)
        }
    }

    private fun handlePay(pay: PreparePayResponse) {
        scope.launchSafe {
            val swapId = pay.swapId()
            logger.d { "Handle invoice $swapId" }
            do {
                val state = try {
                    logger.d { "Handling pay $swapId" }

                    val state = pay.advance()

                    logger.d { "Invoice $swapId state: $state" }

                    when (state) {
                        PaymentState.CONTINUE -> {

                        }

                        PaymentState.SUCCESS, PaymentState.FAILED -> {
                            pay.lockupTxid()?.let { txHash ->
                                database.setSwapTxHash(id = swapId, txHash = txHash)
                            }
                            database.setSwapComplete(swapId)
                        }
                    }

                    state
                } catch (_: LwkException.NoBoltzUpdate) {
                    delay(NO_BOLTZ_UPDATE_DELAY_MS)
                    PaymentState.CONTINUE
                } catch (e: Exception) {
                    handleSwapAdvanceError(swapId, "pay", e)
                }

            } while (isActive && state == PaymentState.CONTINUE)
        }
    }

    /**
     * Decide what a swap loop should do when `advance()` throws something other than
     * `NoBoltzUpdate`. Terminal errors (expired swap, 4xx from Boltz, consumed native handle)
     * mark the swap complete and return FAILED. Everything else — 5xx, Electrum disconnects,
     * IO/timeouts, unknown LwkException variants — is treated as transient: we delay and let
     * the loop retry, so an active swap doesn't disappear on a single network blip.
     */
    private suspend fun handleSwapAdvanceError(swapId: String, kind: String, e: Exception): PaymentState {
        e.printStackTrace()
        return if (isTerminalSwapError(e)) {
            logger.d { "Terminal $kind error for $swapId: ${e::class.simpleName} — marking complete" }
            database.setSwapComplete(swapId)
            PaymentState.FAILED
        } else {
            logger.d { "Transient $kind error for $swapId: ${e::class.simpleName} — retrying after backoff" }
            delay(TRANSIENT_RETRY_DELAY_MS)
            PaymentState.CONTINUE
        }
    }

    private fun isTerminalSwapError(e: Throwable): Boolean = when (e) {
        is LwkException.SwapExpired -> true
        is LwkException.ObjectConsumed -> true
        is LwkException.BoltzBackendHttpException -> {
            val code = e.status.toInt()
            // 408 and 429 are retryable, not terminal.
            code in 400..499 && code != 408 && code != 429
        }
        else -> false
    }

    suspend fun refreshSwapInfo(): Unit = ioExceptionHandler {
        boltzSession.refreshSwapInfo()
    }

    suspend fun quote(satoshi: Long, quoteMode: QuoteMode, send: SwapAsset, receive: SwapAsset): Quote = ioExceptionHandler {
        val builder = if (quoteMode.isSend) boltzSession.quote(satoshi.toULong()) else boltzSession.quoteReceive(satoshi.toULong())
        builder.send(send.toLwk());
        builder.receive(receive.toLwk());
        builder.build().let { Quote.from(it) }
    }

    fun disconnect() {
        logger.d { "Disconnect Boltz for wallet $walletId" }
        scope.cancelChildren()
        _isConnected.value = false
        monitor.clear()
        inspectCache.clear()
        inspectInFlight.clear()
    }

    override fun log(level: LogLevel, message: String) {
        if (level != LogLevel.DEBUG) {
            logger.d { "${level.name} - $message" }
        }
    }

    suspend fun <T> ioExceptionHandler(block: suspend () -> T): T {
        checkBoltzSession()

        return try {
            withContext(context = Dispatchers.IO) {
                block()
            }
        } catch (e: LwkException.Generic) {
            val cleanMessage = e.msg.replace("BoltzApi(HTTP(\"\\\"", "").replace("\\\"\"))", "")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            throw Exception(cleanMessage, e)
        }
    }

    companion object : Loggable() {
        const val LWK_NETWORK = "lwk-mainnet"
        const val BOLTZ_BIP85_INDEX = 26589L
        private const val SWAP_TYPE_SUBMARINE = "submarine"
        private const val SWAP_TYPE_REVERSE_SUBMARINE = "reverse"
        private const val SWAP_TYPE_CHAIN = "chain"

        // BIP-353 resolution is a DNSSEC-validated TXT lookup, prone to transient network
        // failures. Retry with jittered backoff. Triggered explicitly when the user types
        // the `₿` prefix; we don't auto-try plain `user@domain` (those are LNURL).
        private const val BIP353_INSPECT_MAX_ATTEMPTS = 3

        // LNURL resolution does an HTTP round-trip to the lnurlp endpoint; we retry on
        // transient failures with jittered backoff before propagating to the caller.
        private const val LNURL_INSPECT_MAX_ATTEMPTS = 3

        // Polling cadence for the legitimate "no Boltz update yet" case in swap loops —
        // expected and frequent, so a short delay is fine.
        private const val NO_BOLTZ_UPDATE_DELAY_MS = 10_000L

        // Backoff for transient unexpected errors in swap loops (5xx, Electrum disconnects,
        // network IO, unknown LwkException variants). Longer than NO_BOLTZ_UPDATE_DELAY_MS
        // so we don't hammer a stressed dependency, short enough to recover quickly when it
        // comes back.
        private const val TRANSIENT_RETRY_DELAY_MS = 30_000L

        const val BASE_URL = "https://green-webhooks.blockstream.com/"
        const val DEV_BASE_URL = "https://green-webhooks.dev.blockstream.com/"
    }
}

private fun SwapAsset.toLwk(): lwk.SwapAsset = when (this) {
    SwapAsset.Bitcoin -> lwk.SwapAsset.ONCHAIN
    SwapAsset.Liquid -> lwk.SwapAsset.LIQUID
    SwapAsset.Lightning -> lwk.SwapAsset.LIGHTNING
}

enum class Bolt12AmountMode {
    AMOUNTLESS,
    WITH_AMOUNT,
}

sealed interface PaymentInstruction {
    data class Bolt11(val invoice: String, val amountSats: Long?) : PaymentInstruction

    data class Bolt12(
        val offer: String,
        val amountMode: Bolt12AmountMode,
        val amountSats: Long? = null,
    ) : PaymentInstruction

    data class LnUrl(
        val raw: String,
        val minSats: Long,
        val maxSats: Long,
        val description: String?,
        val metadata: String,
    ) : PaymentInstruction
}

/** Extracts the `text/plain` entry from an LNURL-pay metadata array. */
private fun extractLnUrlDescription(metadata: String): String? = try {
    Json.parseToJsonElement(metadata).jsonArray
        .asSequence()
        .mapNotNull { it as? JsonArray }
        .firstOrNull { it.size >= 2 && it[0].jsonPrimitive.content == "text/plain" }
        ?.get(1)?.jsonPrimitive?.content
} catch (_: Exception) {
    null
}


