package com.blockstream.data.walletabi.flow

import com.blockstream.data.json.DefaultJson
import com.blockstream.data.managers.WalletSettingsManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

class WalletAbiFlowSnapshotStore(
    private val walletSettingsManager: WalletSettingsManager
) {
    suspend fun load(walletId: String): WalletAbiFlowSnapshotPayload? {
        return walletSettingsManager.getWalletAbiFlowSnapshot(walletId)?.let { raw ->
            DefaultJson.decodeFromString<WalletAbiFlowSnapshotPayload>(raw)
        }
    }

    suspend fun save(walletId: String, snapshot: WalletAbiFlowSnapshotPayload) {
        walletSettingsManager.setWalletAbiFlowSnapshot(
            walletId = walletId,
            snapshot = DefaultJson.encodeToString(snapshot)
        )
    }

    suspend fun clear(walletId: String) {
        walletSettingsManager.clearWalletAbiFlowSnapshot(walletId)
    }
}

@Serializable
data class WalletAbiFlowSnapshotPayload(
    val review: WalletAbiFlowReviewPayload,
    val phase: String,
    val jade: WalletAbiJadeContextPayload? = null
)

@Serializable
data class WalletAbiFlowReviewPayload(
    val requestId: String,
    val walletId: String,
    val title: String,
    val message: String,
    val accounts: List<WalletAbiAccountOptionPayload>,
    val selectedAccountId: String?,
    val approvalTarget: WalletAbiApprovalTargetPayload,
    val parsedRequest: WalletAbiParsedRequestPayload? = null
)

@Serializable
data class WalletAbiAccountOptionPayload(
    val accountId: String,
    val name: String
)

@Serializable
data class WalletAbiApprovalTargetPayload(
    val kind: String,
    val deviceName: String? = null,
    val deviceId: String? = null
)

@Serializable
data class WalletAbiJadeContextPayload(
    val deviceId: String?,
    val step: String,
    val message: String?,
    val retryable: Boolean
)

@Serializable
data class WalletAbiParsedRequestPayload(
    val kind: String,
    val txCreate: WalletAbiTxCreateRequestPayload? = null
)

@Serializable
data class WalletAbiTxCreateRequestPayload(
    val abiVersion: String,
    val requestId: String,
    val network: String,
    val params: WalletAbiRuntimeParamsPayload,
    val broadcast: Boolean
)

@Serializable
data class WalletAbiRuntimeParamsPayload(
    val inputs: List<WalletAbiInputPayload>,
    val outputs: List<WalletAbiOutputPayload>,
    val feeRateSatKvb: Float? = null,
    val lockTime: JsonElement? = null
)

@Serializable
data class WalletAbiInputPayload(
    val id: String,
    val utxoSource: JsonElement,
    val unblinding: JsonElement,
    val sequence: Long,
    val issuance: JsonElement? = null,
    val finalizer: JsonElement
)

@Serializable
data class WalletAbiOutputPayload(
    val id: String,
    val amountSat: Long,
    val lock: JsonElement,
    val asset: JsonElement,
    val blinder: JsonElement
)
