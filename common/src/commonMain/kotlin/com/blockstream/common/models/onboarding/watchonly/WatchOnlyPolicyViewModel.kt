package com.blockstream.common.models.onboarding.watchonly

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.ui.events.Event
import org.koin.core.component.inject

abstract class WatchOnlyPolicyViewModelAbstract() : GreenViewModel()

class WatchOnlyPolicyViewModel : WatchOnlyPolicyViewModelAbstract() {
    override fun screenName(): String = "OnBoardWatchOnlyChooseSecurity"

    val gdk: Gdk by inject()

    class LocalEvents {
        class SelectPolicy(val isSinglesig: Boolean) : Event
    }

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is LocalEvents.SelectPolicy) {
            postSideEffect(
                SideEffects.NavigateTo(
                    NavigateDestinations.WatchOnlyNetwork(
                        SetupArgs(
                            isRestoreFlow = true,
                            isWatchOnly = true,
                            isSinglesig = event.isSinglesig
                        )
                    )
                )
            )
        }
    }
}

class WatchOnlyPolicyViewModelPreview : WatchOnlyPolicyViewModelAbstract() {
    companion object {
        fun preview() = WatchOnlyPolicyViewModelPreview()
    }
}