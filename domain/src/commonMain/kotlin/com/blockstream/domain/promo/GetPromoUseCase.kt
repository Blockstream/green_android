package com.blockstream.domain.promo

import com.blockstream.data.data.Promo
import com.blockstream.data.database.Database
import com.blockstream.data.devices.DeviceModel
import com.blockstream.jade.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPromoUseCase(
    private val database: Database,
) {
    suspend operator fun invoke(promos: List<Promo>): List<Promo> = withContext(Dispatchers.Default) {
        promos
            .filter { filterTarget(it) }
    }

    private suspend fun filterTarget(promo: Promo): Boolean {
        return when (promo.target) {
            TARGET_ONLY_SWW -> {
                database.getWallets(isHardware = true).isEmpty()
            }

            TARGET_JADE_USER -> {
                // user with at least one jade classic wallet but no jade plus
                database.getWallets(isHardware = true).mapNotNull { it.deviceIdentifiers }.flatten().let {
                    it.any { it.model == DeviceModel.BlockstreamGeneric || it.model == DeviceModel.BlockstreamJade } &&
                            it.all { it.model != DeviceModel.BlockstreamJadePlus }
                }
            }

            TARGET_JADE_PLUS_USER -> {
                // users with at least one jade plus wallet
                database.getWallets(isHardware = true).mapNotNull { it.deviceIdentifiers }.flatten().let {
                    it.any { it.model == DeviceModel.BlockstreamJadePlus }
                }
            }

            null -> true
            else -> false // by default hide if you don't recognize the target
        }
    }

    companion object : Loggable() {
        const val TARGET_ONLY_SWW = "only_sww"
        const val TARGET_JADE_USER = "jade_user"
        const val TARGET_JADE_PLUS_USER = "jadeplus_user"
    }
}
