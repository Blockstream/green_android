package com.blockstream.data.transaction

import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.UtxoView
import kotlinx.serialization.Serializable

@Serializable
data class TransactionConfirmation(
    val from: AccountAsset? = null,
    val to: AccountAsset? = null,

    val isRedeposit: Boolean = false,

    val utxos: List<UtxoView>? = null,

    val amount: String? = null, val amountFiat: String? = null,

    val swapFee: String? = null,
    val networkFee: String? = null,

    val fee: String? = null, val feeFiat: String? = null, val feeRate: String? = null, val feeAssetId: String? = null,

    val totalFees: String? = null, val totalFeesFiat: String? = null,

    val recipientReceives: String? = null, val recipientReceivesFiat: String? = null,

    val total: String? = null, val totalFiat: String? = null,

    val isAccountTransfer: Boolean = false,
    val isSwap: Boolean = false,
    val isLiquidToLightningSwap: Boolean = false,
) : GreenJson<TransactionConfirmation>() {
    override fun kSerializer() = serializer()

}