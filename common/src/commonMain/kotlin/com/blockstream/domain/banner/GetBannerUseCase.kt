package com.blockstream.domain.banner

import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.data.banner.Banner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetBannerUseCase() {

    suspend operator fun invoke(
        screenName: String?, banners: List<Banner>?, previousBanner: Banner?, excludedBanners: List<Banner>, sessionOrNull: GdkSession?
    ): Banner? {

        return withContext(context = Dispatchers.Default) {
            banners?.filterNot {
                // Filter closed banners
                excludedBanners.contains(it)
            }?.filterNot {
                // Filter networks
                (!it.hasNetworks || ((it.networks ?: listOf()).intersect((sessionOrNull?.activeSessions?.map { it.network }
                    ?: setOf()).toSet()).isNotEmpty()))
            }?.filterNot {
                // Filter based on screen name
                (it.screens?.contains(screenName) == true || it.screens?.contains("*") == true)
            }?.filter {
                // Filter based on screen name
                (it.screens?.contains(screenName) == true || it.screens?.contains("*") == true)
            }?.shuffled()?.let {
                // Search for the already displayed banner, else give priority to those with screen name, else "*"
                it.find { it == previousBanner } ?: it.find { it.screens?.contains(screenName) == true } ?: it.firstOrNull()
            }
        }
    }
}