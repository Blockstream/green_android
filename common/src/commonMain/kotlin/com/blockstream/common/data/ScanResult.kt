package com.blockstream.common.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.data.BcurDecodedData

@Parcelize
data class ScanResult(val result: String, val bcur: BcurDecodedData? = null) : Parcelable {
    companion object {
        fun from(bcurDecodedData: BcurDecodedData) =
            ScanResult(result = bcurDecodedData.simplePayload, bcur = bcurDecodedData)
    }
}