package com.blockstream.compose.models.sheets

import com.blockstream.data.Urls
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.sideeffects.SideEffects

abstract class AnalyticsViewModelAbstract : GreenViewModel() {
    abstract val isActionRequired: Boolean
    abstract val showActionButtons: Boolean
}

class AnalyticsViewModel(override val isActionRequired: Boolean) : AnalyticsViewModelAbstract() {
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

    override suspend fun handleEvent(event: Event) {
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
    override val isActionRequired: Boolean = false

    override val showActionButtons = show

    companion object {
        fun preview() = AnalyticsViewModelPreview(true)
    }
}