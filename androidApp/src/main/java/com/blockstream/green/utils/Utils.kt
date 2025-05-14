package com.blockstream.green.utils

import com.blockstream.green.BuildConfig

val isDebug by lazy { BuildConfig.DEBUG }
val isDevelopmentFlavor by lazy { BuildConfig.FLAVOR == "development" || BuildConfig.APPLICATION_ID.contains(".dev") }
val isDevelopmentOrDebug by lazy { isDevelopmentFlavor || isDebug }
val isProductionFlavor by lazy { !isDevelopmentFlavor }
