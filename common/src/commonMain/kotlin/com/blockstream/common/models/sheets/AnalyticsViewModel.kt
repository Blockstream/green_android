package com.blockstream.common.models.sheets

import com.blockstream.common.Urls
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects

abstract class AnalyticsViewModelAbstract : GreenViewModel() {
    abstract val showActionButtons: Boolean
}

class AnalyticsViewModel : AnalyticsViewModelAbstract() {
    override fun screenName(): String = "Consent"

    override val showActionButtons: Boolean = settingsManager.analyticsFeatureEnabled
            && (!settingsManager.isAskedAboutAnalyticsConsent()
            && !settingsManager.getApplicationSettings().analytics)

    class LocalEvents {
        object ClickLearnMore : Events.OpenBrowser(Urls.HELP_WHATS_COLLECTED)
        class ClickDataCollection(val allow: Boolean) : Event
    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ClickDataCollection) {
            if (event.allow) {
                settingsManager.saveApplicationSettings(
                    settingsManager.getApplicationSettings().copy(analytics = true)
                )
            }

            settingsManager.setAskedAboutAnalyticsConsent()

            postSideEffect(SideEffects.Dismiss)
        }
    }
}

class AnalyticsViewModelPreview(val show: Boolean) : AnalyticsViewModelAbstract() {
    override val showActionButtons = show

    companion object {
        fun preview() = AnalyticsViewModelPreview(true)
    }
}