package com.blockstream.common.models.about

import com.blockstream.common.Urls
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

abstract class AboutViewModelAbstract: GreenViewModel() {
    abstract val year: String
    abstract val version: String
}

class AboutViewModel : AboutViewModelAbstract() {

    override fun screenName(): String = "About"

    class LocalEvents {
        object ClickTermsOfService : Events.OpenBrowser(Urls.TERMS_OF_SERVICE)
        object ClickPrivacyPolicy : Events.OpenBrowser(Urls.PRIVACY_POLICY)
        object ClickWebsite : Events.OpenBrowser(Urls.BLOCKSTREAM_GREEN_WEBSITE)
        object ClickTwitter : Events.OpenBrowser(Urls.BLOCKSTREAM_TWITTER)
        object ClickLinkedIn : Events.OpenBrowser(Urls.BLOCKSTREAM_LINKEDIN)
        object ClickFacebook : Events.OpenBrowser(Urls.BLOCKSTREAM_FACEBOOK)
        object ClickTelegram : Events.OpenBrowser(Urls.BLOCKSTREAM_TELEGRAM)
        object ClickGitHub : Events.OpenBrowser(Urls.BLOCKSTREAM_GITHUB)
        object ClickYouTube : Events.OpenBrowser(Urls.BLOCKSTREAM_YOUTUBE)
        object ClickHelp : Events.OpenBrowser(Urls.HELP_CENTER)
        object ClickFeedback : Event
        object ClickLogo : Event
        object CountlyCopyDeviceId : Event
        object CountlyResetDeviceId : Event
        object CountlyZeroOffset : Event
    }

    override val year: String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year.toString()

    override val version = appInfo.version

    private var _clickCounter = 0

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ClickLogo) {
            _clickCounter++
            if (_clickCounter > 5) {
                postSideEffect(
                    SideEffects.OpenMenu()
                )
            }
        } else if (event is LocalEvents.ClickFeedback) {
            postSideEffect(SideEffects.OpenDialog())
        } else if (event is LocalEvents.CountlyResetDeviceId){
            countly.resetDeviceId()
            postSideEffect(SideEffects.Snackbar("DeviceID reset. New DeviceId ${countly.getDeviceId()}"))
        } else if (event is LocalEvents.CountlyZeroOffset){
            settingsManager.zeroCountlyOffset()
            countly.updateOffset()
            postSideEffect(SideEffects.Snackbar("Countly offset reset to zero"))
        } else if (event is LocalEvents.CountlyCopyDeviceId){
            countly.getDeviceId().let { deviceId ->
                postSideEffect(SideEffects.CopyToClipboard(deviceId, "DeviceID copied to Clipboard $deviceId"))
            }
        }
    }
}

class AboutViewModelPreview(override val year: String, override val version: String) : AboutViewModelAbstract(){
    companion object{
        fun preview() = AboutViewModelPreview(year = "2000", version = "4.0.0-preview")
    }
}