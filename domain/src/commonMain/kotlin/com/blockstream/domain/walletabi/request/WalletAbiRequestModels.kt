package com.blockstream.domain.walletabi.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WalletAbiParsedEnvelope(
    val id: JsonElement?,
    val method: WalletAbiMethod,
    val request: WalletAbiParsedRequest
)

@Serializable
enum class WalletAbiMethod(
    val wireValue: String
) {
    @SerialName("wallet_abi_process_request")
    PROCESS_REQUEST("wallet_abi_process_request")
}

@Serializable
sealed interface WalletAbiParsedRequest {
    @Serializable
    data class TxCreate(
        val request: WalletAbiTxCreateRequest
    ) : WalletAbiParsedRequest
}

@Serializable
enum class WalletAbiNetwork(
    val wireValue: String
) {
    @SerialName("liquid")
    LIQUID("liquid"),
    @SerialName("testnet-liquid")
    TESTNET_LIQUID("testnet-liquid"),
    @SerialName("localtest-liquid")
    LOCALTEST_LIQUID("localtest-liquid")
}

@Serializable
data class WalletAbiTxCreateRequest(
    @SerialName("abi_version")
    val abiVersion: String,
    @SerialName("request_id")
    val requestId: String,
    val network: WalletAbiNetwork,
    val params: WalletAbiRuntimeParams,
    val broadcast: Boolean
)

@Serializable
data class WalletAbiRuntimeParams(
    val inputs: List<WalletAbiInput>,
    val outputs: List<WalletAbiOutput>,
    @SerialName("fee_rate_sat_kvb")
    val feeRateSatKvb: Float? = null,
    @SerialName("lock_time")
    val lockTime: JsonElement? = null
)

@Serializable
data class WalletAbiInput(
    val id: String,
    @SerialName("utxo_source")
    val utxoSource: JsonElement,
    val unblinding: JsonElement,
    val sequence: Long,
    val issuance: JsonElement? = null,
    val finalizer: JsonElement
)

@Serializable
data class WalletAbiOutput(
    val id: String,
    @SerialName("amount_sat")
    val amountSat: Long,
    val lock: JsonElement,
    val asset: JsonElement,
    val blinder: JsonElement
)
