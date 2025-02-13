package com.blockstream.green.data.config

class AppInfo constructor(
    val userAgent: String,
    val version: String,
    val isDebug: Boolean,
    val isDevelopment: Boolean,
    val isTest: Boolean = false
){
    val type
        get() = if(isDebug) "debug" else "release"

    val isProduction = !isDevelopment

    val isDevelopmentOrDebug = isDevelopment || isDebug

    val versionFlavorDebug
        get() = "$version ${if(isDevelopment) "(Development)" else ""}${if(isDebug) "[Debug]" else ""}"
}