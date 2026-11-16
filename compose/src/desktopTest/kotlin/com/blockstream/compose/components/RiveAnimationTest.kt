// Tests that every Rive animation the UI asks for points to a .riv file in the Compose resources.
// Runs on desktop only: on Android host tests, reading Compose resources calls android.util.Log.
package com.blockstream.compose.components

import blockstream_green.common.generated.resources.Res
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RiveAnimationTest {

    @Test
    fun everyAnimationReadsARiveFile() = runTest {
        RiveAnimation.entries.forEach { animation ->
            val path = animation.filePath
            assertTrue(Res.readBytes(path).isNotEmpty(), "$path is empty")
        }
    }
}
