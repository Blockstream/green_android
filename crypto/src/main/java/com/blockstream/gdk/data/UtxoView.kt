package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UtxoView constructor(
    val address: String? = null,
    val isBlinded: Boolean? = null,
    val assetId: String? = null,
    val satoshi: Long? = null,
    val isChange: Boolean = false,
): Parcelable{
    companion object{
        fun fromOutput(output: Output): UtxoView {
            return UtxoView(
                address = output.address,
                assetId = output.assetId,
                satoshi = -output.satoshi,
                isChange = output.isChange,
            )
        }
    }
}