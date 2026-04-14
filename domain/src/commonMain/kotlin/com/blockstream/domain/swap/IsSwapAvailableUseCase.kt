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

        if (asset?.isLightning == true) return false

        if (!wallet.isMainnet || wallet.isEphemeral) return false
        if (asset != null && !asset.isPolicyAsset(session)) return false

        // Also require at least 2 distinct swappable (non-Lightning) networks before showing the swap button.
        val swappableNetworks = session.accountAsset.value
            .filter { it.asset.isPolicyAsset(session) && !it.account.isLightning }
            .map { it.account.network.canonicalNetworkId }
            .distinct()

        return swappableNetworks.size >= 2
    }
}
