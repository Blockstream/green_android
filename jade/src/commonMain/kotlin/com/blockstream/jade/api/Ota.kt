package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_USER_INTERACTION
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtaRequestParams(
    @SerialName("fwsize")
    val fwSize: Int,
    @SerialName("fwhash")
    val fwHash: String? = null,
    @SerialName("cmpsize")
    val cmpSize: Int,
    @SerialName("cmphash")
    val cmpHash: ByteArray,
    @SerialName("patchsize")
    val patchSize: Int? = null,
) : JadeSerializer<OtaRequestParams>() {
    override fun kSerializer(): KSerializer<OtaRequestParams> = kotlinx.serialization.serializer()
}

@Serializable
data class OtaRequest(
    override val id: String = jadeId(),
    override val method: String,
    override val params: OtaRequestParams
) : Request<OtaRequest, OtaRequestParams>() {
    override fun timeout(): Int = TIMEOUT_USER_INTERACTION
    override fun kSerializer(): KSerializer<OtaRequest> = kotlinx.serialization.serializer()

    companion object {
        const val OTA = "ota"
        const val OTA_DELTA = "ota_delta"
    }
}