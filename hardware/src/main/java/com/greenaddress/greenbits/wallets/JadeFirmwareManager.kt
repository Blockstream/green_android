package com.greenaddress.greenbits.wallets

import android.os.SystemClock
import android.util.Base64
import com.blockstream.DeviceBrand
import com.blockstream.gdk.GAJson
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.greenaddress.jade.HttpRequestProvider
import com.greenaddress.jade.JadeAPI
import com.greenaddress.jade.entities.JadeVersion
import com.greenaddress.jade.entities.VersionInfo
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.SerialName
import mu.KLogging
import java.io.IOException
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// A firmware instance on the file server
// Meta data, and optionally the actual fw binary
@Serializable
data class FirmwareFileData constructor(
    @SerialName("filepath") val filepath: String,
    @SerialName("image") val image: FirmwareImage,
    @SerialName("firmware") var firmware: ByteArray? = null
) : GAJson<FirmwareFileData>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class FirmwareImage constructor(
    @SerialName("filename") val filename: String,
    @SerialName("version") val version: String,
    @SerialName("config") val config: String,
    @SerialName("fwsize") val fwsize: Int,
    @SerialName("from_version") val fromVersion: String? = null,
    @SerialName("from_config") val fromConfig: String? = null,
    @SerialName("patch_size") val patchSize: Int? = null
) : GAJson<FirmwareImage>() {
    override fun kSerializer() = serializer()
}

@Serializable
data class FirmwareImages constructor(
    @SerialName("full") val full: List<FirmwareImage>? = null,
    @SerialName("delta") val delta: List<FirmwareImage>? = null
) : GAJson<FirmwareImages>() {
    override fun kSerializer() = serializer()
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class FirmwareChannels constructor(
    @SerialName("beta") val beta: FirmwareImages? = null,
    @SerialName("stable") val stable: FirmwareImages? = null,
    @SerialName("previous") val previous: FirmwareImages? = null
) : GAJson<FirmwareChannels>() {
    override fun kSerializer() = serializer()
}

class JadeFirmwareManager(private val firmwareInteraction: FirmwareInteraction,
                          private val httpRequestProvider: HttpRequestProvider,
                          private val jadeFwVersionsFile: String = JADE_FW_VERSIONS_LATEST,
                          private val forceFirmwareUpdate: Boolean = false
) {

    companion object: KLogging(){
        val JADE_MIN_ALLOWED_FW_VERSION = JadeVersion("0.1.24")

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
        return JADE_MIN_ALLOWED_FW_VERSION.compareTo(version) <= 0
    }

    // Check Jade version info to deduce which firmware flavour/directory to use
    private fun getFirmwarePath(info: VersionInfo): String? {
        val prod = info.jadeFeatures.contains(JADE_FEATURE_SECURE_BOOT)
        logger .info { if (prod) "SecureBoot/FlashEncryption detected" else "dev/test unit detected" }
        return when (info.boardType) {
            JADE_BOARD_TYPE_JADE -> {
                // Alas the first version of the jade fw didn't have 'BoardType' - so we assume an early jade.
                logger.info { "Jade 1.0 detected" }
                if (prod) JADE_FW_JADE_PATH else JADE_FW_JADEDEV_PATH
            }
            JADE_BOARD_TYPE_JADE_V1_1 -> {
                // Jade 1.1
                logger.info { "Jade 1.1 detected" }
                if (prod) JADE_FW_JADE1_1_PATH else JADE_FW_JADE1_1DEV_PATH
            }
            else -> {
                val type = info.boardType
                logger.info { "Unsupported hardware detected - $type" }
                null
            }
        }
    }

    // Get Jade firmware server uris - ensures Tor use as appropriate.
    private fun urls(fwFilePath: String): List<URL> {
        val tls = URL(JADE_FW_SERVER_HTTPS + fwFilePath)
        val onion = URL(JADE_FW_SERVER_ONION + fwFilePath)
        return listOf(tls, onion)
    }

    // Uses GDKSession's httpRequest() to get binary file from Jade firmware server
    @Throws(IOException::class)
    private fun downloadBinary(path: String): ByteArray? {
        logger.info { "Fetching firmware file: $path" }
        val ret = httpRequestProvider.httpRequest.httpRequest("GET", urls(path), null, "base64", emptyList())
        if (!ret.has("body")) {
            throw IOException("Failed to fetch firmware file: $path")
        }
        val body = ret["body"].asText()
        return Base64.decode(body, Base64.DEFAULT)
    }

    // Uses GDKSession's httpRequest() to get index file from Jade firmware server
    @Throws(IOException::class)
    private fun downloadIndex(path: String): FirmwareChannels {
        logger.info { "Fetching index file: $path" }
        val ret = httpRequestProvider.httpRequest.httpRequest("GET", urls(path), null, "json", emptyList())
        if (!ret.has("body")) {
            throw IOException("Failed to fetch firmware file: $path")
        }
        val deserializer = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val txt = ret["body"].toString()
        return deserializer.decodeFromString(txt)
    }

    // Get index file and filter channels as appropriate for the passed info
    private fun getAvailableFirmwares(verInfo: VersionInfo): FirmwareImages? {
        // Get relevant fw path (or if hw not supported)
        val fwPath = getFirmwarePath(verInfo)
        if (fwPath.isNullOrBlank()) {
            logger.info { "Unsupported hardware, firmware updates not available" }
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
            logger.info { "Error downloading firmware index file: $e" }
            null
        }
    }

    // Load firmware file into the data object
    private fun loadFirmware(fmw: FirmwareFileData) {
        try {
            // Load file from fw server
            val fw = downloadBinary(fmw.filepath)
            fmw.firmware = fw
        } catch (e: java.lang.Exception) {
            logger.info { "Error downloading firmware file: $e" }
        }
    }

    // Call jade ota update
    // NOTE: the return value is not that useful, as the OTA may have look like it has succeeded
    @Throws(IOException::class)
    private fun doOtaUpdate(jade: JadeAPI, chunksize: Int, fwFile: FirmwareFileData) {
        try {
            logger.info { "Uploading firmware, compressed size: " + fwFile.firmware?.size }
            val updated: Boolean = jade.otaUpdate(
                fwFile.firmware,
                fwFile.image.fwsize,
                fwFile.image.patchSize,
                chunksize,
                firmwareInteraction.getFirmwareCorruption(),
                null
            )
            logger.info { "Jade OTA Update returned: $updated" }
            jade.disconnect()
            if (jade.isUsb) {
                // Sleep to allow jade to reboot
                SystemClock.sleep(5000)
            }
        } catch (e: java.lang.Exception) {
            logger.info { "Error during firmware update: $e" }
            jade.disconnect()
            SystemClock.sleep(1000)
        }

        // On BLE connection re-bonding is expected
        if (jade.isUsb) {
            // Regardless of OTA success, fail, error etc. we try to reconnect.
            if (jade.connect() == null) {
                throw IOException("Failed to reconnect to Jade after OTA")
            }
        }
    }

    // Checks version info and attempts to OTA if required.
    // Exceptions are not usually allowed to propagate out of the function, as failure to access the
    // fw-server, to read the index or download the firmware are not errors that prevent the connection
    // to Jade being made.
    // The function returns whether the current firmware is valid/allowed, regardless of any OTA occurring.
    fun checkFirmware(jade: JadeAPI, deviceHasPinFilter: Boolean): Single<Boolean?>? {
        return Single.create { emitter: SingleEmitter<Boolean?> ->
            try {
                // Do firmware check and ota if necessary
                val verInfo: VersionInfo = jade.versionInfo
                val currentVersion = JadeVersion(verInfo.jadeVersion)
                val fwValid = isJadeFwValid(currentVersion)
                if (verInfo.hasPin != deviceHasPinFilter) {
                    emitter.onSuccess(fwValid)
                    return@create
                }

                // Log if current firmware not valid wrt the allowed minimum version
                if (!fwValid) {
                    logger.info { "Jade firmware is not sufficient to satisfy minimum supported version." }
                    logger.info { "Allowed minimum: $JADE_MIN_ALLOWED_FW_VERSION" }
                    logger.info { "Current version: $currentVersion" }
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
                val full = availableFirmwares?.full?.filter { forceFirmwareUpdate ||
                        (it.config.lowercase() == verInfo.jadeConfig.lowercase() &&
                        JadeVersion(it.version) > currentVersion)
                    }?.map { FirmwareFileData(fwPath + it.filename, it) }
                    .orEmpty()
                    .filter { fmw -> delta.none { it.image.version == fmw.image.version } }

                val updates = delta.toList() + full.toList()
                if (updates.isEmpty()) {
                    logger.info { "No firmware updates currently available." }
                    emitter.onSuccess(fwValid)
                    return@create
                }

                // Get first/only match then offer as Y/N to user
                val (firmwareNameList, bleFirmwareFile) = when (forceFirmwareUpdate) {
                    true -> {
                        val firmwareNameList = updates.map { it.image.version + " " + it.image.config }
                        Pair(firmwareNameList, null)
                    }
                    false -> Pair(null, updates[0])
                }

                firmwareInteraction.askForFirmwareUpgrade(FirmwareUpgradeRequest(
                    DeviceBrand.Blockstream,
                    jade.isUsb,
                    currentVersion.toString(),
                    bleFirmwareFile?.image?.version,
                    firmwareNameList,
                    verInfo.boardType,
                    !fwValid
                )
                ) { firmwareSelectionIndex: Int? ->
                    if (firmwareSelectionIndex != null) {
                        val fwFile = updates[firmwareSelectionIndex]

                        // Update firmware
                        val unused =
                            Single.just(fwFile)
                                .subscribeOn(Schedulers.computation())
                                .doOnSuccess { logger.info { "Loading selected firmware file: " + it.filepath } }
                                .subscribe({ fw: FirmwareFileData ->
                                    loadFirmware(fwFile)
                                    if (fw.firmware == null) {
                                        emitter.onSuccess(fwValid)
                                    } else {
                                        try {
                                            val requireBleRebonding =
                                                jade.isBle && currentVersion.isLessThan(
                                                    JadeVersion("0.1.31")
                                                )

                                            // Try to OTA the fw onto Jade
                                            doOtaUpdate(
                                                jade,
                                                verInfo.jadeOtaMaxChunk,
                                                fwFile
                                            )
                                            if (jade.isUsb) {
                                                // Check fw validity again (from scratch)
                                                val newInfo = jade.versionInfo
                                                val newVersion = JadeVersion(newInfo.jadeVersion)
                                                val fwNowValid = isJadeFwValid(newVersion )
                                                emitter.onSuccess(fwNowValid)
                                                firmwareInteraction.firmwareUpdated(
                                                    false,
                                                    requireBleRebonding
                                                )
                                            } else {
                                                // If it's a BLE connection re-bonding is necessary
                                                firmwareInteraction.firmwareUpdated(
                                                    true,
                                                    requireBleRebonding
                                                )
                                                emitter.onSuccess(true) // the return value is irrelevant as we expect re-bonding
                                            }
                                        } catch (e: java.lang.Exception) {
                                            emitter.onError(e)
                                        }
                                    }
                                }) { t: Throwable? ->
                                    emitter.onError(t!!)
                                }
                    } else {
                        // User declined to update firmware right now
                        logger.info { "No OTA firmware selected" }
                        emitter.onSuccess(fwValid)
                    }
                    null
                }
            } catch (e: java.lang.Exception) {
                emitter.onError(e)
            }
        }
    }
}
