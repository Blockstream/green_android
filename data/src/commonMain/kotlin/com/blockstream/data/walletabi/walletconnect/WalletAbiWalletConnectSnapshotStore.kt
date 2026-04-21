package com.blockstream.data.walletabi.walletconnect

import com.blockstream.data.managers.WalletSettingsManager

class WalletAbiWalletConnectSnapshotStore(
    private val walletSettingsManager: WalletSettingsManager,
) {
    suspend fun load(walletId: String): String? {
        return walletSettingsManager.getWalletAbiWalletConnectSnapshot(walletId)
    }

    suspend fun save(walletId: String, snapshotJson: String) {
        walletSettingsManager.setWalletAbiWalletConnectSnapshot(
            walletId = walletId,
            snapshot = snapshotJson,
        )
    }

    suspend fun clear(walletId: String) {
        walletSettingsManager.clearWalletAbiWalletConnectSnapshot(walletId)
    }
}
