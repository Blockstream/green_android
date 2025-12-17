package com.blockstream.data.lwk

import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.cancelChildren
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapAsset
import com.blockstream.utils.Loggable
import com.github.michaelbull.retry.policy.fullJitterBackoff
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
import lwk.PaymentState
import lwk.PreparePayResponse
import lwk.WebHook
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Lwk(
    val wallet: GreenWallet
) : Logging, KoinComponent {

    private val appInfo: AppInfo by inject()
    private val database: Database by inject()
    private lateinit var boltzSession: BoltzSession

    private val scope = CoroutineScope(context = Dispatchers.IO)

    private val monitor = mutableSetOf<String>()

    private val _isConnected = MutableStateFlow(false)

    val isConnected: Boolean
        get() = _isConnected.value

    private val _invoicePaidSharedFlow =
        MutableSharedFlow<Pair<String, Long?>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val invoicePaidSharedFlow
        get() = _invoicePaidSharedFlow.asSharedFlow()

    fun connect(mnemonic: String, bitcoinAddress: String? = null, liquidAddress: String? = null) {
        scope.launchSafe {
            if (!isConnected) {
                logger.d { "Connect to Boltz using LWK" }
                val network = Network.mainnet()
                val mnemonic = Mnemonic(mnemonic)
                val client = network.defaultElectrumClient()

                tryCatch {
                    val boltzSessionBuilder = BoltzSessionBuilder(
                        network = network,
                        client = AnyClient.fromElectrum(client),
                        timeout = 30.toULong(),
                        mnemonic = mnemonic,
                        referralId = "blockstream",
                        logging = this@Lwk,
                        polling = true,
                        timeoutAdvance = 10.toULong()
                    )

                    retry(fullJitterBackoff(min = 1_000, max = 30_000)) {
                        logger.d { "Trying connection with Boltz" }
                        boltzSession = BoltzSession.fromBuilder(boltzSessionBuilder).also {
                            logger.d { "Connected with Boltz" }
                            onSessionConnect(xPubHashId = wallet.xPubHashId)
                        }

                        // Restore
                        tryCatch {

                            liquidAddress?.let { Address(it) }?.also { liquidAddress ->
                                val list = boltzSession.swapRestore()

                                logger.d { "Restoring swaps, recover address $liquidAddress" }
                                boltzSession.restorableReverseSwaps(list, liquidAddress).forEach { reverseSwap ->
                                    val invoice = boltzSession.restoreInvoice(reverseSwap)

                                    database.setSwap(
                                        id = invoice.swapId(),
                                        walletId = wallet.id,
                                        xPubHashId = wallet.xPubHashId,
                                        swapType = SwapType.ReverseSubmarine,
                                        isAutoSwap = false,
                                        data = invoice.serialize()
                                    )
                                }

                                boltzSession.restorableSubmarineSwaps(list, liquidAddress).forEach { submarineSwap ->
                                    val pay = boltzSession.restorePreparePay(submarineSwap)

                                    database.setSwap(
                                        id = pay.swapId(),
                                        walletId = wallet.id,
                                        xPubHashId = wallet.xPubHashId,
                                        swapType = SwapType.NormalSubmarine,
                                        isAutoSwap = false,
                                        data = pay.serialize()
                                    )
                                }

                                bitcoinAddress?.let { BitcoinAddress(it) }?.also { bitcoinAddress ->
                                    logger.d { "Restoring swaps, recover address $bitcoinAddress" }

                                    (boltzSession.restorableLbtcToBtcSwaps(
                                        list, bitcoinAddress, liquidAddress
                                    ) + boltzSession.restorableBtcToLbtcSwaps(
                                        list, liquidAddress, bitcoinAddress
                                    )).forEach { data ->

                                        val lockup = boltzSession.restoreLockup(data)

                                        database.setSwap(
                                            id = lockup.swapId(),
                                            walletId = wallet.id,
                                            xPubHashId = wallet.xPubHashId,
                                            swapType = SwapType.Chain,
                                            isAutoSwap = false,
                                            data = lockup.serialize()
                                        )
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
            WebHook("${it}webhook/boltz/${wallet.xPubHashId}", listOf())
        }
    }

    suspend fun restorePreparePay(data: String) = ioExceptionHandler {
        boltzSession.restorePreparePay(data)
    }

    // Reverse Submarine Swaps (Lightning -> Chain)
    suspend fun createReverseSubmarineSwap(address: String, amount: Long, description: String?): InvoiceResponse = ioExceptionHandler {
        boltzSession.invoice(amount = amount.toULong(), description = description, claimAddress = Address(address), webhook = webhook())
    }

    // Normal Submarine Swaps (Chain -> Lightning)
    suspend fun createNormalSubmarineSwap(bolt11Invoice: String, refundAddress: String): PreparePayResponse = ioExceptionHandler {

        boltzSession.preparePay(
            lightningPayment = LightningPayment(bolt11Invoice), refundAddress = Address(refundAddress), webhook = webhook()
        )
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
                    delay(10_000)
                    PaymentState.CONTINUE
                } catch (e: LwkException.ObjectConsumed) {
                    // This thors
                    e.printStackTrace()
                    database.setSwapComplete(swapId)
                    PaymentState.FAILED
                } catch (e: Exception) {
                    e.printStackTrace()
                    PaymentState.FAILED
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
                    delay(10_000)
                    PaymentState.CONTINUE
                } catch (e: LwkException.ObjectConsumed) {
                    // This thors
                    e.printStackTrace()
                    database.setSwapComplete(swapId)
                    PaymentState.FAILED
                } catch (e: Exception) {
                    e.printStackTrace()
                    PaymentState.FAILED
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
                            database.setSwapComplete(swapId)
                        }
                    }

                    state
                } catch (_: LwkException.NoBoltzUpdate) {
                    delay(10_000)
                    PaymentState.CONTINUE
                } catch (e: LwkException.ObjectConsumed) {
                    e.printStackTrace()
                    database.setSwapComplete(swapId)
                    PaymentState.FAILED
                } catch (e: Exception) {
                    e.printStackTrace()
                    PaymentState.FAILED
                }

            } while (isActive && state == PaymentState.CONTINUE)
        }
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
        logger.d { "Disconnect Boltz for wallet ${wallet.id}" }
        scope.cancelChildren()
        _isConnected.value = false
        monitor.clear()
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

            throw Exception(cleanMessage, e.cause)
        }
    }

    companion object : Loggable() {
        const val LWK_NETWORK = "lwk-mainnet"
        const val BOLTZ_BIP85_INDEX = 26589L
        private const val SWAP_TYPE_SUBMARINE = "submarine"
        private const val SWAP_TYPE_REVERSE_SUBMARINE = "reverse"
        private const val SWAP_TYPE_CHAIN = "chain"

        const val BASE_URL = "https://green-webhooks.blockstream.com/"
        const val DEV_BASE_URL = "https://green-webhooks.dev.blockstream.com/"
    }
}

private fun SwapAsset.toLwk(): lwk.SwapAsset = when (this) {
    SwapAsset.Bitcoin -> lwk.SwapAsset.ONCHAIN
    SwapAsset.Liquid -> lwk.SwapAsset.LIQUID
    SwapAsset.Lightning -> lwk.SwapAsset.LIGHTNING
}


