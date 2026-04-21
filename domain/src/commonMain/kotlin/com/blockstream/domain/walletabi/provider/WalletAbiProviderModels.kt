package com.blockstream.domain.walletabi.provider

import com.blockstream.data.json.DefaultJson
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class WalletAbiProviderStatus {
    @SerialName("ok")
    OK,

    @SerialName("error")
    ERROR,
}

@Serializable
data class WalletAbiProviderTransactionInfo(
    @SerialName("tx_hex")
    val txHex: String,
    val txid: String,
)

@Serializable
data class WalletAbiProviderErrorInfo(
    val code: String,
    val message: String,
)

@Serializable
data class WalletAbiProviderPreviewAssetDelta(
    @SerialName("asset_id")
    val assetId: String,
    @SerialName("wallet_delta_sat")
    val walletDeltaSat: Long,
)

@Serializable
enum class WalletAbiProviderPreviewOutputKind {
    @SerialName("external")
    EXTERNAL,

    @SerialName("receive")
    RECEIVE,

    @SerialName("change")
    CHANGE,

    @SerialName("fee")
    FEE,
}

@Serializable
data class WalletAbiProviderPreviewOutput(
    val kind: WalletAbiProviderPreviewOutputKind,
    @SerialName("asset_id")
    val assetId: String,
    @SerialName("amount_sat")
    val amountSat: Long,
    @SerialName("script_pubkey")
    val scriptPubkey: String,
)

@Serializable
data class WalletAbiProviderRequestPreview(
    @SerialName("asset_deltas")
    val assetDeltas: List<WalletAbiProviderPreviewAssetDelta>,
    val outputs: List<WalletAbiProviderPreviewOutput>,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class WalletAbiProviderProcessResponse(
    @SerialName("abi_version")
    val abiVersion: String,
    @SerialName("request_id")
    val requestId: String,
    val network: WalletAbiNetwork,
    val status: WalletAbiProviderStatus,
    val transaction: WalletAbiProviderTransactionInfo? = null,
    val artifacts: JsonObject? = null,
    val error: WalletAbiProviderErrorInfo? = null,
)

fun WalletAbiProviderProcessResponse.preview(): WalletAbiProviderRequestPreview? {
    val previewValue = artifacts?.get("preview") ?: return null
    return DefaultJson.decodeFromJsonElement(
        WalletAbiProviderRequestPreview.serializer(),
        previewValue,
    )
}

@Serializable
data class WalletAbiProviderEvaluateResponse(
    @SerialName("abi_version")
    val abiVersion: String,
    @SerialName("request_id")
    val requestId: String,
    val network: WalletAbiNetwork,
    val status: WalletAbiProviderStatus,
    val preview: WalletAbiProviderRequestPreview? = null,
    val error: WalletAbiProviderErrorInfo? = null,
)
