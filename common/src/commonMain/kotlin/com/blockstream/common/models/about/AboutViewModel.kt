package com.blockstream.common.models.about

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_thank_you_for_your_feedback
import com.blockstream.common.Urls
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.StringHolder
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

abstract class AboutViewModelAbstract : GreenViewModel() {
    abstract val year: String
    abstract val version: String

    @NativeCoroutinesState
    abstract val rate: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val email: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val feedback: MutableStateFlow<String>
}

class AboutViewModel : AboutViewModelAbstract() {

    override fun screenName(): String = "About"

    override val rate: MutableStateFlow<Int> = MutableStateFlow(0)
    override val email: MutableStateFlow<String> = MutableStateFlow("")
    override val feedback: MutableStateFlow<String> = MutableStateFlow("")


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
        object ResetPromos : Event
        object SendFeedback: Event
    }

    override val year: String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year.toString()

    override val version = appInfo.version

    private var _clickCounter = 0

    init {

        combine(rate, feedback) { rate, feedback ->
            rate > 0 && feedback.isNotBlank()
        }.onEach {
            _isValid.value = it
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
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
        } else if (event is LocalEvents.CountlyResetDeviceId) {
            countly.resetDeviceId()
            postSideEffect(SideEffects.Snackbar(text = StringHolder.create("DeviceID reset. New DeviceId ${countly.getDeviceId()}")))
        } else if (event is LocalEvents.CountlyZeroOffset) {
            settingsManager.zeroCountlyOffset()
            countly.updateOffset()
            postSideEffect(SideEffects.Snackbar(text = StringHolder.create("Countly offset reset to zero")))
        } else if (event is LocalEvents.ResetPromos) {
            settingsManager.resetPromoDismissals()
            postSideEffect(SideEffects.Snackbar(text = StringHolder.create("Reset promos")))
        } else if (event is LocalEvents.CountlyCopyDeviceId) {
            countly.getDeviceId().let { deviceId ->
                postSideEffect(
                    SideEffects.CopyToClipboard(
                        deviceId,
                        "DeviceID copied to Clipboard $deviceId"
                    )
                )
            }
        } else if (event is LocalEvents.SendFeedback) {
            countly.recordFeedback(
                rating = rate.value,
                email = email.value.trim(),
                comment = feedback.value
            )
            postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_thank_you_for_your_feedback)))
            postSideEffect(SideEffects.Dismiss)

            rate.value = 0
            email.value = ""
            feedback.value = ""
        }
    }
}

class AboutViewModelPreview(override val year: String, override val version: String) :
    AboutViewModelAbstract() {

    override val rate: MutableStateFlow<Int> = MutableStateFlow(0)
    override val email: MutableStateFlow<String> = MutableStateFlow("")
    override val feedback: MutableStateFlow<String> = MutableStateFlow("")

    companion object {
        fun preview() = AboutViewModelPreview(year = "2000", version = "4.0.0-preview")
    }
}