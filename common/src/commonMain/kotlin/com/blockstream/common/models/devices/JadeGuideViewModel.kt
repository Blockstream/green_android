package com.blockstream.common.models.devices

import com.blockstream.common.models.GreenViewModel

open class JadeGuideViewModel : GreenViewModel(
) {
    override fun screenName(): String = "JadeSetupGuide"

    init {
        bootstrap()
    }
}

class JadeGuideViewModelPreview : JadeGuideViewModel() {
    companion object {
        fun preview() = JadeGuideViewModel()
    }
}
