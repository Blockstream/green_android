package com.blockstream.common.data

import breez_sdk.LnUrlWithdrawRequestData
import com.arkivanov.essenty.parcelable.CommonParceler
import com.arkivanov.essenty.parcelable.ParcelReader
import com.arkivanov.essenty.parcelable.ParcelWriter
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.parcelable.TypeParceler
import com.arkivanov.essenty.parcelable.readLong
import com.arkivanov.essenty.parcelable.readString
import com.arkivanov.essenty.parcelable.writeLong
import com.arkivanov.essenty.parcelable.writeString
import com.arkivanov.essenty.parcelable.writeStringOrNull


@Suppress("OPT_IN_USAGE", "OPT_IN_OVERRIDE")
internal object LnUrlWithdrawRequestDataParceler : CommonParceler<LnUrlWithdrawRequestData> {
    override fun create(reader: ParcelReader): LnUrlWithdrawRequestData = LnUrlWithdrawRequestData(
        reader.readString(), // callback
        reader.readString(), // k1
        reader.readString(), // defaultDescription
        reader.readLong().toULong(), // minWithdrawable
        reader.readLong().toULong(), // maxWithdrawable
    )

    override fun LnUrlWithdrawRequestData.write(writer: ParcelWriter) {
        writer.writeString(callback)
        writer.writeStringOrNull(k1)
        writer.writeString(defaultDescription)
        writer.writeLong(minWithdrawable.toLong())
        writer.writeLong(maxWithdrawable.toLong())
    }
}

@Parcelize
@TypeParceler<LnUrlWithdrawRequestData, LnUrlWithdrawRequestDataParceler>()
data class LnUrlWithdrawRequest(val requestData: LnUrlWithdrawRequestData) : Parcelable

