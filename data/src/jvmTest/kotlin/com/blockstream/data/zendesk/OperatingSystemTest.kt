// Tests how the desktop's Java os.name maps to the operating systems the Zendesk
// support form accepts.

package com.blockstream.data.zendesk

import kotlin.test.Test
import kotlin.test.assertEquals

class OperatingSystemTest {

    @Test
    fun maps_java_os_names_to_zendesk_operating_systems() {
        mapOf(
            "Mac OS X" to "macos",
            "Windows 11" to "windows",
            "Windows Server 2022" to "windows",
            "Linux" to "linux"
        ).forEach { (osName, expected) ->
            assertEquals(expected, operatingSystemName(osName), osName)
        }
    }
}
