package com.blockstream.data.lightning

import breez_sdk.LogEntry
import breez_sdk.LogStream
import breez_sdk.setLogStream
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.AppConfig
import com.blockstream.data.di.ApplicationScope
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.LoginData
import com.blockstream.data.platformFileSystem
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import kotlin.time.Clock

class LightningManager constructor(
    private val greenlightKeys: GreenlightKeys,
    private val appInfo: AppInfo,
    private val appConfig: AppConfig,
    private val scope: ApplicationScope,
    private val gdk: Gdk,
    val firebase: FcmCommon,
) {
    private val bridges = mutableMapOf<String, LightningSdk>()
    private val references = mutableMapOf<LightningSdk, Int>()

    private val mutex = Mutex()

    val logs = StringBuilder()

    init {
        if (appConfig.lightningFeatureEnabled) {
            setLogStream(object : LogStream {
                override fun log(l: LogEntry) {
                    if (l.level != "TRACE") {
                        logs.append("${Clock.System.now()} - ${l.line}\n")
                        if (logs.length > 4_000_000) {
                            logger.d { "Clear Lightning Logs" }
                            logs.deleteRange(0, 1_000_000)
                        }
                    }
                }
            })
        }
    }
    
    suspend fun getLightningBridge(loginData: LoginData): LightningSdk {
        val file = "${gdk.dataDir}/breezSdk/${loginData.xpubHashId}/0"

        return mutex.withLock {
            (bridges.getOrPut(file) {
                logger.i { "Creating a new LightningBridge $file" }

                LightningSdk(
                    appInfo = appInfo,
                    workingDir = file,
                    greenlightKeys = greenlightKeys,
                    firebase = firebase,
                )
            }).also { bridge ->
                references[bridge] = (references[bridge] ?: 0) + 1
            }
        }
    }

    suspend fun createDiagnosticData(session: GdkSession): Path {
        val fileSystem = platformFileSystem()
        val logDir = "${appConfig.cacheDir}/logs/".toPath()

        // Delete old logs
        fileSystem.deleteRecursively(logDir)

        if (!platformFileSystem().exists(logDir)) {
            fileSystem.createDirectories(logDir, mustCreate = true)
        }

        return "${logDir}/greenlight_diagnostic_${Clock.System.now()}.txt".toPath().also {
            withContext(context = Dispatchers.IO) {
                fileSystem.write(it) {
                    session.lightningSdk.generateDiagnosticData()?.also {
                        this.writeUtf8("\nLightning Diagnostic Data: -------------------------------------------------\n")
                        this.writeUtf8(it)
                        this.writeUtf8("\n------------------------------------------------------------------\n")
                    }
                }
            }
        }
    }

    fun release(lightningSdk: LightningSdk?) {
        if (lightningSdk == null) return

        scope.launchSafe {
            mutex.withLock {
                logger.i { "Release LightningBridge" }

                val reference = ((references[lightningSdk] ?: 1) - 1).also {
                    references[lightningSdk] = it
                }

                if (reference < 1) {
                    logger.i { "Stopping LightningBridge" }
                    // Remove
                    bridges.remove(lightningSdk.workingDir)
                    // Stop
                    lightningSdk.stop()
                }
            }
        }
    }

    companion object : Loggable()
}