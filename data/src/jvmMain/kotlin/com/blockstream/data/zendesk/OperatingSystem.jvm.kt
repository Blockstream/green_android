// The desktop's operating system name and version for Zendesk support tickets.

package com.blockstream.data.zendesk

internal actual fun operatingSystemName() = operatingSystemName(System.getProperty("os.name"))

internal actual fun operatingSystemVersion(): String = System.getProperty("os.version")

internal fun operatingSystemName(osName: String): String = when {
    osName.startsWith("Mac", ignoreCase = true) -> "macos"
    osName.startsWith("Windows", ignoreCase = true) -> "windows"
    else -> "linux"
}
