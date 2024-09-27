package com.blockstream.jade.firmware

import com.blockstream.jade.HttpRequestHandler
import com.blockstream.jade.JadeAPI
import com.blockstream.jade.Loggable
import com.blockstream.jade.api.VersionInfo
import com.blockstream.jade.data.JadeError
import com.blockstream.jade.data.JadeError.Companion.CBOR_RPC_USER_CANCELLED
import com.blockstream.jade.data.JadeState
import com.blockstream.jade.data.JadeVersion
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64

// A firmware instance on the file server
// Meta data, and optionally the actual fw binary

sealed class FirmwareUpdateState {
    data class Initiate(val firmwareFileData: FirmwareFileData, val hash: String):  FirmwareUpdateState()
    data class Uploading(val written: Int, val totalSize: Int):  FirmwareUpdateState()
    data class Failed(val userCancelled: Boolean, val error: String, val firmwareFileData: FirmwareFileData):  FirmwareUpdateState()
    data class Uploaded(val success: Boolean, val firmwareFileData: FirmwareFileData): FirmwareUpdateState()
    data class Completed(val requireReconnection: Boolean, val requireBleRebonding: Boolean): FirmwareUpdateState()

//    data class Completed(val success: Boolean, val firmwareFileData: FirmwareFileData) : FirmwareUpdateState()
//    fun firmwarePushedToDevice(firmwareFileData: FirmwareFileData, hash: String)
//    fun firmwareProgress(written: Int, totalSize: Int)
//    fun firmwareFailed(userCancelled: Boolean, error: String, firmwareFileData: FirmwareFileData)
//    fun firmwareComplete(success: Boolean, firmwareFileData: FirmwareFileData)
//    fun firmwareUpdated(requireReconnection: Boolean, requireBleRebonding: Boolean)
}

class FirmwareFileData(
    val filepath: String,
    val image: FirmwareImage,
    var firmware: ByteArray? = null
)

@Serializable
data class FirmwareImage constructor(
    @SerialName("filename") val filename: String,
    @SerialName("version") val version: String,
    @SerialName("config") val config: String,
    @SerialName("fwsize") val fwsize: Int,
    @SerialName("fwhash") val fwhash: String? = null,
    @SerialName("from_version") val fromVersion: String? = null,
    @SerialName("from_config") val fromConfig: String? = null,
    @SerialName("patch_size") val patchSize: Int? = null
)

@Serializable
data class FirmwareImages constructor(
    @SerialName("full") val full: List<FirmwareImage>? = null,
    @SerialName("delta") val delta: List<FirmwareImage>? = null
)

@Serializable
data class FirmwareChannels constructor(
    @SerialName("beta") val beta: FirmwareImages? = null,
    @SerialName("stable") val stable: FirmwareImages? = null,
    @SerialName("previous") val previous: FirmwareImages? = null
){
    companion object{
        fun fromHttpRequest(response: JsonElement) : FirmwareChannels{
            val deserializer = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            return deserializer.decodeFromJsonElement(response.jsonObject["body"]!!.jsonObject)
        }
    }
}

class JadeFirmwareManager constructor(
    private val firmwareInteraction: FirmwareInteraction,
    private val httpRequestHandler: HttpRequestHandler,
    private val jadeFwVersionsFile: String = JADE_FW_VERSIONS_LATEST,
    private val forceFirmwareUpdate: Boolean = false
) {

    companion object: Loggable(){
        // FIXME: Also we'd then be able to set any new 'supports-swaps' gdk capability to 'true' for Jade.
        val JADE_MIN_ALLOWED_FW_VERSION = JadeVersion("1.0.25")

        const val JADE_FW_SERVER_HTTPS = "https://jadefw.blockstream.com"
        const val JADE_FW_SERVER_ONION =
            "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion"

        const val JADE_FW_JADE_PATH = "/bin/jade/"
        const val JADE_FW_JADE1_1_PATH = "/bin/jade1.1/"
        const val JADE_FW_JADEDEV_PATH = "/bin/jadedev/"
        const val JADE_FW_JADE1_1DEV_PATH = "/bin/jade1.1dev/"

        const val JADE_BOARD_TYPE_JADE = "JADE"
        const val JADE_BOARD_TYPE_JADE_V1_1 = "JADE_V1.1"
        const val JADE_FEATURE_SECURE_BOOT = "SB"

        const val JADE_FW_VERSIONS_LATEST = "LATEST"
        const val JADE_FW_VERSIONS_BETA = "BETA"
        const val JADE_FW_VERSIONS_PREVIOUS = "PREVIOUS"
    }

    // Check Jade fw against minimum allowed firmware version
    private fun isJadeFwValid(version: JadeVersion): Boolean {
        return version >= JADE_MIN_ALLOWED_FW_VERSION
    }

    // Check Jade version info to deduce which firmware flavour/directory to use
    private fun getFirmwarePath(info: VersionInfo): String? {
        val prod = info.jadeFeatures.contains(JADE_FEATURE_SECURE_BOOT)
        logger .i { if (prod) "SecureBoot/FlashEncryption detected" else "dev/test unit detected" }
        return when (info.boardType) {
            JADE_BOARD_TYPE_JADE -> {
                // Alas the first version of the jade fw didn't have 'BoardType' - so we assume an early jade.
                logger.i { "Jade 1.0 detected" }
                if (prod) JADE_FW_JADE_PATH else JADE_FW_JADEDEV_PATH
            }
            JADE_BOARD_TYPE_JADE_V1_1 -> {
                // Jade 1.1
                logger.i { "Jade 1.1 detected" }
                if (prod) JADE_FW_JADE1_1_PATH else JADE_FW_JADE1_1DEV_PATH
            }
            else -> {
                val type = info.boardType
                logger.i { "Unsupported hardware detected - $type" }
                null
            }
        }
    }

    // Get Jade firmware server uris - ensures Tor use as appropriate.
    private fun urls(fwFilePath: String): List<String> {
        val tls = JADE_FW_SERVER_HTTPS + fwFilePath
        val onion = JADE_FW_SERVER_ONION + fwFilePath
        return listOf(tls, onion)
    }

    // Uses GDKSession's httpRequest() to get binary file from Jade firmware server
    @Throws(Exception::class)
    private fun downloadBinary(path: String): ByteArray? {
        logger.i { "Fetching firmware file: $path" }
        val ret = httpRequestHandler.httpRequest("GET", urls(path), null, "base64", emptyList())
        if(!ret.jsonObject.containsKey("body")){
            throw Exception("Failed to fetch firmware file: $path")
        }
        val body = ret.jsonObject["body"]!!.jsonPrimitive.content
        return Base64.decode(body)
    }

    // Uses GDKSession's httpRequest() to get index file from Jade firmware server
    @Throws(Exception::class)
    private fun downloadIndex(path: String): FirmwareChannels {
        logger.i { "Fetching index file: $path" }
        val response = httpRequestHandler.httpRequest("GET", urls(path), null, "json", emptyList())
        if(!response.jsonObject.containsKey("body")){
            throw Exception("Failed to fetch firmware file: $path")
        }

        return FirmwareChannels.fromHttpRequest(response)
    }

    // Get index file and filter channels as appropriate for the passed info
    private fun getAvailableFirmwares(verInfo: VersionInfo): FirmwareImages? {
        // Get relevant fw path (or if hw not supported)
        val fwPath = getFirmwarePath(verInfo)
        if (fwPath.isNullOrBlank()) {
            logger.i { "Unsupported hardware, firmware updates not available" }
            return null
        }
        // Get the index file from that path
        return try {
            downloadIndex(fwPath + "index.json").let {
                return when (jadeFwVersionsFile) {
                    JADE_FW_VERSIONS_BETA -> it.beta
                    JADE_FW_VERSIONS_PREVIOUS -> it.previous
                    else -> it.stable
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.i { "Error downloading firmware index file: $e" }
            null
        }
    }

    // Load firmware file into the data object
    private fun loadFirmware(fmw: FirmwareFileData) {
        try {
            // Load file from fw server
            val fw = downloadBinary(fmw.filepath)
            fmw.firmware = fw
        } catch (e: Exception) {
            logger.i { "Error downloading firmware file: $e" }
        }
    }

    // Call jade ota update
    // NOTE: the return value is not that useful, as the OTA may have look like it has succeeded
    @OptIn(ExperimentalStdlibApi::class)
    @Throws(Exception::class)
    private suspend fun doOtaUpdate(jade: JadeAPI, fwFile: FirmwareFileData, firmwareInteraction: FirmwareInteraction): Boolean {
        try {
            logger.i { "Uploading firmware, compressed size: " + fwFile.firmware?.size }

            val sha256 = SHA256()

            if (firmwareInteraction.getFirmwareCorruption()) {
                // Corrupt hash (for testing purposes)
                sha256.update("corrupt_hash".encodeToByteArray())
            }
            val cmphash = sha256.digest(fwFile.firmware!!)

            firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Initiate(fwFile, cmphash.toHexString()))

            val updated: Boolean = jade.otaUpdate(
                fwFile.firmware!!,
                fwFile.image.fwsize,
                fwFile.image.fwhash,
                fwFile.image.patchSize,
                cmphash
            ) { written, totalSize ->
                firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Uploading(written, totalSize))
            }
            
            logger.i { "Jade OTA Update returned: $updated" }

            firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Uploaded(updated, fwFile))

            return true
        } catch (e: JadeError) {
            logger.i { "Error during firmware update: $e" }
            val userCancelled = e.code == CBOR_RPC_USER_CANCELLED
            firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Failed(userCancelled = userCancelled, error = e.message ?: "", firmwareFileData =  fwFile))

            if(!userCancelled){
                jade.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.e { "Error during firmware update: $e" }
            firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Failed(userCancelled = false, error = e.message ?: "", firmwareFileData =  fwFile))
            jade.disconnect()
        }

        return false
    }

    // Checks version info and attempts to OTA if required.
    // Exceptions are not usually allowed to propagate out of the function, as failure to access the
    // fw-server, to read the index or download the firmware are not errors that prevent the connection
    // to Jade being made.
    // The function returns whether the current firmware is valid/allowed, regardless of any OTA occurring.
    suspend fun checkFirmware(jade: JadeAPI, checkIfUninitialized: Boolean = false): Boolean {
        // Do firmware check and ota if necessary
        val verInfo: VersionInfo = jade.getVersionInfo()
        val currentVersion = JadeVersion(verInfo.jadeVersion)
        val fwValid = isJadeFwValid(currentVersion)

        if (checkIfUninitialized && verInfo.jadeState != JadeState.UNINIT) {
            return fwValid
        }

        try {
            // Log if current firmware not valid wrt the allowed minimum version
            if (!fwValid) {
                logger.i { "Jade firmware is not sufficient to satisfy minimum supported version." }
                logger.i { "Allowed minimum: $JADE_MIN_ALLOWED_FW_VERSION" }
                logger.i { "Current version: $currentVersion" }
            }

            // Fetch any available/selected firmware update
            val fwPath = getFirmwarePath(verInfo)
            val availableFirmwares = getAvailableFirmwares(verInfo)

            // Filter upgradable delta releases
            val delta = availableFirmwares?.delta?.filter {
                it.config.lowercase() == verInfo.jadeConfig.lowercase() &&
                        it.fromConfig?.lowercase() == verInfo.jadeConfig.lowercase() &&
                        it.fromVersion == currentVersion.toString() && JadeVersion(it.version) > currentVersion
            }?.map { FirmwareFileData(fwPath + it.filename, it) }
                .orEmpty()

            // Filter upgradable full releases, not in delta
            val full = availableFirmwares?.full?.filter {
                forceFirmwareUpdate ||
                        (it.config.lowercase() == verInfo.jadeConfig.lowercase() &&
                                JadeVersion(it.version) > currentVersion)
            }?.map { FirmwareFileData(fwPath + it.filename, it) }
                .orEmpty()
                .filter { fmw -> delta.none { it.image.version == fmw.image.version } }

            val updates = delta.toList() + full.toList()
            if (updates.isEmpty()) {
                logger.i { "No firmware updates currently available." }
                return  fwValid
            }

            // Get first/only match then offer as Y/N to user
            val (firmwareNameList, bleFirmwareFile) = when (forceFirmwareUpdate) {
                true -> {
                    val firmwareNameList =
                        updates.map { it.image.version + " " + it.image.config }
                    Pair(firmwareNameList, null)
                }

                false -> Pair(null, updates[0])
            }

            val firmwareSelectionIndex = firmwareInteraction.askForFirmwareUpgrade(
                FirmwareUpgradeRequest(
                    "Blockstream",
                    jade.isUsb,
                    currentVersion.toString(),
                    bleFirmwareFile?.image?.version,
                    firmwareNameList,
                    verInfo.boardType,
                    !fwValid
                )
            ).await()

            if (firmwareSelectionIndex != null) {
                val fwFile = updates[firmwareSelectionIndex]

                // Update firmware
                logger.i { "Loading selected firmware file: " + fwFile.filepath }

                loadFirmware(fwFile)

                if (fwFile.firmware == null) {
                    return  fwValid
                } else {
                    val requireBleRebonding = jade.isBle && currentVersion.isLessThan(JadeVersion("0.1.31"))

                    // Try to OTA the fw onto Jade
                    val updated = doOtaUpdate(
                        jade = jade,
                        fwFile = fwFile,
                        firmwareInteraction = firmwareInteraction
                    )

                    if (updated) {
                        if (jade.isUsb) {
                            // Delay to give time to reboot
                            delay(6000L)

                            // Check fw validity again (from scratch)
                            val newInfo = jade.getVersionInfo()
                            val newVersion = JadeVersion(newInfo.jadeVersion)
                            val fwNowValid = isJadeFwValid(newVersion)

                            firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Completed(requireReconnection = false, requireBleRebonding = requireBleRebonding))

                            return fwNowValid
                        } else {
                            // If it's a BLE connection re-bonding is necessary
                            firmwareInteraction.firmwareUpdateState(FirmwareUpdateState.Completed(requireReconnection = true, requireBleRebonding = requireBleRebonding))
                            return true // the return value is irrelevant as we expect re-bonding
                        }
                    } else {
                        return  fwValid
                    }
                }
            } else {
                // User declined to update firmware right now
                logger.i { "No OTA firmware selected" }
                return  fwValid
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return fwValid
    }
}
