package com.blockstream.common.lightning

import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LightningManager(private val scope: ApplicationScope, private val greenlightKeys: GreenlightKeys) {
    private val bridges = mutableMapOf<String, LightningBridge>()
    private val references = mutableMapOf<LightningBridge, Int>()

    private val mutex = Mutex()

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