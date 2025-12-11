package com.blockstream.common.models.devices

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_setup_guide
import com.blockstream.common.models.GreenViewModel
import com.blockstream.ui.navigation.NavData
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import org.jetbrains.compose.resources.getString

abstract class JadeGuideViewModelAbstract : GreenViewModel()

class JadeGuideViewModel : JadeGuideViewModelAbstract(
) {
    override fun screenName(): String = "JadeSetupGuide"

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_setup_guide))
        }

        bootstrap()
    }
}

class JadeGuideViewModelPreview : JadeGuideViewModelAbstract() {
    companion object {
        fun preview() = JadeGuideViewModelPreview()
    }
}
