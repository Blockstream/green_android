package com.blockstream.domain.send

import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.FeeBlockHigh
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Network

class ShowFeeSelectorUseCase() {

    suspend operator fun invoke(session: GdkSession, network: Network? = null): Boolean {
        if (network == null) {
            return false
        }

        if (network.isLightning) return false

        val highFee = tryCatch { session.getFeeEstimates(network).fees.getOrNull(FeeBlockHigh) } ?: 0

        return (network.isBitcoin || (network.isLiquid && highFee > network.defaultFee))
    }
}
