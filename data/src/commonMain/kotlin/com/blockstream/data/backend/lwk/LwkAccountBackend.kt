package com.blockstream.data.backend.lwk

import com.blockstream.data.backend.AbstractAccountBackend
import com.blockstream.data.comparators.ComparatorAssets
import com.blockstream.data.extensions.isPolicyAsset
import com.blockstream.data.extensions.toSortedLinkedHashMap
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Address
import com.blockstream.data.gdk.data.Addressee
import com.blockstream.data.gdk.data.Assets
import com.blockstream.data.gdk.data.Bip21Params
import com.blockstream.data.gdk.data.CreateTransaction
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.gdk.data.Transactions
import com.blockstream.data.gdk.data.UnspentOutputs
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.lwk.LwkAddress
import com.blockstream.data.utils.toHex
import com.blockstream.utils.Loggable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lwk.Chain
import lwk.LwkException
import lwk.Pset
import lwk.Signer
import lwk.WalletTxOut
import lwk.Wollet
import lwk.Address as LwkLibAddress

class LwkAccountBackend constructor(
    val networkBackend: LwkNetworkBackend,
    val networkClient: LwkNetworkClient,
    val wollet: Wollet,
    val signer: Signer,
    val account: Account
) : AbstractAccountBackend(), WaterfallsAccount {
    private var nextAddressIndex: Int = 0

    // A scan can be triggered by a subscription hint and by the poll fallback at the same time;
    // two of them applying an update to the same wollet must not interleave.
    private val scanMutex = Mutex()

    override val id: String
        get() = account.id

    override suspend fun openWaterfallsStream(): WaterfallsStream =
        networkClient.openWaterfallsStream(wollet.descriptor())

    override suspend fun getReceiveAddress(): Address {
        val firstUnusedIndex = wollet.address(null).index().toInt()

        // Hand out addresses past the wallet's first-unused index, but stay
        // within the BIP44 gap limit so we don't generate addresses the
        // wallet won't scan.
        if (nextAddressIndex < firstUnusedIndex || nextAddressIndex - firstUnusedIndex >= GAP_LIMIT) {
            nextAddressIndex = firstUnusedIndex
        }

        val address = wollet.address(nextAddressIndex.toUInt())
        nextAddressIndex++

        logger.d { "Address #${address.index()} ${address.address()}" }

        return LwkAddress(
            address = address.address().toString(),
            index = address.index().toLong()
        )
    }

    override suspend fun getBalance(confirmations: Int): Map<String, Long> {
        return wollet.balance().mapValues {
            it.value.toLong()
        }.also {
            logger.d { "Balance $it" }
            setAssets(
                Assets(
                    it.toSortedLinkedHashMap(
                        ComparatorAssets(
                            networkBackend,
                            networkBackend.networkAssetManager,
                            networkBackend
                        )
                    )
                )
            )
        }
    }

    override suspend fun getTransactions(
        params: TransactionParams
    ): Transactions {
        val txs = wollet.transactionsPaginated(offset = params.offset.toUInt(), limit = params.limit.toUInt()).map { walletTx ->
            Transaction(
                blockHeight = walletTx.height()?.toLong() ?: 0L,
                createdAtTs = walletTx.timestamp()?.toLong()?.let { it * 1_000_000L} ?: 0L,
                inputs = walletTx.inputs().toInputOutputs(isOutput = false),
                outputs = walletTx.outputs().toInputOutputs(isOutput = true),
                fee = walletTx.fee().toLong(),
                feeRate = 0L,
                memo = "",
                spvVerified = "disabled",
                txHash = walletTx.txid().toString(),
                type = walletTx.type(),
                satoshi = walletTx.balance(),
            ).also {
                it.accountInjected = account
                it.confirmationsMaxInjected = it.getConfirmationsMax(networkBackend.blockStateFlow.value.height)
            }
        }
        return Transactions(txs)
    }

    /**
     * Maps the wallet outputs LWK can unblind into [InputOutput]. Entries that are not ours
     * (and therefore can't be unblinded) come through as null and are skipped.
     */
    private fun List<WalletTxOut?>.toInputOutputs(isOutput: Boolean): List<InputOutput> =
        mapNotNull { it?.toInputOutput(isOutput = isOutput) }

    private fun WalletTxOut.toInputOutput(isOutput: Boolean): InputOutput {
        val secrets = unblinded()
        val isInternal = extInt() == Chain.INTERNAL
        return InputOutput(
            address = address().toString(),
            assetId = secrets.asset(),
            satoshi = secrets.value().toLong(),
            amountblinder = secrets.valueBf(), // value blinding factor
            assetblinder = secrets.assetBf(), // asset blinding factor
            isOutput = isOutput,
            isInternal = isInternal,
            isChange = isOutput && isInternal,
            isRelevant = true,
        )
    }

    override suspend fun createTransaction(
        params: CreateTransactionParams
    ): CreateTransaction {
        val addressees = params.addresseesAsParams.orEmpty()
        check(addressees.size <= 1)

        val addressParams = addressees.first()

        // Accept bare addresses as well as BIP21 URIs of the form
        // "liquid:<address>?amount=<coins>&assetid=<hex>". Strip the scheme
        // prefix off the address and lift amount/assetid out of the query string.
        val query = addressParams.address.substringAfter("?", "")
            .split("&")
            .filter { param -> param.contains("=") }
            .associate { param -> param.substringBefore("=") to param.substringAfter("=") }

        val assetId = checkNotNull(query["assetid"] ?: addressParams.assetId) { "Recipient without assetId" }

        val bip21Params = query["amount"]?.let {
            Bip21Params(amount = it, assetId = assetId)
        }

        val address = addressParams.address.substringBefore("?").substringAfter(":")
        val satoshi = query["amount"]?.let(::bip21AmountToSatoshi) ?: addressParams.satoshi

        val createTransaction = CreateTransaction(
            addressees = listOf(
                Addressee(
                    address = address,
                    assetId = assetId,
                    satoshi = satoshi,
                    isGreedy = addressParams.isGreedy,
                    bip21Params = bip21Params
                )
            ),
            feeRate = params.feeRate,
            memo = params.memo,
        )

        return try {
            val builder = networkBackend.lwkNetwork.txBuilder()

            params.feeRate?.let { builder.feeRate(it.toFloat()) }

            if (addressParams.isGreedy && assetId.isPolicyAsset(networkBackend.network)) {
                builder.drainLbtcWallet() // spend all L-BTC inputs
                builder.drainLbtcTo(LwkLibAddress(address))
            } else {
                builder.addRecipient(
                    address = LwkLibAddress(address),
                    satoshi = satoshi.toULong(),
                    asset = assetId
                )
            }

            val pset = builder.finish(wollet)
            val balance = wollet.psetDetails(pset).balance()
            val tx = pset.extractTx()

            val outputs = balance.recipients().map {
                Output(
                    address = it.address()?.toString(),
                    assetId = it.asset(),
                    satoshi = it.value()?.toLong() ?: 0L,
                    isChange = false,
                )
            }

            createTransaction.copy(
                satoshi = balance.balances().mapValues {
                    when (it.key) {
                        networkBackend.network.policyAsset if addressParams.isGreedy -> (wollet.balance()[addressParams.assetId]?.toLong()
                            ?: 0L) - balance.fee().toLong()

                        networkBackend.network.policyAsset -> it.value + balance.fee().toLong() // Remove the lbtc fee from the amount
                        else -> it.value
                    }
                },
                fee = balance.fee().toLong(),
                outputs = outputs,
                transaction = tx.toBytes().toHex(),
                txHash = tx.txid().toString(),
                pset = pset.toString()
            )
        } catch (e: LwkException.Generic) {
            if (e.msg.startsWith("InvalidAmount")) {
                createTransaction.copy(error = "id_invalid_amount")
            } else if (e.msg.startsWith("InsufficientFunds")) {
                createTransaction.copy(error = "id_insufficient_funds")
            } else throw e
        }
    }

    override suspend fun getUnspentOutputs(isBump: Boolean, isExpired: Boolean, expiredAt: Long?): UnspentOutputs {
        return UnspentOutputs()
    }

    override suspend fun getOutputDescriptors(): String = wollet.descriptor().toString()

    override suspend fun signTransaction(createTransaction: CreateTransaction): CreateTransaction {
        requireNotNull(createTransaction.pset)

        val pset = Pset(createTransaction.pset)

        val signedTransaction = signer
            .sign(pset)
            .let {
                if (networkBackend.network.isAmp2) {
                    networkBackend.amp2Client.cosign(it)
                } else {
                    it
                }
            }
            .finalize()

        return createTransaction.copy(
            transaction = signedTransaction.toString()
        )
    }

    override suspend fun updateAccount(name: String?, hidden: Boolean?) {
        networkBackend.updateAccount(account = account, name = name, hidden = hidden)
    }

    // BIP21 amounts are expressed in whole coins with up to 8 decimal places; parse as
    // fixed-point to avoid the rounding that floating point would introduce.
    private fun bip21AmountToSatoshi(amount: String): Long {
        val parts = amount.split(".")
        val whole = parts[0].ifEmpty { "0" }.toLong()
        val fraction = parts.getOrElse(1) { "" }.padEnd(8, '0').take(8).toLong()
        return whole * 100_000_000L + fraction
    }

    override suspend fun scanBlockchain() {
        scanMutex.withLock {
            // A null update means the scan found nothing new, so there is nothing to announce.
            networkClient.fullScanToIndex(wollet = wollet)?.also { update ->
                logger.d { "Update for ${account.id}, onlyTip=${update.onlyTip()}" }
                wollet.applyUpdate(update)
                networkBackend.notifyTransaction(account.pointer)
            }
        }
    }

    companion object : Loggable() {
        private const val GAP_LIMIT = 20
    }
}
