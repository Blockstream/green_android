package com.blockstream.compose.models.devices

import com.blockstream.compose.utils.StringHolder
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_setup_guide
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData

abstract class JadeGuideViewModelAbstract : GreenViewModel()

class JadeGuideViewModel : JadeGuideViewModelAbstract(
) {
    override fun screenName(): String = "JadeSetupGuide"

    init {
        _navData.value = NavData(title = StringHolder(stringResource = Res.string.id_setup_guide))

        bootstrap()
    }
}

class JadeGuideViewModelPreview : JadeGuideViewModelAbstract() {
    companion object {
        fun preview() = JadeGuideViewModelPreview()
    }
}
