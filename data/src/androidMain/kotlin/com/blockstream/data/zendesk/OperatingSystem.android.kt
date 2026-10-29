// Android's operating system name and API level for Zendesk support tickets.

package com.blockstream.data.zendesk

import android.os.Build

internal actual fun operatingSystemName() = "android"

internal actual fun operatingSystemVersion() = Build.VERSION.SDK_INT.toString()
