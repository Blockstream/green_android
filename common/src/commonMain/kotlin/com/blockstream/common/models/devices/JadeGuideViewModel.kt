package com.blockstream.common.models.devices

import com.blockstream.common.models.GreenViewModel

abstract class JadeGuideViewModelAbstract : GreenViewModel()

class JadeGuideViewModel : JadeGuideViewModelAbstract(
) {
    override fun screenName(): String = "JadeSetupGuide"

    init {
        bootstrap()
    }
}

class JadeGuideViewModelPreview : JadeGuideViewModelAbstract() {
    companion object {
        fun preview() = JadeGuideViewModelPreview()
    }
}
