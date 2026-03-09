package com.blockstream.jade.api

import com.blockstream.jade.JADE_BOARD_TYPE_JADE
import com.blockstream.jade.JADE_BOARD_TYPE_JADE_CORE
import com.blockstream.jade.JADE_BOARD_TYPE_JADE_PLUS
import com.blockstream.jade.JADE_BOARD_TYPE_JADE_V1_1
import com.blockstream.jade.data.JadeNetworks
import com.blockstream.jade.data.JadeState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionInfo constructor(
    @SerialName("JADE_VERSION")
    val jadeVersion: String,
    @SerialName("JADE_OTA_MAX_CHUNK")
    val jadeOtaMaxChunk: Int,
    @SerialName("JADE_CONFIG")
    val jadeConfig: String,
    @SerialName("BOARD_TYPE")
    val boardType: String,
    @SerialName("JADE_FEATURES")
    val jadeFeatures: String,
    @SerialName("IDF_VERSION")
    val idfVersion: String,
    @SerialName("CHIP_FEATURES")
    val chipFeatures: String,
    @SerialName("EFUSEMAC")
    val efuseMac: String? = null,
    @SerialName("JADE_STATE")
    val jadeState: JadeState,
    @SerialName("JADE_NETWORKS")
    val jadeNetworks: JadeNetworks,
    @SerialName("JADE_HAS_PIN")
    val jadeHasPin: Boolean,
) : JadeSerializer<VersionInfo>() {
    override fun kSerializer(): KSerializer<VersionInfo> = serializer()

    val isJade
        get() = boardType == JADE_BOARD_TYPE_JADE || boardType == JADE_BOARD_TYPE_JADE_V1_1

    val isJadePlus
        get() = boardType == JADE_BOARD_TYPE_JADE_PLUS

    val isJadeCore
        get() = boardType == JADE_BOARD_TYPE_JADE_CORE

    val isJadeV2
        get() = isJadePlus || isJadeCore

}

@Serializable
data class VersionInfoRequest(
    override val id: String = jadeId(),
    override val method: String = "get_version_info",
    override val params: Unit = Unit
) : Request<VersionInfoRequest, Unit>() {
    override fun kSerializer(): KSerializer<VersionInfoRequest> = kotlinx.serialization.serializer()
}

@Serializable
data class VersionInfoResponse(
    override val id: String,
    override val result: VersionInfo,
    override val error: Error? = null
) : Response<VersionInfoResponse, VersionInfo>() {
    override fun kSerializer(): KSerializer<VersionInfoResponse> =
        kotlinx.serialization.serializer()
}