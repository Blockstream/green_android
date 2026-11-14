package com.blockstream.green.utils

import android.content.Context
import android.os.Build
import com.blockstream.green.BuildConfig

val isDebug by lazy { BuildConfig.DEBUG }
val isDevelopmentFlavor by lazy { BuildConfig.FLAVOR == "development" || BuildConfig.APPLICATION_ID.contains(".dev") }
val isDevelopmentOrDebug by lazy { isDevelopmentFlavor || isDebug }
val isProductionFlavor by lazy { !isDevelopmentFlavor }

fun Context.installationInfo(): String = runCatching {
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        packageManager.getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstallerPackageName(packageName)
    }

    val splits = packageManager.getPackageInfo(packageName, 0).splitNames?.joinToString(",")

    "installer=$installer, splits=$splits, libDir=${applicationInfo.nativeLibraryDir}, apk=${applicationInfo.sourceDir}"
}.getOrElse { "unavailable: ${it.message}" }
