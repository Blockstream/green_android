package com.blockstream.domain.wallet

import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.createLoginCredentials
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Persists a derived Lightning mnemonic for the active session/wallet.
 *
 * What it does:
 * - Derives the Lightning mnemonic from the provided [GdkSession] using
 *   `session.deriveLightningMnemonic()`.
 * - Encrypts the mnemonic bytes via [GreenKeystore.encryptData] on the IO dispatcher.
 * - Stores/replaces the credential in the [Database] tied to the [wallet] with
 *   type [CredentialType.LIGHTNING_MNEMONIC] under the current Lightning network id
 *   (`session.lightning!!.id`).
 *
 * Behavior and guarantees:
 * - Best‑effort save: any exceptions are caught and printed; no exception is propagated to callers.
 * - Read/write effects: writes an encrypted credential record to the database; does not mutate
 *   session state.
 * - Thread‑safety: coroutine‑safe; encryption is executed on `Dispatchers.IO`.
 *
 * Notes:
 * - The optional [mnemonic] parameter is currently ignored; the mnemonic is always derived from
 *   the [session]. This parameter exists for potential future use.
 */
class SaveDerivedLightningMnemonicUseCase(
    private val database: Database,
    val greenKeystore: GreenKeystore,
) {

    /**
     * Derives, encrypts, and persists the Lightning mnemonic for the given [wallet].
     *
     * Steps:
     * 1. Derive the mnemonic from [session].
     * 2. Encrypt it with [greenKeystore].
     * 3. Store it in [database] as a [CredentialType.LIGHTNING_MNEMONIC] credential for the
     *    Lightning network associated with the current [session].
     *
     * Error handling: exceptions are caught and logged via `printStackTrace()`; no exception is
     * thrown to the caller.
     *
     * @param session the active session used to derive the Lightning mnemonic and to resolve the
     *        Lightning network id
     * @param wallet the target wallet owning the resulting credential
     * @param mnemonic currently unused; reserved for future use where a caller‑provided mnemonic
     *        may be saved instead of deriving from [session]
     */
    suspend operator fun invoke(
        session: GdkSession, wallet: GreenWallet, mnemonic: String? = null,
    ) {
        tryCatch {
            val encryptedData = withContext(context = Dispatchers.IO) {
                greenKeystore.encryptData(session.deriveLightningMnemonic().encodeToByteArray())
            }

            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = wallet.id,
                    network = session.lightning.id,
                    credentialType = CredentialType.LIGHTNING_MNEMONIC,
                    encryptedData = encryptedData
                )
            )
        }
    }
}
