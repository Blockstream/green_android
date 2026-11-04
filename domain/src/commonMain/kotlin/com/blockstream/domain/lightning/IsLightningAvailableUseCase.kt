// Asks Greenlight whether the Lightning account of a wallet's node may be offered to the user,
// which glsdk allows before the node is registered.
package com.blockstream.domain.lightning

import com.blockstream.data.extensions.tryCatchNullSuspend
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.lightning.GreenlightSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class IsLightningAvailableUseCase {
    // Null when Greenlight can't answer: glsdk says to treat an error as unknown, not as unavailable
    suspend operator fun invoke(session: GdkSession): Boolean? {
        // Greenlight is mainnet only, and supportsLightning also confirms glsdk's native library loaded
        if (session.isTestnet || !session.supportsLightning()) return false

        return withContext(Dispatchers.IO) {
            tryCatchNullSuspend { GreenlightSdk.isLightningAvailable(mnemonic = session.deriveLightningMnemonic()) }
        }
    }
}
