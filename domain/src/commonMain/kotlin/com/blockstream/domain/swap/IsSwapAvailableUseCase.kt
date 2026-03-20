package com.blockstream.domain.swap

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession

class IsSwapAvailableUseCase {
    operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        asset: EnrichedAsset? = null
    ): Boolean {

        if (session.device?.isJadeCore?.value == true) return false

        if (wallet.isWatchOnly && !wallet.isHardware) return false

        return wallet.isMainnet && !wallet.isEphemeral && (asset == null || asset.isPolicyAsset(session))
    }
}
