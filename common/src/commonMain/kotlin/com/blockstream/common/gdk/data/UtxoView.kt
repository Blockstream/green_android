package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class UtxoView constructor(
    val address: String? = null,
//    val isBlinded: Boolean? = null,
//    val isConfidential: Boolean? = null,
    val assetId: String? = null,
    val satoshi: Long? = null,
    val amount: String? = null,
    val amountExchange: String? = null,
    val isChange: Boolean = false,
) : GreenJson<UtxoView>(), Parcelable {
    override fun kSerializer() = serializer()

    companion object {
        fun fromOutput(
            output: Output,
            amount: String? = null,
            amountExchange: String? = null,
        ): UtxoView {
            return UtxoView(
                address = output.domain ?: output.address,
                assetId = output.assetId,
                satoshi = -output.satoshi,
                isChange = output.isChange == true,
                amount = amount,
                amountExchange = amountExchange
            )
        }
    }
}