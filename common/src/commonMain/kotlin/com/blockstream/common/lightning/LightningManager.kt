

package com.blockstream.common.lightning

import co.touchlab.kermit.Logger
import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet

class LightningManager(private val greenlightKeys: GreenlightKeys): SynchronizedObject() {
    private val bridges = ConcurrentMutableMap<String, LightningBridge>()
    private val references = ConcurrentMutableMap<LightningBridge, AtomicRef<Int>>()

    private val isKeepAliveEnabled = atomic<Boolean>(false)

    fun getLightningBridge(
        file: String
    ): LightningBridge {
        return synchronized(this) {
            (bridges.getOrPut(file) {
                Logger.i { "Creating a new LightningBridge $file" }

                LightningBridge(
                    workingDir = file,
                    greenlightKeys = greenlightKeys
                ).also {
                    references[it] = atomic<Int>(0)
                }
            }).also { bridge ->
                references[bridge]?.update { it + 1 }
            }
        }
    }

    fun release(lightningBridge: LightningBridge) {
        Logger.i { "Release LightningBridge" }
        references[lightningBridge]?.update { it - 1 }
        gc(lightningBridge)
    }

    fun setKeepAlive(keepAlive: Boolean) {
        Logger.i { "setKeepAlive $keepAlive" }
        isKeepAliveEnabled.value = keepAlive

        if (!keepAlive) {
            references.keys.forEach {
                gc(it)
            }
        }
    }

    private fun gc(lightningBridge: LightningBridge) {
        if (!isKeepAliveEnabled.value) {
            // Avoid calling decrementAndGet() inside if statement as it may not be called if there are other boolean calls
            val reference = references[lightningBridge]?.updateAndGet {
                it - 1
            } ?: 0

            if (reference < 0) {
                Logger.i { "Stopping LightningBridge" }
                // Remove
                bridges.remove(lightningBridge.workingDir)
                // Stop
                lightningBridge.stop()
            }
        }
    }
}