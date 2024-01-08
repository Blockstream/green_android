package com.blockstream.common.gdk.data


import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.gdk.GreenJson
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue

@Serializable
@Parcelize
data class Transaction(
    var accountInjected: Account? = null,
    @SerialName("block_height") val blockHeight: Long,
    @SerialName("can_cpfp") val canCPFP: Boolean,
    @SerialName("can_rbf") val canRBF: Boolean,
    @SerialName("created_at_ts") val createdAtTs: Long,
    @SerialName("inputs") val inputs: List<InputOutput>,
    @SerialName("outputs") val outputs: List<InputOutput>,
    @SerialName("fee") val fee: Long,
    @SerialName("fee_rate") val feeRate: Long,
    @SerialName("memo") val memo: String,
    @SerialName("rbf_optin") val rbfOptin: Boolean,
    @SerialName("spv_verified") val spvVerified: String,
    @SerialName("txhash") val txHash: String,
    @SerialName("type") val type: String,
    @SerialName("satoshi") val satoshi: Map<String, Long>,
    @SerialName("message") val message: String? = null,
    @SerialName("plaintext") val plaintext: Pair<String,String>? = null,
    @SerialName("url") val url: Pair<String,String>? = null,
    @SerialName("isPendingCloseChannel") val isPendingCloseChannel: Boolean = false,
    @SerialName("isLightningSwap") val isLightningSwap: Boolean = false,
    @SerialName("isLightningReverseSwap") val isLightningReverseSwap: Boolean = false,
    @SerialName("isInProgressSwap") val isInProgressSwap: Boolean = false,
    @SerialName("isRefundableSwap") val isRefundableSwap: Boolean = false
) : GreenJson<Transaction>(), Parcelable {
    val account
        get() = accountInjected!!

    val network
        get() = account.network

    override fun kSerializer() = serializer()

    enum class SPVResult {
        Disabled, InProgress, NotVerified, NotLongest, Unconfirmed, Verified;

        fun disabledOrVerified() = this == Disabled || this == Verified
        fun disabledOrUnconfirmedOrVerified() = this == Disabled || this == Unconfirmed || this == Verified
        fun inProgress() = this == InProgress
        fun inProgressOrUnconfirmed() = this == InProgress || this == Unconfirmed
        fun unconfirmed() = this == Unconfirmed
        fun failed() = this == NotVerified || this == NotLongest
    }

    enum class Type(val gdkType: String) {
        OUT("outgoing"), IN("incoming"), REDEPOSIT("redeposit"), MIXED("mixed"), UNKNOWN("unknown");

        companion object {
            fun from(gdkType: String) = when (gdkType) {
                OUT.gdkType -> OUT
                IN.gdkType -> IN
                REDEPOSIT.gdkType -> REDEPOSIT
                MIXED.gdkType -> MIXED
                else -> UNKNOWN
            }
        }
    }

    @IgnoredOnParcel
    val createdAtInstant: Instant by lazy { Instant.fromEpochMilliseconds(createdAtTs / 1000) }

    val txType: Type
        get() = Type.from(type)

    val isIn
        get() = txType == Type.IN

    val isOut
        get() = txType == Type.OUT

    val isRedeposit
        get() = txType == Type.REDEPOSIT

    val isMixed
        get() = txType == Type.MIXED

    val satoshiPolicyAsset: Long
        get() = satoshi[BTC_POLICY_ASSET] ?: 0L

    @IgnoredOnParcel
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

    @IgnoredOnParcel
    val utxoViews : List<UtxoView> by lazy {
        if (account.isLightning) {
            return@lazy listOf(
                UtxoView(
                    address = inputs.firstOrNull()?.address,
                    satoshi = satoshi[BTC_POLICY_ASSET],
                    isChange = false
                )
            )
        }
        if(txType == Type.OUT && network.isLiquid){
            // On Liquid we have to synthesize the utxo view as we can't unblind the outputs

            satoshi.mapNotNull { asset ->
                if(asset.key != network.policyAsset){
                    UtxoView(
                        assetId = asset.key,
                        satoshi = asset.value,
                        isChange = false
                    )
                }else if(asset.key == network.policyAsset && (asset.value.absoluteValue - fee.absoluteValue) != 0L) {
                    UtxoView(
                        assetId = asset.key,
                        satoshi = -(asset.value.absoluteValue - fee.absoluteValue),
                        isChange = false
                    )
                }else{
                    null
                }
            }

            assets.map { asset ->
                // Remove lbtc fee
                UtxoView(
                    assetId = asset.first,
                    satoshi = asset.second.takeIf { asset.first != network.policyAsset } ?: -(asset.second.absoluteValue - fee.absoluteValue),
                    isChange = false
                )
            }

        }else{
            // Mixed transactions has one part of the transaction as inputs and the other as outputs
            val utxos = outputs.takeIf { txType != Type.MIXED } ?: (inputs + outputs)

            // Try to find the relevant UTXO, different networks/policies have different json structure
            utxos.filter {
                when (txType){
                    Type.OUT -> {
                        if(network.isLiquid){
                            // This is handled above
                            false
                        }else{
                            // On Bitcoin, find the tx that is not relevant to our wallet
                            it.isRelevant == false
                        }
                    }
                    Type.IN -> {
                        it.isRelevant == true
                    }
                    Type.REDEPOSIT -> {
                        false // Hide all utxos as we only display the fee
                    }
                    Type.MIXED -> {
                        it.isRelevant == true
                    }
                    else -> {
                        false
                    }
                }
            }.map {
                // Singlesig liquid returns an unblinded address
                val outAddress = if(network.isLiquid && network.isSinglesig) null else it.address
                val outSatoshi = if(txType == Type.OUT || (txType == Type.MIXED && (satoshi[it.assetId] ?: 0) < 0 )) -(it.satoshi ?: 0) else it.satoshi

                UtxoView(
                    address = outAddress,
                    isBlinded = it.isBlinded,
                    isConfidential = it.isConfidential,
                    assetId = it.assetId,
                    satoshi = outSatoshi,
                    isChange = it.isChange ?: false
                )
            }
        }
    }

    @IgnoredOnParcel
    val assets by lazy {
        satoshi
            .toList()
            .sortedBy {
                if (it.first == network.policyAsset) -1 else it.first.compareTo(it.first)
            }.toMap()
            .mapNotNull {
                if (network.isLiquid && it.key == network.policyAsset && txType == Type.OUT && satoshi.size > 1) {
                    // Check if the L-BTC amount is actually the fee
                    val valueWithoutFee = it.value.absoluteValue - fee.absoluteValue
                    if (valueWithoutFee != 0L || txType == Type.REDEPOSIT) {
                        it.key to it.value
                    } else {
                        // Remove to display fee as amount in liquid
                        null
                    }
                } else if (network.isLiquid && txType == Type.REDEPOSIT && it.key != network.policyAsset) {
                    // Remove irrelevant assets if is redeposit
                    null
                } else if (network.isLightning && txType == Type.OUT) {
                    // Add fee
                    it.key to -(it.value.absoluteValue + fee)
                } else {
                    it.toPair()
                }
            }
    }

    fun getConfirmations(currentBlock: Long): Long {
        if (network.isLightning) return (if (blockHeight > 0) 6 else 0)
        if (blockHeight == 0L || currentBlock == 0L) return 0
        return currentBlock - blockHeight + 1
    }

    val isLoadingTransaction
        get() = blockHeight == -1L

    fun getUnblindedString() = (inputs.mapNotNull { it.getUnblindedString() } + outputs.mapNotNull { it.getUnblindedString() }).joinToString(",")

    fun getUnblindedData(): TransactionUnblindedData {
        val unblindedInputs = inputs.filter {
            it.hasUnblindingData()
        }.map {
            InputUnblindedData(vin = it.ptIdx?.toUInt() ?: 0.toUInt(), assetId = it.assetId ?: "", assetblinder = it.assetblinder ?: "", satoshi = it.satoshi ?: 0, amountblinder = it.amountblinder ?: "")
        }

        val unblindedOutputs = outputs.filter {
            it.hasUnblindingData()
        }.map {
            OutputUnblindedData(vout = it.ptIdx?.toUInt() ?: 0.toUInt(), assetId = it.assetId ?: "", assetblinder = it.assetblinder ?: "", satoshi = it.satoshi ?: 0, amountblinder = it.amountblinder ?: "")
        }

        return TransactionUnblindedData(txid = txHash, type = type, inputs = unblindedInputs, outputs = unblindedOutputs, version = 0)
    }

    companion object {
        // Create a dummy transaction to describe the loading state (blockHeight == -1)
        val LoadingTransaction = Transaction(
            blockHeight = -1,
            canCPFP = false,
            canRBF = false,
            createdAtTs = 0,
            inputs = listOf(),
            outputs = listOf(),
            fee = 0,
            feeRate = 0,
            memo = "",
            rbfOptin = false,
            spvVerified = "",
            txHash = "",
            type = "",
            satoshi = mapOf()
        )
    }
}