package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.DateSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.greenaddress.greenapi.data.TransactionData
import com.greenaddress.greenapi.data.TwoFactorStatusData
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
@Parcelize
data class Transaction(
    @SerialName("addressees") val addressees: List<String>,
    @SerialName("block_height") val blockHeight: Long,
    @SerialName("can_cpfp") val canCPFP: Boolean,
    @SerialName("can_rbf") val canRBF: Boolean,

    @Serializable(with = DateSerializer::class)
    @SerialName("created_at") val createdAt: Date,

    @SerialName("inputs") val inputs: List<InputOutput>,
    @SerialName("outputs") val outputs: List<InputOutput>,

    @SerialName("fee") val fee: Long,
    @SerialName("fee_rate") val feeRate: Long,

    @SerialName("has_payment_request") val hasPaymentRequest: Boolean,
    @SerialName("instant") val instant: Boolean,
    @SerialName("memo") val memo: String,
    @SerialName("rbf_optin") val rbfOptin: Boolean,

    @SerialName("server_signed") val serverSigned: Boolean,

    @SerialName("spv_verified") val spvVerified: String,
    @SerialName("transaction_size") val txSize: Long,
    @SerialName("transaction_vsize") val txVSize: Long,
    @SerialName("transaction_weight") val txWeight: Long,

    @SerialName("txhash") val txHash: String,

    @SerialName("type") val type: String,
    @SerialName("user_signed") val userSigned: Boolean,

    @SerialName("satoshi") val satoshi: Map<String, Long>
): Parcelable {

    enum class SPVResult {
        Disabled, InProgress, NotVerified, NotLongest, Unconfirmed, Verified;

        fun disabledOrVerified() = this == Disabled || this == Verified
        fun inProgress() = this == InProgress
        fun inProgressOrUnconfirmed() = this == InProgress || this == Unconfirmed
        fun unconfirmed() = this == Unconfirmed
        fun failed() = this == NotVerified || this == NotLongest
    }

    enum class Type {
        OUT, IN, REDEPOSIT
    }

    val txType: Type
        get() = when (type) {
            "outgoing" -> Type.OUT
            "incoming" -> Type.IN
            "redeposit" -> Type.REDEPOSIT
            else -> Type.OUT
        }

    val isIn
        get() = txType == Type.IN

    val isRedeposit
        get() = txType == Type.REDEPOSIT

    val isOut
        get() = txType == Type.OUT

    val spv: SPVResult by lazy{
        when (spvVerified) {
            "in_progress" -> SPVResult.InProgress
            "not_verified" -> SPVResult.NotVerified
            "not_longest" -> SPVResult.NotLongest
            "unconfirmed" -> SPVResult.Unconfirmed
            "verified" -> SPVResult.Verified
            else -> SPVResult.Disabled
        }
    }

    fun assets(network: Network): List<BalancePair> = satoshi.toSortedMap { o1, o2 ->
        if (o1 == network.policyAsset) -1 else o1.compareTo(o2)
    }.mapNotNull {
        if (network.isLiquid && it.key == network.policyAsset && txType == Type.OUT && satoshi.size > 1) {
            // Remove to display fee as amount in liquid
            null
        } else {
            it.toPair()
        }
    }

    fun getConfirmations(currentBlock: Long): Long{
        if (blockHeight == 0L || currentBlock == 0L) return 0
        return currentBlock - blockHeight + 1
    }


    @Deprecated("REMOVE")
    private val objectMapper by lazy { ObjectMapper() }
    fun getTransactionDataV3() : TransactionData {
        return objectMapper.treeToValue(
            objectMapper.readTree(Json.encodeToString(this)),
            TransactionData::class.java
        )
    }

    fun isLoadingTransaction() = blockHeight == -1L

    companion object {
        // Create a dummy transaction to describe the loading state (blockHeight == -1)
        val LoadingTransaction = Transaction(
            addressees = listOf(),
            blockHeight = -1,
            canCPFP = false,
            canRBF = false,
            createdAt = Date(),
            inputs = listOf(),
            outputs = listOf(),
            fee = 0,
            feeRate = 0,
            hasPaymentRequest = false,
            instant = false,
            memo = "",
            rbfOptin = false,
            serverSigned = false,
            spvVerified = "",
            txSize = 0,
            txVSize = 0,
            txWeight = 0,
            txHash = "",
            type = "",
            userSigned = false,
            satoshi = mapOf()
        )
    }
}