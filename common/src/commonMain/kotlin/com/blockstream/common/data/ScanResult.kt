package com.blockstream.common.data

import cafe.adriel.voyager.core.lifecycle.JavaSerializable
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.data.BcurDecodedData

@Parcelize
data class ScanResult(val result: String, val bcur: BcurDecodedData? = null) : Parcelable, JavaSerializable {
    companion object {
        fun from(bcurDecodedData: BcurDecodedData) =
            ScanResult(result = bcurDecodedData.simplePayload, bcur = bcurDecodedData)
    }
}