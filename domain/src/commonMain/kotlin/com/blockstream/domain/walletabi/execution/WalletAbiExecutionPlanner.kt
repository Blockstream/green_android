package com.blockstream.domain.walletabi.execution

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Network
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import com.blockstream.domain.walletabi.request.WalletAbiOutput
import com.blockstream.domain.walletabi.request.WalletAbiParsedRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import lwk.Script
import lwk.TxOut
import lwk.Network as LwkNetwork

interface WalletAbiExecutionPlanner {
    suspend fun plan(
        session: GdkSession,
        request: WalletAbiParsedRequest,
        selectedAccountId: String? = null
    ): WalletAbiExecutionPlan
}

fun interface WalletAbiOutputAddressResolver {
    fun resolve(
        output: WalletAbiOutput,
        network: WalletAbiNetwork,
        assetId: String
    ): String?
}

sealed interface WalletAbiExecutionPlan {
    val request: WalletAbiParsedRequest.TxCreate
    val accounts: List<Account>
    val selectedAccount: Account
    val feeRate: Long?
    val outputs: List<WalletAbiPlannedOutput>
}

data class WalletAbiPlannedOutput(
    val outputId: String,
    val destinationAddress: String,
    val amountSat: Long,
    val assetId: String,
    val recipientScript: String?
)

data class WalletAbiSinglePaymentPlan(
    override val request: WalletAbiParsedRequest.TxCreate,
    override val accounts: List<Account>,
    override val selectedAccount: Account,
    override val feeRate: Long?,
    val output: WalletAbiPlannedOutput
) : WalletAbiExecutionPlan {
    override val outputs: List<WalletAbiPlannedOutput> = listOf(output)
}

data class WalletAbiSplitPaymentPlan(
    override val request: WalletAbiParsedRequest.TxCreate,
    override val accounts: List<Account>,
    override val selectedAccount: Account,
    override val feeRate: Long?,
    override val outputs: List<WalletAbiPlannedOutput>
) : WalletAbiExecutionPlan {
    init {
        require(outputs.size > 1) { "Split payments require more than one output" }
    }
}

sealed interface WalletAbiExecutionValidationError {
    val message: String

    data object SessionNotConnected : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI requires a connected wallet session"
    }

    data object HardwareWalletUnsupported : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI real execution currently supports software wallets only"
    }

    data object MissingMnemonic : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI real execution requires mnemonic credentials"
    }

    data class UnsupportedRequestType(
        val type: String
    ) : WalletAbiExecutionValidationError {
        override val message: String = "Unsupported Wallet ABI request type '$type'"
    }

    data object BroadcastRequired : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI real execution currently requires broadcast=true"
    }

    data class ExplicitInputsUnsupported(
        val count: Int
    ) : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI real execution does not support explicit inputs, got $count"
    }

    data class OutputCountUnsupported(
        val count: Int
    ) : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI real execution requires at least one output, got $count"
    }

    data object LockTimeUnsupported : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI real execution does not support lock_time"
    }

    data class InvalidFeeRate(
        val feeRateSatKvb: Float
    ) : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI fee rate must be a positive whole number in sats/kvB, got $feeRateSatKvb"
    }

    data class NoEligibleAccount(
        val network: String
    ) : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI found no eligible software Liquid account for '$network'"
    }

    data class SelectedAccountUnavailable(
        val accountId: String
    ) : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI account '$accountId' is unavailable for this request"
    }

    data object MissingAssetId : WalletAbiExecutionValidationError {
        override val message: String = "Wallet ABI output asset_id is required for real execution"
    }

    data class UnsupportedAsset(
        val assetId: String
    ) : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI real execution currently supports the selected account policy asset only, got '$assetId'"
    }

    data object WalletOwnedOutputUnsupported : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI real execution supports external payment outputs only"
    }

    data object OutputLockUnsupported : WalletAbiExecutionValidationError {
        override val message: String =
            "Wallet ABI real execution supports external address or script outputs only"
    }
}

class WalletAbiExecutionValidationException(
    val error: WalletAbiExecutionValidationError
) : IllegalArgumentException(error.message)

class DefaultWalletAbiExecutionPlanner(
    private val outputAddressResolver: WalletAbiOutputAddressResolver = LwkWalletAbiOutputAddressResolver()
) : WalletAbiExecutionPlanner {
    override suspend fun plan(
        session: GdkSession,
        request: WalletAbiParsedRequest,
        selectedAccountId: String?
    ): WalletAbiExecutionPlan {
        requireConnectedSoftwareSession(session)

        val txCreate = request as? WalletAbiParsedRequest.TxCreate
            ?: throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.UnsupportedRequestType(request::class.simpleName ?: "unknown")
            )

        val txRequest = txCreate.request
        if (!txRequest.broadcast) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.BroadcastRequired
            )
        }
        if (txRequest.params.inputs.isNotEmpty()) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.ExplicitInputsUnsupported(txRequest.params.inputs.size)
            )
        }
        if (txRequest.params.outputs.isEmpty()) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.OutputCountUnsupported(txRequest.params.outputs.size)
            )
        }
        if (txRequest.params.lockTime != null) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.LockTimeUnsupported
            )
        }

        val feeRate = txRequest.params.feeRateSatKvb?.toWholePositiveFeeRate()
            ?: txRequest.params.feeRateSatKvb?.let { feeRateSatKvb ->
                throw WalletAbiExecutionValidationException(
                    WalletAbiExecutionValidationError.InvalidFeeRate(feeRateSatKvb)
                )
            }

        val accounts = eligibleAccounts(session = session, requestNetwork = txRequest.network)
        val selectedAccount = selectedAccountId?.let { requestedAccountId ->
            accounts.firstOrNull { it.id == requestedAccountId }
                ?: throw WalletAbiExecutionValidationException(
                    WalletAbiExecutionValidationError.SelectedAccountUnavailable(requestedAccountId)
                )
        } ?: session.activeAccount.value?.let { activeAccount ->
            accounts.firstOrNull { it.id == activeAccount.id }
        } ?: accounts.first()

        val plannedOutputs = txRequest.params.outputs.map { output ->
            if (output.isWalletOwnedOrBurnLike()) {
                throw WalletAbiExecutionValidationException(
                    WalletAbiExecutionValidationError.WalletOwnedOutputUnsupported
                )
            }

            val assetId = output.assetIdOrNull()
                ?: throw WalletAbiExecutionValidationException(
                    WalletAbiExecutionValidationError.MissingAssetId
                )
            if (assetId != selectedAccount.network.policyAsset) {
                throw WalletAbiExecutionValidationException(
                    WalletAbiExecutionValidationError.UnsupportedAsset(assetId)
                )
            }

            val destinationAddress = outputAddressResolver.resolve(
                output = output,
                network = txRequest.network,
                assetId = assetId
            )
                ?: throw WalletAbiExecutionValidationException(
                    WalletAbiExecutionValidationError.OutputLockUnsupported
                )

            WalletAbiPlannedOutput(
                outputId = output.id,
                destinationAddress = destinationAddress,
                amountSat = output.amountSat,
                assetId = assetId,
                recipientScript = output.scriptHexOrNull()
            )
        }

        return when (plannedOutputs.size) {
            1 -> WalletAbiSinglePaymentPlan(
                request = txCreate,
                accounts = accounts,
                selectedAccount = selectedAccount,
                feeRate = feeRate,
                output = plannedOutputs.single()
            )

            else -> WalletAbiSplitPaymentPlan(
                request = txCreate,
                accounts = accounts,
                selectedAccount = selectedAccount,
                feeRate = feeRate,
                outputs = plannedOutputs
            )
        }
    }

    private suspend fun requireConnectedSoftwareSession(session: GdkSession) {
        if (!session.isConnected) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.SessionNotConnected
            )
        }
        if (session.isHardwareWallet) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.HardwareWalletUnsupported
            )
        }

        val mnemonic = runCatching { session.getCredentials().mnemonic }.getOrNull()
        if (mnemonic.isNullOrBlank()) {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.MissingMnemonic
            )
        }
    }

    private suspend fun eligibleAccounts(
        session: GdkSession,
        requestNetwork: WalletAbiNetwork
    ): List<Account> {
        fun currentAccounts() = (session.accounts.value + session.allAccounts.value)
            .distinctBy { account -> account.id }
            .filter { account ->
                account.isLiquid &&
                    !account.isLightning &&
                    account.network.matchesWalletAbiNetwork(requestNetwork)
            }

        val accounts = currentAccounts()
        if (accounts.isNotEmpty()) {
            return accounts
        }

        session.updateAccountsAndBalances(refresh = true).join()
        return currentAccounts().ifEmpty {
            throw WalletAbiExecutionValidationException(
                WalletAbiExecutionValidationError.NoEligibleAccount(requestNetwork.wireValue)
            )
        }
    }
}

private class LwkWalletAbiOutputAddressResolver : WalletAbiOutputAddressResolver {
    override fun resolve(
        output: WalletAbiOutput,
        network: WalletAbiNetwork,
        assetId: String
    ): String? {
        return output.toAddress(network = network, assetId = assetId)
    }
}

private fun WalletAbiOutput.isWalletOwnedOrBurnLike(): Boolean {
    val lockObject = lock as? JsonObject ?: return true
    when (lockObject["type"]?.jsonPrimitive?.content?.trim()?.lowercase()) {
        "script" -> {
            val script = lockObject["script"]?.jsonPrimitive?.content?.trim()?.lowercase()
                ?: return true
            if (script.startsWith("6a")) {
                return true
            }
        }

        "address" -> {
            if (lockObject.addressCandidateOrNull() == null) {
                return true
            }
        }

        else -> return true
    }

    val blinderObject = blinder as? JsonObject
    val blinderType = blinderObject?.get("type")?.jsonPrimitive?.content?.trim()?.lowercase()
        ?: runCatching { blinder.jsonPrimitive.content.trim().lowercase() }.getOrNull()
    return blinderType == "wallet"
}

private fun WalletAbiOutput.assetIdOrNull(): String? {
    val assetObject = asset as? JsonObject ?: return null
    return assetObject["asset_id"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
        ?: assetObject["assetId"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
}

private fun WalletAbiOutput.scriptHexOrNull(): String? {
    val lockObject = lock as? JsonObject ?: return null
    if (lockObject["type"]?.jsonPrimitive?.content?.trim()?.lowercase() != "script") {
        return null
    }

    return lockObject["script"]?.jsonPrimitive?.content?.trim()?.lowercase()
        ?.takeIf { it.isNotBlank() }
}

private fun WalletAbiOutput.toAddress(
    network: WalletAbiNetwork,
    assetId: String
): String? {
    val lockObject = lock as? JsonObject ?: return null
    return when (lockObject["type"]?.jsonPrimitive?.content?.trim()?.lowercase()) {
        "address" -> lockObject.addressCandidateOrNull()

        "script" -> {
            val scriptHex = scriptHexOrNull()
                ?: return null

            runCatching {
                TxOut.fromExplicit(
                    scriptPubkey = Script(scriptHex),
                    assetId = assetId,
                    satoshi = amountSat.toULong()
                ).unconfidentialAddress(
                    network.toLwkNetwork()
                )?.toString()?.trim()?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }

        else -> null
    }
}

private fun JsonObject.addressCandidateOrNull(): String? {
    return sequenceOf(
        this["address"].jsonStringOrNull(),
        this["recipient"].nestedAddressCandidateOrNull(),
        this["recipient_address"].jsonStringOrNull(),
        this["recipientAddress"].jsonStringOrNull()
    ).firstOrNull { !it.isNullOrBlank() }?.trim()
}

private fun JsonElement?.nestedAddressCandidateOrNull(): String? {
    return when (this) {
        is JsonObject -> sequenceOf(
            this["address"].jsonStringOrNull(),
            this["confidential_address"].jsonStringOrNull(),
            this["confidentialAddress"].jsonStringOrNull(),
            this["unconfidential_address"].jsonStringOrNull(),
            this["unconfidentialAddress"].jsonStringOrNull()
        ).firstOrNull { !it.isNullOrBlank() }

        is JsonArray -> asSequence()
            .mapNotNull { it.nestedAddressCandidateOrNull() }
            .firstOrNull()

        else -> jsonStringOrNull()
    }?.trim()
}

private fun JsonElement?.jsonStringOrNull(): String? {
    return runCatching { (this as? JsonPrimitive)?.content }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun WalletAbiNetwork.toLwkNetwork(): LwkNetwork {
    return when (this) {
        WalletAbiNetwork.LIQUID -> LwkNetwork.mainnet()
        WalletAbiNetwork.TESTNET_LIQUID -> LwkNetwork.testnet()
        WalletAbiNetwork.LOCALTEST_LIQUID -> LwkNetwork.regtestDefault()
    }
}

private fun Float.toWholePositiveFeeRate(): Long? {
    if (this <= 0f) {
        return null
    }

    val wholeNumber = toLong()
    return wholeNumber.takeIf { wholeNumber.toFloat() == this }
}

private fun Network.matchesWalletAbiNetwork(requestNetwork: WalletAbiNetwork): Boolean {
    return when (requestNetwork) {
        WalletAbiNetwork.LIQUID -> isLiquidMainnet
        WalletAbiNetwork.TESTNET_LIQUID -> isLiquidTestnet && !isDevelopment
        WalletAbiNetwork.LOCALTEST_LIQUID -> isLiquid && isDevelopment
    }
}
