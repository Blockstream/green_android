package com.blockstream.compose.models.sheets

import com.blockstream.common.Urls
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel

abstract class RecoveryHelpViewModelAbstract : GreenViewModel()
class RecoveryHelpViewModel : RecoveryHelpViewModelAbstract() {
    override fun screenName(): String = "OnBoardEnterRecoveryHelp"

    class LocalEvents {
        object ClickHelp : Events.OpenBrowser(Urls.HELP_MNEMONIC_NOT_WORKING)
    }

    init {
        bootstrap()
    }
}

class RecoveryHelpViewModelPreview() : RecoveryHelpViewModelAbstract() {

    companion object {
        fun preview() = RecoveryHelpViewModelPreview()
    }
}