package com.blockstream.domain.boltz

import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.data.CredentialType
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.boltzMnemonic
import com.blockstream.common.lwk.LwkManager
import com.blockstream.jade.Loggable
import kotlinx.coroutines.delay

/**
 * Connects to LWK and lets it process events for a specific swap for a short period.
 *
 * This use case is typically invoked by a background worker after swap state changes. It resolves
 * the associated wallet, ensures LWK is connected using the stored Boltz mnemonic, waits for a
 * fixed window so LWK can handle events, and then releases the LWK instance.
 */
class HandleSwapEventsUseCase(
    private val database: Database,
    private val greenKeystore: GreenKeystore,
    private val lwkManager: LwkManager,
    private val getWalletFromSwapUseCase: GetWalletFromSwapUseCase
) : Loggable() {

    /**
     * Handles events for the given [swapId]. If the swap and its wallet can be resolved, connects
     * LWK (deriving the Boltz mnemonic if necessary), waits briefly to allow processing, then
     * releases resources.
     *
     * @param swapId the identifier of the stored swap
     * @throws Exception if the Boltz mnemonic cannot be obtained when needed
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
            delay(60_000)

            logger.d { "Releasing LWK" }
            lwkManager.release(lwk)
        }
    }
}
