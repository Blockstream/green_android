package com.blockstream.domain.swap

import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.data.CredentialType
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.boltzMnemonic
import com.blockstream.data.lwk.LwkManager
import com.blockstream.jade.Loggable
import kotlinx.coroutines.delay

/**
 * Connects to the Liquid Wallet Kit (LWK) and allows it to process background events for a specific swap.
 *
 * This use case is typically invoked by a background worker or after a swap state change. It ensures
 * the associated wallet is active, derives the necessary Boltz mnemonic for authentication,
 * and maintains a connection for a fixed window to allow LWK to handle pending Boltz events
 * (e.g., claiming funds or reacting to status updates).
 */
class HandleSwapEventsUseCase(
    private val database: Database,
    private val greenKeystore: GreenKeystore,
    private val lwkManager: LwkManager,
    private val getWalletFromSwapUseCase: GetWalletFromSwapUseCase
) : Loggable() {

    /**
     * Handles background events for the given [swapId].
     *
     * Lifecycle:
     * 1. Resolves the [swap] and its owner [wallet] from the database.
     * 2. Acquires an LWK instance for the wallet.
     * 3. If not already connected, derives the Boltz mnemonic using the wallet's login credentials
     *    and the [greenKeystore].
     * 4. Establishes a connection with Boltz via LWK.
     * 5. Keeps the connection open for a conservative 60-second window to ensure background
     *    processing (like on-chain script monitoring) can complete.
     * 6. Releases the LWK instance once finished.
     *
     * @param swapId the unique identifier of the stored swap to process
     * @throws Exception if the Boltz mnemonic cannot be derived or if the wallet is unavailable
     */
    suspend operator fun invoke(
        swapId: String,
    ) {
        logger.d { "HandleEvents invoked" }

        val (swap, wallet) = getWalletFromSwapUseCase(swapId = swapId)

        if (swap != null && wallet != null) {

            val lwk = lwkManager.getLwk(wallet = wallet).apply {
                if (!isConnected) {
                    val derivedBoltzMnemonic = database.getLoginCredential(
                        id = wallet.id,
                        credentialType = CredentialType.BOLTZ_MNEMONIC
                    )?.boltzMnemonic(greenKeystore) ?: throw Exception("No boltz mnemonic found")

                    logger.d { "Connect from LWK" }

                    connect(derivedBoltzMnemonic)
                }
            }

            logger.d { "Waiting for LWK to handle events" }
            // LWK needs some time to process Boltz events and update its internal state.
            // 60 seconds is a conservative window to ensure background processing completes.
            delay(60_000)

            logger.d { "Releasing LWK" }
            lwkManager.release(lwk)
        }
    }
}
