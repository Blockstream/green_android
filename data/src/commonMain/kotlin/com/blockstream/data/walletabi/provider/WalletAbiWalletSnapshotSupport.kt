package com.blockstream.data.walletabi.provider

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.Utxo
import com.blockstream.data.gdk.params.BroadcastTransactionParams
import com.blockstream.data.gdk.params.TransactionParams
import com.blockstream.data.jade.JadeHWWallet
import com.blockstream.network.NetworkResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import lwk.Address
import lwk.AssetBlindingFactor
import lwk.Chain
import lwk.ExternalUtxo
import lwk.LwkException
import lwk.Mnemonic
import lwk.OutPoint
import lwk.Script
import lwk.SecretKey
import lwk.Signer
import lwk.Transaction
import lwk.TxOut
import lwk.TxOutSecrets
import lwk.Txid
import lwk.ValueBlindingFactor
import lwk.WalletAbiBip32DerivationPair
import lwk.WalletAbiBroadcasterCallbacks
import lwk.WalletAbiOutputAllocatorCallbacks
import lwk.WalletAbiPrevoutResolverCallbacks
import lwk.WalletAbiReceiveAddressProviderCallbacks
import lwk.WalletAbiRequestSession
import lwk.WalletAbiSessionFactoryCallbacks
import lwk.WalletAbiWalletOutputRequest
import lwk.WalletAbiWalletOutputTemplate
import lwk.Wollet
import lwk.WolletDescriptor
import lwk.walletAbiBip32DerivationPairFromSigner
import lwk.walletAbiGenerateRequestId
import lwk.walletAbiOutputTemplateFromAddress
import lwk.Network as LwkNetwork
import com.blockstream.data.gdk.data.Transaction as GdkTransaction

private const val WALLET_ABI_HARDENED_BIT_LONG = 0x8000_0000L
private const val WALLET_ABI_HARDENED_BIT_UINT = 0x8000_0000u
private const val WALLET_ABI_XPUB_PAYLOAD_LENGTH = 78
private const val WALLET_ABI_XPUB_PUBLIC_KEY_OFFSET = 45
private const val WALLET_ABI_BASE58_CHECKSUM_LENGTH = 4
private const val WALLET_ABI_BASE58_RADIX = 58
private const val WALLET_ABI_BASE58_ZERO_CHAR = '1'
private const val WALLET_ABI_BASE58_ALPHABET =
    "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

data class WalletAbiIndexedUtxo(
    val account: Account,
    val io: InputOutput,
    val utxo: Utxo,
)

class WalletAbiUtxoIndex {
    private val lock = Any()
    private val entries = mutableMapOf<String, WalletAbiIndexedUtxo>()

    fun replace(nextEntries: Map<String, WalletAbiIndexedUtxo>) {
        synchronized(lock) {
            entries.clear()
            entries.putAll(nextEntries)
        }
    }

    fun get(outpointKey: String): WalletAbiIndexedUtxo? = synchronized(lock) {
        entries[outpointKey]
    }
}

interface WalletAbiWalletSignerSupport {
    val maxWeightToSatisfy: UInt

    fun getBip32DerivationPair(
        account: Account,
        io: InputOutput,
        derivationPath: List<UInt>,
    ): WalletAbiBip32DerivationPair

    fun unblind(
        txOut: TxOut,
        policyAsset: String,
    ): TxOutSecrets
}

private class WalletAbiSoftwareWalletSignerSupport(
    session: GdkSession,
    lwkNetwork: LwkNetwork,
) : WalletAbiWalletSignerSupport {
    private val signer: Signer by lazy {
        val mnemonic = runBlocking {
            session.getCredentials().mnemonic
                ?.takeIf { it.isNotBlank() }
                ?: throw LwkException.Generic("Wallet ABI requires mnemonic credentials")
        }
        Signer(Mnemonic(mnemonic), lwkNetwork)
    }

    private val descriptor: WolletDescriptor by lazy {
        signer.wpkhSlip77Descriptor()
    }

    override val maxWeightToSatisfy: UInt by lazy {
        walletAbiMaxWeightToSatisfy(
            network = lwkNetwork,
            descriptor = descriptor,
        )
    }

    override fun getBip32DerivationPair(
        account: Account,
        io: InputOutput,
        derivationPath: List<UInt>,
    ): WalletAbiBip32DerivationPair {
        return walletAbiBip32DerivationPairFromSigner(signer, derivationPath)
    }

    override fun unblind(
        txOut: TxOut,
        policyAsset: String,
    ): TxOutSecrets {
        if (!txOut.isPartiallyBlinded()) {
            return txOut.toWalletAbiExplicitSecrets(policyAsset)
        }

        val blindingKey = descriptor.deriveBlindingKey(txOut.scriptPubkey())
            ?: throw LwkException.Generic("Wallet ABI failed to derive blinding key for txout")
        return txOut.unblind(blindingKey)
    }
}

class WalletAbiJadeWalletSignerSupport(
    private val jadeWallet: JadeHWWallet,
) : WalletAbiWalletSignerSupport {
    override val maxWeightToSatisfy: UInt = 107u

    override fun getBip32DerivationPair(
        account: Account,
        io: InputOutput,
        derivationPath: List<UInt>,
    ): WalletAbiBip32DerivationPair {
        val masterXpub = jadeWallet.getXpubs(
            network = account.network,
            paths = listOf(emptyList()),
            hwInteraction = null,
        ).single()
        val derivedXpub = jadeWallet.getXpubs(
            network = account.network,
            paths = listOf(derivationPath.map { it.toInt() }),
            hwInteraction = null,
        ).single()
        val fingerprint = jadeWallet.wally.bip32Fingerprint(masterXpub)
            ?: throw LwkException.Generic("Wallet ABI failed to derive Jade master fingerprint")

        return WalletAbiBip32DerivationPair(
            pubkey = derivedXpub.walletAbiXpubPublicKeyHex(),
            keySource = "[$fingerprint]${derivationPath.walletAbiDerivationPathString()}",
        )
    }

    override fun unblind(
        txOut: TxOut,
        policyAsset: String,
    ): TxOutSecrets {
        if (!txOut.isPartiallyBlinded()) {
            return txOut.toWalletAbiExplicitSecrets(policyAsset)
        }

        val blindingKey = SecretKey.fromBytes(
            jadeWallet.getBlindingKey(
                scriptHex = txOut.scriptPubkey().toString(),
                hwInteraction = null,
            ).hexToByteArray(),
        )
        return txOut.unblind(blindingKey)
    }
}

class WalletAbiWalletSnapshotSupport(
    private val session: GdkSession,
    private val primaryAccount: Account,
    private val snapshotAccounts: List<Account>,
    private val lwkNetwork: LwkNetwork,
    private val esploraHttpClient: WalletAbiEsploraHttpClient,
    private val utxoIndex: WalletAbiUtxoIndex = WalletAbiUtxoIndex(),
    private val signerSupport: WalletAbiWalletSignerSupport =
        WalletAbiSoftwareWalletSignerSupport(session, lwkNetwork),
) : WalletAbiSessionFactoryCallbacks,
    WalletAbiPrevoutResolverCallbacks,
    WalletAbiOutputAllocatorCallbacks,
    WalletAbiReceiveAddressProviderCallbacks,
    WalletAbiBroadcasterCallbacks {
    private val txOutCacheLock = Any()
    private val txOutCache = mutableMapOf<String, TxOut>()

    override fun openWalletRequestSession(): WalletAbiRequestSession {
        val spendableUtxos = runBlocking { buildSpendableUtxos() }
        return WalletAbiRequestSession(
            sessionId = walletAbiGenerateRequestId(),
            network = lwkNetwork,
            spendableUtxos = spendableUtxos,
        )
    }

    override fun getBip32DerivationPair(outpoint: OutPoint): WalletAbiBip32DerivationPair? {
        val indexed = runBlocking { indexedUtxoOrNull(outpoint) } ?: return null
        val derivationPath = walletAbiDerivationPath(
            accountDerivationPath = indexed.account.derivationPath,
            io = indexed.io,
        ) ?: throw LwkException.Generic(
            "Wallet ABI derivation path unavailable for outpoint ${outpoint.cacheKey()}",
        )

        return signerSupport.getBip32DerivationPair(
            account = indexed.account,
            io = indexed.io,
            derivationPath = derivationPath,
        )
    }

    override fun unblind(txOut: TxOut): TxOutSecrets {
        return signerSupport.unblind(txOut, primaryAccount.network.policyAsset)
    }

    override fun getTxOut(outpoint: OutPoint): TxOut {
        val outpointKey = outpoint.cacheKey()

        synchronized(txOutCacheLock) {
            txOutCache[outpointKey]?.let { return it }
        }

        val resolvedTxOut = runBlocking {
            indexedUtxoOrNull(outpoint)?.let { indexed ->
                resolveTxOut(indexed)
            } ?: fetchTxOutFromEsplora(outpoint)
        } ?: throw LwkException.Generic("Wallet ABI txout not found for outpoint $outpointKey")

        synchronized(txOutCacheLock) {
            txOutCache[outpointKey] = resolvedTxOut
        }
        return resolvedTxOut
    }

    override fun getWalletOutputTemplate(
        session: WalletAbiRequestSession,
        request: WalletAbiWalletOutputRequest,
    ): WalletAbiWalletOutputTemplate {
        return walletAbiOutputTemplateFromAddress(getSignerReceiveAddress())
    }

    override fun getSignerReceiveAddress(): Address {
        return Address(
            runBlocking {
                session.getReceiveAddress(primaryAccount).address
            },
        )
    }

    override fun broadcastTransaction(tx: Transaction): Txid {
        val details = runBlocking {
            session.broadcastTransaction(
                network = primaryAccount.network,
                broadcastTransaction = BroadcastTransactionParams(transaction = tx.toString()),
            )
        }

        return Txid(details.txHash ?: tx.txid().toString())
    }

    private suspend fun indexedUtxo(outpoint: OutPoint): WalletAbiIndexedUtxo {
        return indexedUtxoOrNull(outpoint)
            ?: throw LwkException.Generic("Wallet ABI wallet UTXO not found for outpoint ${outpoint.cacheKey()}")
    }

    private suspend fun indexedUtxoOrNull(outpoint: OutPoint): WalletAbiIndexedUtxo? {
        val outpointKey = outpoint.cacheKey()
        utxoIndex.get(outpointKey)?.let { return it }

        refreshIndexedUtxos()
        return utxoIndex.get(outpointKey)
    }

    private suspend fun buildSpendableUtxos(): List<ExternalUtxo> {
        return refreshIndexedUtxos().values.map { indexed ->
            indexed.toExternalUtxo(
                txOut = resolveTxOut(indexed),
                maxWeightToSatisfy = signerSupport.maxWeightToSatisfy,
            )
        }
    }

    private suspend fun refreshIndexedUtxos(): Map<String, WalletAbiIndexedUtxo> {
        val nextEntries = buildMap {
            snapshotAccounts.forEach { account ->
                val unspentOutputs = session.getUnspentOutputs(account)
                unspentOutputs.unspentOutputs.values.flatten().forEach { entry ->
                    val io = decode<InputOutput>(entry)
                    val utxo = decode<Utxo>(entry)
                    val indexed = WalletAbiIndexedUtxo(
                        account = account,
                        io = io.normalizeWalletAbi(account),
                        utxo = utxo,
                    )
                    if (isChainSpendable(indexed)) {
                        put(indexed.outpoint().cacheKey(), indexed)
                    }
                }
            }
        }

        utxoIndex.replace(nextEntries)
        return nextEntries
    }

    private suspend fun isChainSpendable(indexed: WalletAbiIndexedUtxo): Boolean {
        if (indexed.io.isSpent == true) {
            return false
        }

        val txid = indexed.outpoint().txidHexOrNull() ?: return true
        val vout = indexed.outpoint().vout().toInt()

        indexed.account.network.walletAbiEsploraApiBaseUrls().forEach { apiBaseUrl ->
            when (val response = esploraHttpClient.getTransactionOutspends(apiBaseUrl, txid)) {
                is NetworkResponse.Success -> {
                    if (walletAbiOutspendIsSpent(response.data, vout)) {
                        return false
                    }
                }

                is NetworkResponse.Error -> Unit
            }
        }

        return true
    }

    private inline fun <reified T> decode(entry: JsonElement): T {
        return try {
            GreenJson.json.decodeFromJsonElement(entry)
        } catch (error: Exception) {
            throw LwkException.Generic("Wallet ABI failed to decode wallet UTXO entry: ${error.message}")
        }
    }

    private suspend fun fetchTxOutFromEsplora(outpoint: OutPoint): TxOut? {
        val txid = outpoint.txidHexOrNull() ?: return null
        val vout = outpoint.vout()

        snapshotAccounts
            .map { it.network }
            .distinctBy { it.id }
            .forEach { network ->
                network.walletAbiEsploraApiBaseUrls().forEach { apiBaseUrl ->
                    val transactionHex = when (val response = esploraHttpClient.getTransactionHex(apiBaseUrl, txid)) {
                        is NetworkResponse.Success -> response.data.trim()
                        is NetworkResponse.Error -> null
                    } ?: return@forEach

                    val transaction = runCatching { Transaction.fromString(transactionHex) }.getOrNull()
                        ?: return@forEach
                    transaction.outputs().getOrNull(vout.toInt())?.let { return it }
                }
            }

        return null
    }

    private suspend fun fetchTxOutFromWalletTransactions(indexed: WalletAbiIndexedUtxo): TxOut? {
        val transactions = session.getTransactions(
            account = indexed.account,
            params = TransactionParams(
                subaccount = indexed.account.pointer,
                limit = 100,
            ),
        ).transactions

        return walletAbiResolveTxOutFromTransactions(
            indexed = indexed,
            transactions = transactions,
        )
    }

    private suspend fun resolveTxOut(indexed: WalletAbiIndexedUtxo): TxOut {
        if (!indexed.io.hasUnblindingData()) {
            indexed.toWalletAbiExplicitTxOutOrNull()?.let { return it }
            fetchTxOutFromWalletTransactions(indexed)?.let { return it }
        }
        fetchTxOutFromEsplora(indexed.outpoint())?.let { return it }
        throw LwkException.Generic("Wallet ABI txout not found for outpoint ${indexed.outpoint().cacheKey()}")
    }
}

internal fun walletAbiResolveTxOutFromTransactions(
    indexed: WalletAbiIndexedUtxo,
    transactions: List<GdkTransaction>,
): TxOut? {
    if (indexed.io.hasUnblindingData()) {
        return null
    }

    val outpoint = indexed.outpoint()
    val txid = outpoint.txidHexOrNull() ?: return null
    val vout = outpoint.vout().toInt()

    val output = transactions
        .firstOrNull { transaction ->
            transaction.txHash.normalizedTxidHexOrNull() == txid
        }
        ?.outputs
        ?.getOrNull(vout)
        ?: return null

    return output.copy(
        satoshi = output.satoshi ?: indexed.io.satoshi ?: indexed.utxo.satoshi,
        assetId = output.assetId?.ifBlank { null }
            ?: indexed.io.assetId?.ifBlank { null }
            ?: indexed.utxo.assetId.ifBlank { null }
            ?: indexed.account.network.policyAsset,
    ).toWalletAbiExplicitTxOutOrNull(indexed.account.network.policyAsset)
}

internal fun walletAbiOutspendIsSpent(
    outspends: List<WalletAbiEsploraOutspend>,
    vout: Int,
): Boolean {
    return outspends.getOrNull(vout)?.spent == true
}

data class WalletAbiOutputDerivation(
    val chain: Chain,
    val wildcardIndex: UInt,
)

fun walletAbiDerivationPath(
    accountDerivationPath: List<Long>?,
    io: InputOutput,
): List<UInt>? {
    io.userPath.toWalletAbiPathOrNull()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }

    val accountPath = accountDerivationPath.toWalletAbiPathOrNull() ?: return null
    val derivation = io.toWalletAbiDerivation()
    return accountPath + derivation.chain.toWalletAbiPathChild() + derivation.wildcardIndex
}

fun InputOutput.toWalletAbiDerivation(): WalletAbiOutputDerivation {
    userPath
        ?.takeIf { it.size >= 2 }
        ?.let { path ->
            val extInt = path[path.lastIndex - 1].toWalletAbiPathIndexOrNull()
            val wildcardIndex = path.last().toWalletAbiPathIndexOrNull()
            if (extInt != null && wildcardIndex != null && extInt <= 1u) {
                return WalletAbiOutputDerivation(
                    chain = if (extInt == 1u) Chain.INTERNAL else Chain.EXTERNAL,
                    wildcardIndex = wildcardIndex,
                )
            }
        }

    return WalletAbiOutputDerivation(
        chain = if (isInternal == true || isChange == true || subtype == 1) {
            Chain.INTERNAL
        } else {
            Chain.EXTERNAL
        },
        wildcardIndex = (pointer ?: 0).coerceAtLeast(0).toUInt(),
    )
}

fun InputOutput.toWalletAbiExplicitTxOutOrNull(policyAsset: String): TxOut? {
    val script = preferredScriptHex()
        ?.let { scriptHex ->
            runCatching { Script(scriptHex) }.getOrNull()
        }
        ?: address
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { candidate ->
                runCatching { Address(candidate).scriptPubkey() }.getOrNull()
            }
        ?: return null

    val assetId = (assetId ?: policyAsset)
        .trim()
        .takeIf { it.isNotEmpty() }
        ?: return null
    val satoshi = satoshi
        ?.takeIf { it >= 0L }
        ?.toULong()
        ?: return null

    return TxOut.fromExplicit(script, assetId, satoshi)
}

fun InputOutput.preferredScriptHex(): String? {
    return sequenceOf(
        scriptPubkey,
        script,
    ).mapNotNull { candidate ->
        candidate
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase()
    }.firstOrNull()
}

fun String?.normalizedTxidHexOrNull(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) {
        return null
    }

    val lowered = raw.lowercase()
    if (lowered.length == 64 && lowered.all { char -> char in '0'..'9' || char in 'a'..'f' }) {
        return lowered
    }

    return WALLET_ABI_TXID_HEX_REGEX.find(lowered)?.groupValues?.get(1)
}

fun OutPoint.txidHexOrNull(): String? {
    return txid().toString().normalizedTxidHexOrNull()
        ?: toString().normalizedTxidHexOrNull()
}

fun OutPoint.cacheKey(): String {
    val txidHex = txidHexOrNull() ?: txid().toString()
    return "$txidHex:${vout()}"
}

private fun InputOutput.normalizeWalletAbi(account: Account): InputOutput {
    return copy(
        txHash = txHash?.ifBlank { null } ?: prevtxhash?.ifBlank { null },
        ptIdx = ptIdx ?: previdx ?: 0L,
        assetId = assetId?.ifBlank { null } ?: account.network.policyAsset,
        satoshi = satoshi,
    )
}

private fun WalletAbiIndexedUtxo.outpoint(): OutPoint {
    val txid = (io.txHash?.ifBlank { null } ?: utxo.txHash).normalizedTxidHexOrNull()
        ?: throw LwkException.Generic("Wallet ABI invalid txid '${io.txHash}'")
    val vout = io.ptIdx ?: utxo.index
    return OutPoint.fromParts(Txid(txid), vout.toUInt())
}

private fun WalletAbiIndexedUtxo.toWalletAbiExplicitTxOutOrNull(): TxOut? {
    val normalizedIo = io.copy(
        satoshi = io.satoshi ?: utxo.satoshi,
        assetId = io.assetId?.ifBlank { null } ?: utxo.assetId.ifBlank { null } ?: account.network.policyAsset,
    )
    return normalizedIo.toWalletAbiExplicitTxOutOrNull(account.network.policyAsset)
}

internal fun WalletAbiIndexedUtxo.toTxOutSecrets(): TxOutSecrets {
    val assetId = (io.assetId?.ifBlank { null } ?: utxo.assetId.ifBlank { null } ?: account.network.policyAsset)
    val value = (io.satoshi ?: utxo.satoshi).coerceAtLeast(0).toULong()
    val assetBlinderHex = io.assetblinder?.trim().orEmpty()
    val valueBlinderHex = io.amountblinder?.trim().orEmpty()

    if (assetBlinderHex.isNotEmpty() && valueBlinderHex.isNotEmpty()) {
        val assetBlinder = runCatching {
            AssetBlindingFactor.fromString(assetBlinderHex)
        }.getOrElse { error ->
            throw LwkException.Generic(
                "Wallet ABI invalid asset blinder for ${utxo.txHash}:${utxo.index}: ${error.message}",
            )
        }
        val valueBlinder = runCatching {
            ValueBlindingFactor.fromString(valueBlinderHex)
        }.getOrElse { error ->
            throw LwkException.Generic(
                "Wallet ABI invalid value blinder for ${utxo.txHash}:${utxo.index}: ${error.message}",
            )
        }

        return TxOutSecrets(
            assetId = assetId,
            assetBf = assetBlinder,
            value = value,
            valueBf = valueBlinder,
        )
    }

    return TxOutSecrets.fromExplicit(
        assetId = assetId,
        value = value,
    )
}

internal fun walletAbiMaxWeightToSatisfy(
    network: LwkNetwork,
    descriptor: WolletDescriptor,
): UInt {
    val wollet = Wollet(network, descriptor, null)
    return try {
        wollet.maxWeightToSatisfy()
    } finally {
        wollet.close()
    }
}

private fun WalletAbiIndexedUtxo.toExternalUtxo(
    txOut: TxOut,
    maxWeightToSatisfy: UInt,
): ExternalUtxo {
    return ExternalUtxo.fromUncheckedData(
        outpoint(),
        txOut,
        toTxOutSecrets(),
        maxWeightToSatisfy,
    )
}

private fun String.walletAbiXpubPublicKeyHex(): String {
    val payload = walletAbiBase58CheckDecode()
    if (payload.size != WALLET_ABI_XPUB_PAYLOAD_LENGTH) {
        throw LwkException.Generic("Wallet ABI invalid xpub payload length ${payload.size}")
    }
    return payload.copyOfRange(
        WALLET_ABI_XPUB_PUBLIC_KEY_OFFSET,
        WALLET_ABI_XPUB_PAYLOAD_LENGTH,
    ).toHexString()
}

private fun List<UInt>.walletAbiDerivationPathString(): String {
    return joinToString("/") { child ->
        val hardened = child >= WALLET_ABI_HARDENED_BIT_UINT
        val index = if (hardened) {
            child - WALLET_ABI_HARDENED_BIT_UINT
        } else {
            child
        }
        if (hardened) {
            "$index'"
        } else {
            index.toString()
        }
    }
}

private fun String.walletAbiBase58CheckDecode(): ByteArray {
    val raw = walletAbiBase58Decode()
    if (raw.size < WALLET_ABI_BASE58_CHECKSUM_LENGTH) {
        throw LwkException.Generic("Wallet ABI invalid base58check payload")
    }
    return raw.copyOf(raw.size - WALLET_ABI_BASE58_CHECKSUM_LENGTH)
}

private fun String.walletAbiBase58Decode(): ByteArray {
    var output = byteArrayOf()
    for (char in this) {
        var carry = WALLET_ABI_BASE58_ALPHABET.indexOf(char)
        if (carry < 0) {
            throw LwkException.Generic("Wallet ABI invalid base58 character '$char'")
        }
        for (index in output.indices.reversed()) {
            val value = (output[index].toInt() and 0xff) * WALLET_ABI_BASE58_RADIX + carry
            output[index] = value.toByte()
            carry = value shr 8
        }
        while (carry > 0) {
            output = byteArrayOf((carry and 0xff).toByte()) + output
            carry = carry shr 8
        }
    }

    val leadingZeros = takeWhile { it == WALLET_ABI_BASE58_ZERO_CHAR }.length
    return ByteArray(leadingZeros) + output.dropWhile { it == 0.toByte() }.toByteArray()
}

private fun TxOut.toWalletAbiExplicitSecrets(policyAsset: String): TxOutSecrets {
    val assetId = when {
        isFee() -> policyAsset
        else -> asset()
    } ?: policyAsset
    val explicitValue = value()
        ?: throw LwkException.Generic("Wallet ABI explicit txout amount is unavailable")

    return TxOutSecrets.fromExplicit(
        assetId = assetId,
        value = explicitValue,
    )
}

private fun List<Long>?.toWalletAbiPathOrNull(): List<UInt>? {
    return this?.mapNotNull { child -> child.toWalletAbiPathUIntOrNull() }
        ?.takeIf { it.size == this.size }
}

private fun Long.toWalletAbiPathUIntOrNull(): UInt? {
    return when {
        this < 0L -> null
        this <= UInt.MAX_VALUE.toLong() -> toUInt()
        else -> null
    }
}

private fun Long.toWalletAbiPathIndexOrNull(): UInt? {
    val normalized = if (this >= WALLET_ABI_HARDENED_BIT_LONG) {
        this - WALLET_ABI_HARDENED_BIT_LONG
    } else {
        this
    }
    return normalized.takeIf { it in 0..UInt.MAX_VALUE.toLong() }?.toUInt()
}

private fun Chain.toWalletAbiPathChild(): UInt = when (this) {
    Chain.EXTERNAL -> 0u
    Chain.INTERNAL -> 1u
}

private val WALLET_ABI_TXID_HEX_REGEX = Regex("([0-9a-f]{64})")
