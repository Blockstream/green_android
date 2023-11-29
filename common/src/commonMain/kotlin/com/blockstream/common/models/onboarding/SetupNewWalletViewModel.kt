package com.blockstream.common.models.onboarding

import com.blockstream.common.Urls
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class SetupNewWalletViewModelAbstract() : GreenViewModel() {
    override fun screenName(): String = "SetupNewWallet"

    @NativeCoroutinesState
    abstract val termsOfServiceIsChecked: MutableStateFlow<Boolean>
}

class SetupNewWalletViewModel : SetupNewWalletViewModelAbstract() {

    class LocalEvents {
        class ClickTermsOfService : Events.OpenBrowser(Urls.TERMS_OF_SERVICE)
        class ClickPrivacyPolicy : Events.OpenBrowser(Urls.PRIVACY_POLICY)
        object ClickAddWallet : Event
        object ClickUseHardwareDevice : Event
    }

    class LocalSideEffects {
        class ShowConsent(sideEffect: SideEffect): SideEffects.SideEffectEvent(Events.EventSideEffect(sideEffect))
        object NavigateAddWallet : SideEffect
        object NavigateUseHardwareDevice : SideEffect
    }

    @NativeCoroutinesState
    override val termsOfServiceIsChecked =
        MutableStateFlow(viewModelScope, settingsManager.isDeviceTermsAccepted())

    init {
        // If you have already agreed, check by default
        viewModelScope.coroutineScope.launch {
            termsOfServiceIsChecked.value =
                settingsManager.isDeviceTermsAccepted() || database.walletsExists()
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClickAddWallet -> {
                if (shouldShowConsentDialog()) {
                    postSideEffect(LocalSideEffects.ShowConsent(LocalSideEffects.NavigateAddWallet))
                } else {
                    postSideEffect(LocalSideEffects.NavigateAddWallet)
                }
                countly.addWallet()
            }

            is LocalEvents.ClickUseHardwareDevice -> {
                if (shouldShowConsentDialog()) {
                    postSideEffect(LocalSideEffects.ShowConsent(LocalSideEffects.NavigateUseHardwareDevice))
                } else {
                    postSideEffect(LocalSideEffects.NavigateUseHardwareDevice)
                }
                countly.hardwareWallet()
                settingsManager.setDeviceTermsAccepted()
            }
        }
    }

    private fun shouldShowConsentDialog(): Boolean {
        return settingsManager.analyticsFeatureEnabled && (!settingsManager.isAskedAboutAnalyticsConsent() && !settingsManager.getApplicationSettings().analytics)
    }
}

class SetupNewWalletViewModelPreview(termsOfServiceIsChecked: Boolean) : SetupNewWalletViewModelAbstract() {

    override val termsOfServiceIsChecked = MutableStateFlow(viewModelScope, termsOfServiceIsChecked)

    companion object {
        fun preview() = SetupNewWalletViewModelPreview(false)
    }
}