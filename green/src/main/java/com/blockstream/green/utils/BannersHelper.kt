package com.blockstream.green.utils

import com.blockstream.green.R
import com.blockstream.green.data.Banner
import com.blockstream.green.database.Wallet
import com.blockstream.green.ui.AppFragment


object BannersHelper {

    fun handle(appFragment: AppFragment<*>, wallet: Wallet?) {
        appFragment.countly.remoteConfigUpdateEvent.observe(appFragment.viewLifecycleOwner) {

            val oldBanner = appFragment.getAppViewModel()?.banner?.value

            appFragment.countly.getRemoteConfigValueForBanners("banners")
                // Filter
                ?.filter {
                    // Filter closed banners & networks
                    appFragment.getAppViewModel()?.closedBanners?.contains(it) == false && (it.networks == null || it.networks.contains(wallet?.network)) &&

                    // Filter based on screen name
                    (it.screens?.contains(appFragment.screenName) == true || it.screens?.contains("*") == true)
                }
                ?.shuffled()
                ?.let {
                    // Search for the already displayed banner, else give priority to those with screen name, else "*"
                    it.find { it == oldBanner } ?: it.find { it.screens?.contains(appFragment.screenName) == true } ?: it.firstOrNull()
                }.also { banner ->
                    // Set banner to ViewModel
                    appFragment.getAppViewModel()?.banner?.postValue(banner)
                }
                ?.also { banner ->
                    appFragment.getBannerAlertView()?.let { bannerAlertView ->
                        if(banner.dismissable == true) {
                            bannerAlertView.closeButton {
                                dismiss(appFragment, banner)
                            }
                        }

                        if(banner.link.isNullOrBlank()){
                            bannerAlertView.primaryButton("", null)
                        }else{
                            bannerAlertView.primaryButton(appFragment.requireContext().getString(R.string.id_learn_more)){
                                handleClick(appFragment, banner)
                            }
                        }
                    }
                }
        }
    }

    fun dismiss(appFragment: AppFragment<*>, banner: Banner) {
        appFragment.getAppViewModel()?.let{
            it.closedBanners += banner
            appFragment.getAppViewModel()?.banner?.postValue(null)
        }

    }

    fun handleClick(appFragment: AppFragment<*>, banner: Banner?) {
        banner?.link.takeIf { !it.isNullOrBlank() }?.let {
            appFragment.openBrowser(it)
        }
    }
}