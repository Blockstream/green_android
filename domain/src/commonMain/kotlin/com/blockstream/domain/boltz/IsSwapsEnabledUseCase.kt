package com.blockstream.domain.boltz

import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.managers.WalletSettingsManager

/**
 * Determines whether swap functionality is enabled for a given [GreenWallet].
 *
 * Evaluation rules (reflecting current implementation):
 * - Software wallets (non‑hardware): swaps are enabled only if a Lightning mnemonic has been
 *   previously derived and stored in the database as a [CredentialType.BOLTZ_MNEMONIC].
 * - Hardware wallets: swaps are enabled only if BOTH conditions hold:
 *   1) The per‑wallet setting flag is enabled via [WalletSettingsManager.isSwapsEnabled].
 *   2) A Lightning mnemonic has been previously derived and stored in the database as a
 *      [CredentialType.BOLTZ_MNEMONIC].
 *
 * Notes:
 * - This use case performs read‑only checks against settings and the database.
 * - No exceptions are thrown; it simply returns `true`/`false`.
 * - Thread‑safety: safe to call from coroutines; does not mutate state.
 */
class IsSwapsEnabledUseCase(private val database: Database, private val walletSettingsManager: WalletSettingsManager) {
    /**
     * Returns `true` if swaps are currently enabled for the provided [wallet].
     *
     * For software wallets this requires the presence of a stored Lightning mnemonic credential.
     * For hardware wallets this requires both the swaps setting to be enabled and the presence of
     * a stored Lightning mnemonic credential.
     *
     * @param wallet the target wallet to evaluate
     * @return `true` when swaps are enabled per the rules above; otherwise `false`
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
    ): Boolean {
        return ((!wallet.isEphemeral && !wallet.isHardware) || walletSettingsManager.isSwapsEnabled(wallet.id)) && database.getLoginCredential(
            id = wallet.id,
            credentialType = CredentialType.BOLTZ_MNEMONIC
        ) != null
    }
}
