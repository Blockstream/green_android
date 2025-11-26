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
            val afterExcludedFilter = banners?.filterNot {
                excludedBanners.contains(it)
            }

            val activeNetworks = sessionOrNull?.activeSessions?.map { it.network }?.toMutableList() ?: mutableListOf()
            sessionOrNull?.lightning?.network?.also { activeNetworks.add(it) }

            val afterNetworkFilter = afterExcludedFilter?.filter { banner ->
                !banner.hasNetworks || ((banner.networks ?: listOf()).intersect(activeNetworks.toSet()).isNotEmpty())
            }

            val afterScreenFilter = afterNetworkFilter?.filter { banner ->
                (banner.screens?.contains(screenName) == true || banner.screens?.contains("*") == true)
            }

            afterScreenFilter?.shuffled()?.let {
                it.find { it == previousBanner } ?: it.find { it.screens?.contains(screenName) == true } ?: it.firstOrNull()
            }
        }
    }
}
