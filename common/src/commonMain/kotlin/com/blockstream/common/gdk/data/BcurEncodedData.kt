package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class BcurEncodedData(
    @SerialName("parts") val parts: List<String>,
): GreenJson<BcurEncodedData>(), Parcelable{
    override fun kSerializer() = serializer()
}