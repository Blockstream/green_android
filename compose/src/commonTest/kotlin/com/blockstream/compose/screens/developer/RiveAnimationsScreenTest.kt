// Tests that the developer Rive animations screen lists every animation the app ships.
package com.blockstream.compose.screens.developer

import com.blockstream.compose.components.RiveAnimation
import kotlin.test.Test
import kotlin.test.assertEquals

class RiveAnimationsScreenTest {

    @Test
    fun listsEveryAnimation() {
        assertEquals(RiveAnimation.entries, shippedRiveAnimations)
    }
}
