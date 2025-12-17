package com.blockstream.domain.swap

import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database

/**
 * Determines whether swap functionality is enabled for a given [GreenWallet].
 *
 * Current behavior (matches implementation):
 * - Swaps are enabled if the wallet is a non‑ephemeral software wallet (i.e., not hardware and
 *   not ephemeral).
 * - Swaps are also enabled if the wallet has a stored Lightning mnemonic credential in the
 *   database: [CredentialType.BOLTZ_MNEMONIC].
 *
 * This means the presence of a [CredentialType.BOLTZ_MNEMONIC] enables swaps regardless of the
 * wallet type, while software wallets that are not ephemeral are enabled even without it.
 *
 * Notes:
 * - This use case performs a read‑only check against the database.
 * - No exceptions are thrown; it simply returns `true`/`false`.
 * - Thread‑safety: safe to call from coroutines; does not mutate state.
 */
class IsSwapsEnabledUseCase(private val database: Database) {
    /**
     * Returns `true` if swaps are currently enabled for the provided [wallet].
     *
     * Enabled when either:
     * - the wallet is software and not ephemeral; or
     * - a [CredentialType.BOLTZ_MNEMONIC] exists in the database for this wallet.
     *
     * @param wallet the target wallet to evaluate
     * @return `true` when swaps are enabled per the rules above; otherwise `false`
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
    ): Boolean {
        // Swaps are enabled if:
        // 1. It's a software wallet (non-ephemeral and non-hardware)
        // 2. OR it has a BOLTZ_MNEMONIC already stored (e.g. for hardware wallets that already set up swaps)
        return (!wallet.isEphemeral && !wallet.isHardware) || database.getLoginCredential(
            id = wallet.id,
            credentialType = CredentialType.BOLTZ_MNEMONIC
        ) != null
    }
}
