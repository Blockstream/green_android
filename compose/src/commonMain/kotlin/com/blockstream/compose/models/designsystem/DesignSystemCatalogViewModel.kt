package com.blockstream.compose.models.designsystem

import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData

class DesignSystemCatalogViewModel : GreenViewModel() {

    init {
        _navData.value = NavData(title = "Design system")
        bootstrap()
    }
}
