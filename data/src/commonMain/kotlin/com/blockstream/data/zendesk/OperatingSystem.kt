// The operating system and its version, as the Zendesk support ticket fields record them.

package com.blockstream.data.zendesk

internal expect fun operatingSystemName(): String

internal expect fun operatingSystemVersion(): String
