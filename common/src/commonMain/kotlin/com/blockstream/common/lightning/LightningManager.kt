package com.blockstream.common.lightning

import breez_sdk.LogEntry
import breez_sdk.LogStream
import breez_sdk.setLogStream
import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class LightningManager constructor(private val scope: ApplicationScope, private val greenlightKeys: GreenlightKeys) {
    private val bridges = mutableMapOf<String, LightningBridge>()
    private val references = mutableMapOf<LightningBridge, Int>()

    private val mutex = Mutex()

    val logs = StringBuilder()

    init {
        setLogStream(object : LogStream {
            override fun log(l: LogEntry) {
                if(l.level == "DEBUG") {
                    logs.append("${Clock.System.now()} - ${l.line}\n")
                    if(logs.length > 2_000_000){
                        logger.d { "Clear Lightning Logs" }
                        logs.deleteRange(0, 1_000_000)
                    }
                }
            }
        })
    }

    @NativeCoroutinesIgnore
    suspend fun getLightningBridge(
        file: String
    ): LightningBridge {
        return mutex.withLock {
            (bridges.getOrPut(file) {
                logger.i { "Creating a new LightningBridge $file" }

                LightningBridge(
                    workingDir = file,
                    greenlightKeys = greenlightKeys
                )
            }).also { bridge ->
                references[bridge] = (references[bridge] ?: 0) + 1
            }
        }
    }

    fun release(lightningBridge: LightningBridge) {
        scope.launch {
            mutex.withLock {
                logger.i { "Release LightningBridge" }

                val reference = ((references[lightningBridge] ?: 1) - 1).also {
                    references[lightningBridge] = it
                }

                if (reference < 1) {
                    logger.i { "Stopping LightningBridge" }
                    // Remove
                    bridges.remove(lightningBridge.workingDir)
                    // Stop
                    lightningBridge.stop()
                }
            }
        }
    }

    companion object: Loggable()
}