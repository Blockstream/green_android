package com.blockstream.common.models.home

import com.blockstream.common.Urls
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.models.wallets.WalletsViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable

class HomeViewModel: WalletsViewModel(isHome = true){

    class LocalEvents {
        class ClickTermsOfService : Events.OpenBrowser(Urls.TERMS_OF_SERVICE)
        class ClickPrivacyPolicy : Events.OpenBrowser(Urls.PRIVACY_POLICY)
        object ClickGetStarted : Event
    }

    class LocalSideEffects {
        class ShowConsent(sideEffect: SideEffect): SideEffects.SideEffectEvent(Events.EventSideEffect(sideEffect))
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClickGetStarted -> {
                if (shouldShowConsentDialog()) {
                    postSideEffect(
                        LocalSideEffects.ShowConsent(SideEffects.NavigateTo(
                            NavigateDestinations.SetupNewWallet))
                    )
                } else {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetupNewWallet))
                }
                settingsManager.setDeviceTermsAccepted()
            }
        }
    }

    private fun shouldShowConsentDialog(): Boolean {
        return settingsManager.analyticsFeatureEnabled && (!settingsManager.isAskedAboutAnalyticsConsent() && !settingsManager.getApplicationSettings().analytics)
    }

    companion object: Loggable()
}