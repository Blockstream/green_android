package com.blockstream.jade.data

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
) : JadeJson<VersionInfo>()