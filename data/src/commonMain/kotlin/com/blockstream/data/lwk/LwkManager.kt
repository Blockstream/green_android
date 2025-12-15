package com.blockstream.data.lwk

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.di.ApplicationScope
import com.blockstream.utils.Loggable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LwkManager constructor(
    private val scope: ApplicationScope
) {
    private val bridges = mutableMapOf<String, Lwk>()
    private val references = mutableMapOf<Lwk, Int>()

    private val mutex = Mutex()

    suspend fun getLwk(wallet: GreenWallet): Lwk {
        return mutex.withLock {
            (bridges.getOrPut(wallet.id) {
                logger.i { "Creating a new Lwk for ${wallet.id}" }
                Lwk(wallet)
            }).also { bridge ->
                references[bridge] = (references[bridge] ?: 0) + 1
            }
        }
    }

    fun release(lwk: Lwk?) {
        if (lwk == null) return

        scope.launch {
            mutex.withLock {
                logger.i { "Release Lwk" }

                val reference = ((references[lwk] ?: 1) - 1).also {
                    references[lwk] = it
                }

                if (reference < 1) {
                    // Remove from bridges
                    bridges.remove(lwk.wallet.id)

                    // Stop
                    lwk.disconnect()
                }
            }
        }
    }

    companion object : Loggable()
}