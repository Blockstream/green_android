package com.blockstream.common.data

import com.blockstream.common.gdk.data.BcurDecodedData
import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(val result: String, val bcur: BcurDecodedData? = null) {
    companion object {
        fun from(bcurDecodedData: BcurDecodedData) =
            ScanResult(result = bcurDecodedData.simplePayload, bcur = bcurDecodedData)
    }
}