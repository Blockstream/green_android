package com.blockstream.green.utils

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blockstream.common.data.Banner
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.events.Events
import com.blockstream.green.R
import com.blockstream.green.adapters.bindBanner
import com.blockstream.green.ui.AppFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


object BannersHelper {

    // Replace this until GreenViewModel also handles session
    fun handle(appFragment: AppFragment<*>, session: GdkSession?) {
        val countly = appFragment.countly
        val screenName = appFragment.getGreenViewModel()?.screenName() ?: appFragment.screenName

        countly.remoteConfigUpdateEvent.onEach {
            val oldBanner = appFragment.getGreenViewModel()?.banner?.value

            countly.getRemoteConfigValueForBanners()
                // Filter
                ?.filter {
                    // Filter closed banners
                    appFragment.getGreenViewModel()?.closedBanners?.contains(it) == false &&

                    // Filter networks
                    (!it.hasNetworks || ((it.networks ?: listOf()).intersect((session?.activeSessions?.map { it.network } ?: setOf()).toSet()).isNotEmpty()))  &&

                    // Filter based on screen name
                    (it.screens?.contains(screenName) == true || it.screens?.contains("*") == true)
                }
                ?.shuffled()
                ?.let {
                    // Search for the already displayed banner, else give priority to those with screen name, else "*"
                    it.find { it == oldBanner } ?: it.find { it.screens?.contains(screenName) == true } ?: it.firstOrNull()
                }.also { banner ->
                    // Set banner to ViewModel
                    appFragment.getGreenViewModel()?.banner?.value = banner
                }
                ?.also { banner ->
                    appFragment.getBannerAlertView()?.let { bannerAlertView ->
                        if(banner.dismissable == true) {
                            bannerAlertView.closeButton {
                                appFragment.getGreenViewModel()?.postEvent(Events.BannerDismiss)
                            }
                        }

                        if(banner.link.isNullOrBlank()){
                            bannerAlertView.primaryButton("", null)
                        }else{
                            bannerAlertView.primaryButton(appFragment.requireContext().getString(R.string.id_learn_more)){
                                appFragment.getGreenViewModel()?.postEvent(Events.BannerAction)
                            }
                        }
                    }
                }
        }.launchIn(appFragment.lifecycleScope)
    }

    fun setupBanner(appFragment: AppFragment<*>, banner: Banner?){
        appFragment.getBannerAlertView()?.let { bannerAlertView ->
            if(banner == null){
                bannerAlertView.isVisible = false
            }else {
                bannerAlertView.isVisible = true

                bindBanner(bannerAlertView, banner)

                if (banner.dismissable == true) {
                    bannerAlertView.closeButton {
                        appFragment.getGreenViewModel()?.postEvent(Events.BannerDismiss)
                    }
                }else{
                    bannerAlertView.closeButton(null)
                }

                if (banner.link.isNullOrBlank()) {
                    bannerAlertView.primaryButton("", null)
                } else {
                    bannerAlertView.primaryButton(
                        appFragment.requireContext().getString(R.string.id_learn_more)
                    ) {
                        appFragment.getGreenViewModel()?.postEvent(Events.BannerAction)
                    }
                }
            }
        }
    }
}