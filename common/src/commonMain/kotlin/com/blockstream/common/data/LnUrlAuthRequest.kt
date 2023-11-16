package com.blockstream.common.data

import breez_sdk.LnUrlAuthRequestData
import com.arkivanov.essenty.parcelable.CommonParceler
import com.arkivanov.essenty.parcelable.ParcelReader
import com.arkivanov.essenty.parcelable.ParcelWriter
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.parcelable.TypeParceler
import com.arkivanov.essenty.parcelable.readString
import com.arkivanov.essenty.parcelable.readStringOrNull
import com.arkivanov.essenty.parcelable.writeString
import com.arkivanov.essenty.parcelable.writeStringOrNull


@Suppress("OPT_IN_USAGE", "OPT_IN_OVERRIDE")
internal object LnUrlAuthRequestDataParceler : CommonParceler<LnUrlAuthRequestData> {
    override fun create(reader: ParcelReader): LnUrlAuthRequestData = LnUrlAuthRequestData(
        reader.readString(), // k1
        reader.readString(), // domain
        reader.readString(), // url
        reader.readStringOrNull(), // action
    )

    override fun LnUrlAuthRequestData.write(writer: ParcelWriter) {
        writer.writeString(k1)
        writer.writeString(domain)
        writer.writeString(url)
        writer.writeStringOrNull(action)
    }
}

@Parcelize
@TypeParceler<LnUrlAuthRequestData, LnUrlAuthRequestDataParceler>()
data class LnUrlAuthRequest(val requestData: LnUrlAuthRequestData) : Parcelable

