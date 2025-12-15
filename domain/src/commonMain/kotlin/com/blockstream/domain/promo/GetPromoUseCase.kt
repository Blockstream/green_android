package com.blockstream.domain.promo

import com.blockstream.data.data.Promo
import com.blockstream.data.database.Database
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.managers.PromoManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.jade.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPromoUseCase(val promoManager: PromoManager, private val database: Database, val settingsManager: SettingsManager) {

    suspend operator fun invoke(screenName: String?, previousPromo: Promo?): Promo? {
        return withContext(context = Dispatchers.Default) {
            val promos = promoManager.promos.value

            promos
                .filterNot {
                    // Filter closed promos
                    settingsManager.isPromoDismissed(it.id)
                }
                .filter {
                    // Filter closed promos
                    filterTarget(it)
                }
                .filter {
                    // Filter based on screen name
                    (it.screens == null || it.screens!!.contains(screenName) || it.screens!!.contains("*"))
                }
                .let {
                    // Search for the already displayed promo, else give priority to those with screen name, else "*"
                    it.find { it == previousPromo } ?: it.find { it.screens?.contains(screenName) == true }
                    ?: it.firstOrNull()
                }
        }
    }

    private suspend fun filterTarget(promo: Promo): Boolean {

        return when (promo.target) {
            TARGET_ONLY_SWW -> {
                // user with only software wallets
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