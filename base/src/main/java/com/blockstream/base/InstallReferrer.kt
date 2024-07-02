package com.blockstream.base

import ly.count.android.sdk.ModuleAttribution

// No-Op Install Referrer for F-Droid
open class InstallReferrer {
    open fun handleReferrer(attribution: ModuleAttribution.Attribution, onComplete: (referrer: String) -> Unit) {
        onComplete.invoke("")
    }
}