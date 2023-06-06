package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize

@Parcelize
data class UtxoView constructor(
    val address: String? = null,
    val isBlinded: Boolean? = null,
    val isConfidential: Boolean? = null,
    val assetId: String? = null,
    val satoshi: Long? = null,
    val isChange: Boolean = false,
): Parcelable {
    companion object{
        fun fromOutput(output: Output): UtxoView {
            return UtxoView(
                address = output.domain ?: output.address,
                assetId = output.assetId,
                satoshi = -output.satoshi,
                isChange = output.isChange,
            )
        }
    }
}