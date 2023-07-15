package com.blockstream.lightning

import mu.KLogging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LightningManager {
    private val bridges = ConcurrentHashMap<File, LightningBridge>()
    private val references = ConcurrentHashMap<LightningBridge, AtomicInteger>()

    private val isKeepAliveEnabled = AtomicBoolean(false)

    fun getLightningBridge(
        file: File
    ): LightningBridge {
        return (bridges.getOrPut(file) {
            logger.info { "Creating a new LightningBridge" }

            LightningBridge(
                workingDir = file
            ).also {
                references[it] = AtomicInteger(0)
            }
        }).also {
            references[it]?.incrementAndGet()
        }
    }

    fun release(lightningBridge: LightningBridge) {
        logger.info { "Release LightningBridge" }
        references[lightningBridge]?.decrementAndGet()
        gc(lightningBridge)
    }

    fun setKeepAlive(keepAlive: Boolean) {
        logger.info { "setKeepAlive $keepAlive" }
        isKeepAliveEnabled.set(keepAlive)

        if (!keepAlive) {
            references.keys.forEach {
                gc(it)
            }
        }
    }

    private fun gc(lightningBridge: LightningBridge) {
        if (!isKeepAliveEnabled.get()) {
            // Avoid calling decrementAndGet() inside if statement as it may not be called if there are other boolean calls
            val reference = references[lightningBridge]?.decrementAndGet() ?: 0

            if (reference < 0) {
                logger.info { "Stopping LightningBridge" }
                // Remove
                bridges.remove(lightningBridge.workingDir)
                // Stop
                lightningBridge.stop()
            }
        }
    }

    companion object : KLogging()
}