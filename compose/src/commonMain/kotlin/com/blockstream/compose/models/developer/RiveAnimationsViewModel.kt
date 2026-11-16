// View model of the developer Rive animations screen; it only names the screen.
package com.blockstream.compose.models.developer

import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData

class RiveAnimationsViewModel : GreenViewModel() {

    init {
        _navData.value = NavData(title = "Rive animations")

        bootstrap()
    }
}
